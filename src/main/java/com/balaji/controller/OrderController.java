package com.balaji.controller;

import com.balaji.dto.OrderRequestDTO;
import com.balaji.model.Order;
import com.balaji.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ── Place order (AJAX from frontend) ──────────────────────────────────────
    @PostMapping("/api/order/place")
    @ResponseBody
    public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequestDTO req) {
        // @Valid triggers validation — errors caught by GlobalExceptionHandler
        Order order = orderService.placeOrder(req);
        return ResponseEntity.ok(Map.of(
                "orderId",      order.getId(),
                "totalPrice",   order.getTotalPrice(),
                "customerName", order.getCustomerName(),
                "status",       order.getStatus().name(),
                "message",      "Order placed successfully!"
        ));
    }

    // ── Order confirmation page ───────────────────────────────────────────────
    @GetMapping("/order/confirmation/{id}")
    public String confirmation(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderById(id));
        return "confirmation";
    }

    // ── Admin: all orders (SECURED) ───────────────────────────────────────────
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders";
    }

    // ── Admin: update order status (SECURED) ──────────────────────────────────
    @PostMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Order.OrderStatus s = Order.OrderStatus.valueOf(status.toUpperCase());
        Order updated = orderService.updateStatus(id, s);
        return ResponseEntity.ok(Map.of("status", updated.getStatus().name()));
    }
}
