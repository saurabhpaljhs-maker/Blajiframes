package com.balaji.service;

import com.balaji.dto.OrderRequestDTO;
import com.balaji.dto.PriceCalculationDTO;
import com.balaji.exception.BalajiExceptions.*;
import com.balaji.model.*;
import com.balaji.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final FrameService    frameService;

    @Transactional
    public Order placeOrder(OrderRequestDTO req) {
        // Validate IDs exist (throws ResourceNotFoundEx if not)
        PriceCalculationDTO price = frameService.calculatePrice(
                req.getFrameSizeId(), req.getBeadingOptionId(), req.getCoverOptionId());

        FrameSize     size    = frameService.getSizeById(req.getFrameSizeId());
        BeadingOption bead    = frameService.getBeadingById(req.getBeadingOptionId());
        CoverOption   cover   = frameService.getCoverById(req.getCoverOptionId());

        Order order = Order.builder()
                .customerName(sanitize(req.getCustomerName()))
                .customerPhone(req.getCustomerPhone().trim())
                .customerEmail(req.getCustomerEmail())
                .deliveryAddress(req.getDeliveryAddress())
                .frameSize(size)
                .beadingOption(bead)
                .coverOption(cover)
                .uploadedPhotoPath(req.getUploadedPhotoPath())
                .basePrice(price.getBasePrice())
                .beadingPrice(price.getBeadingPrice())
                .coverPrice(price.getCoverPrice())
                .totalPrice(price.getTotalPrice())
                .paymentMethod(req.getPaymentMethod())
                .status(Order.OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order placed: ID={} Customer={} Total={}",
                saved.getId(), saved.getCustomerName(), saved.getTotalPrice());
        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Order", id));
    }

    @Transactional
    public Order updateStatus(Long id, Order.OrderStatus status) {
        Order order = getOrderById(id);
        order.setStatus(status);
        log.info("Order #{} status -> {}", id, status);
        return orderRepository.save(order);
    }

    // Strip any dangerous HTML/script tags from input
    private String sanitize(String input) {
        if (input == null) return null;
        return input.trim()
                .replaceAll("<[^>]*>", "")          // remove HTML tags
                .replaceAll("[<>\"'%;()&+]", "");   // remove special chars
    }
}
