package com.balaji.service;

import com.balaji.config.RazorpayConfig;
import com.balaji.dto.RazorpayDTO;
import com.balaji.dto.OrderRequestDTO;
import com.balaji.model.Order;
import com.balaji.model.RazorpayOrder;
import com.balaji.repository.RazorpayOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayConfig       razorpayConfig;
    private final RazorpayOrderRepository razorpayOrderRepo;
    private final OrderService         orderService;
    private final FrameService         frameService;

    private static final String RAZORPAY_ORDERS_API = "https://api.razorpay.com/v1/orders";

    // ── Step 1: Create Razorpay Order ────────────────────────────────────────
    @Transactional
    public RazorpayDTO.CreateOrderResponse createRazorpayOrder(
            RazorpayDTO.CreateOrderRequest req) throws Exception {

        // 1a. Calculate price
        var price = frameService.calculatePrice(
                req.getFrameSizeId(), req.getBeadingOptionId(), req.getCoverOptionId());

        // 1b. Save internal order first (status = PENDING)
        OrderRequestDTO orderReq = new OrderRequestDTO();
        orderReq.setFrameSizeId(req.getFrameSizeId());
        orderReq.setBeadingOptionId(req.getBeadingOptionId());
        orderReq.setCoverOptionId(req.getCoverOptionId());
        orderReq.setCustomerName(req.getCustomerName());
        orderReq.setCustomerPhone(req.getCustomerPhone());
        orderReq.setCustomerEmail(req.getCustomerEmail());
        orderReq.setDeliveryAddress(req.getDeliveryAddress());
        orderReq.setUploadedPhotoPath(req.getUploadedPhotoPath());
        orderReq.setPaymentMethod("RAZORPAY");
        Order internalOrder = orderService.placeOrder(orderReq);

        // 1c. Amount in paise (INR × 100)
        int amountPaise = price.getTotalPrice()
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        // 1d. Call Razorpay API to create order
        JSONObject body = new JSONObject();
        body.put("amount",   amountPaise);
        body.put("currency", "INR");
        body.put("receipt",  "balaji_order_" + internalOrder.getId());
        body.put("notes", new JSONObject()
                .put("customerName",  req.getCustomerName())
                .put("customerPhone", req.getCustomerPhone())
                .put("orderId",       internalOrder.getId()));

        String credentials = Base64.getEncoder().encodeToString(
                (razorpayConfig.getKeyId() + ":" + razorpayConfig.getKeySecret())
                        .getBytes(StandardCharsets.UTF_8));

        HttpClient  client  = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RAZORPAY_ORDERS_API))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type",  "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Razorpay API error: {}", response.body());
            throw new RuntimeException("Razorpay order creation failed: " + response.body());
        }

        JSONObject rzpResponse = new JSONObject(response.body());
        String razorpayOrderId = rzpResponse.getString("id");

        // 1e. Save RazorpayOrder record in DB
        RazorpayOrder rzpOrder = RazorpayOrder.builder()
                .razorpayOrderId(razorpayOrderId)
                .order(internalOrder)
                .amount(price.getTotalPrice())
                .amountInPaise(amountPaise)
                .currency("INR")
                .paymentStatus(RazorpayOrder.PaymentStatus.CREATED)
                .build();
        razorpayOrderRepo.save(rzpOrder);

        log.info("Razorpay order created: {} for internal order: {}",
                razorpayOrderId, internalOrder.getId());

        // 1f. Return to frontend
        return RazorpayDTO.CreateOrderResponse.builder()
                .razorpayOrderId(razorpayOrderId)
                .razorpayKeyId(razorpayConfig.getKeyId())
                .amountInPaise(amountPaise)
                .currency("INR")
                .customerName(req.getCustomerName())
                .customerPhone(req.getCustomerPhone())
                .customerEmail(req.getCustomerEmail() != null ? req.getCustomerEmail() : "")
                .internalOrderId(internalOrder.getId())
                .build();
    }

    // ── Step 2: Verify Payment Signature ─────────────────────────────────────
    @Transactional
    public RazorpayDTO.PaymentVerifyResponse verifyPayment(
            RazorpayDTO.PaymentVerifyRequest req) {

        try {
            // 2a. Verify HMAC SHA256 signature
            boolean valid = verifySignature(
                    req.getRazorpayOrderId(),
                    req.getRazorpayPaymentId(),
                    req.getRazorpaySignature());

            if (!valid) {
                log.warn("Invalid Razorpay signature for order: {}", req.getRazorpayOrderId());
                markPaymentFailed(req.getRazorpayOrderId());
                return RazorpayDTO.PaymentVerifyResponse.builder()
                        .success(false)
                        .message("Payment verification failed. Please contact support.")
                        .build();
            }

            // 2b. Update RazorpayOrder record
            RazorpayOrder rzpOrder = razorpayOrderRepo
                    .findByRazorpayOrderId(req.getRazorpayOrderId())
                    .orElseThrow(() -> new RuntimeException("Razorpay order not found"));

            rzpOrder.setRazorpayPaymentId(req.getRazorpayPaymentId());
            rzpOrder.setRazorpaySignature(req.getRazorpaySignature());
            rzpOrder.setPaymentStatus(RazorpayOrder.PaymentStatus.PAID);
            rzpOrder.setPaidAt(LocalDateTime.now());
            razorpayOrderRepo.save(rzpOrder);

            // 2c. Update internal Order status → CONFIRMED
            orderService.updateStatus(
                    rzpOrder.getOrder().getId(), Order.OrderStatus.CONFIRMED);

            log.info("Payment verified ✅ OrderId={} PaymentId={}",
                    req.getRazorpayOrderId(), req.getRazorpayPaymentId());

            return RazorpayDTO.PaymentVerifyResponse.builder()
                    .success(true)
                    .message("Payment successful!")
                    .orderId(rzpOrder.getOrder().getId())
                    .redirectUrl("/order/confirmation/" + rzpOrder.getOrder().getId())
                    .build();

        } catch (Exception e) {
            log.error("Payment verification error: {}", e.getMessage());
            return RazorpayDTO.PaymentVerifyResponse.builder()
                    .success(false)
                    .message("Verification error: " + e.getMessage())
                    .build();
        }
    }

    // ── HMAC SHA256 signature verification ───────────────────────────────────
    private boolean verifySignature(String orderId, String paymentId, String signature)
            throws Exception {
        String payload = orderId + "|" + paymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                razorpayConfig.getKeySecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
        StringBuilder hexHash = new StringBuilder();
        for (byte b : hash) {
            hexHash.append(String.format("%02x", b));
        }
        return hexHash.toString().equals(signature);
    }

    private void markPaymentFailed(String razorpayOrderId) {
        razorpayOrderRepo.findByRazorpayOrderId(razorpayOrderId).ifPresent(o -> {
            o.setPaymentStatus(RazorpayOrder.PaymentStatus.FAILED);
            razorpayOrderRepo.save(o);
        });
    }
}
