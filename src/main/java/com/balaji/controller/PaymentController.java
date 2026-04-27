package com.balaji.controller;

import com.balaji.dto.RazorpayDTO;
import com.balaji.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final RazorpayService razorpayService;

    // ── Step 1: Frontend calls this to create Razorpay order ─────────────────
    // Returns: razorpayOrderId, keyId, amount — needed to open Razorpay checkout
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestBody RazorpayDTO.CreateOrderRequest req) {
        try {
            RazorpayDTO.CreateOrderResponse resp = razorpayService.createRazorpayOrder(req);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Create Razorpay order error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Step 2: Frontend calls this after Razorpay payment success ────────────
    // Verifies HMAC signature → confirms order
    @PostMapping("/verify")
    public ResponseEntity<RazorpayDTO.PaymentVerifyResponse> verifyPayment(
            @RequestBody RazorpayDTO.PaymentVerifyRequest req) {
        RazorpayDTO.PaymentVerifyResponse resp = razorpayService.verifyPayment(req);
        return ResponseEntity.ok(resp);
    }

    // ── Razorpay Webhook (optional — for missed callbacks) ────────────────────
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String sig) {
        // For production: verify webhook signature here
        log.info("Razorpay webhook received");
        return ResponseEntity.ok("OK");
    }
}
