package com.balaji.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ── DTO to create a Razorpay order ──────────────────────────────────────────
// (sent from frontend → backend)
public class RazorpayDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderRequest {
        private Long   frameSizeId;
        private Long   beadingOptionId;
        private Long   coverOptionId;
        private String customerName;
        private String customerPhone;
        private String customerEmail;
        private String deliveryAddress;
        private String uploadedPhotoPath;
    }

    // ── Response sent back to frontend (to open Razorpay checkout) ──────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderResponse {
        private String  razorpayOrderId;    // order_xxxxx
        private String  razorpayKeyId;      // rzp_test_xxxxx
        private Integer amountInPaise;      // amount × 100
        private String  currency;           // INR
        private String  customerName;
        private String  customerPhone;
        private String  customerEmail;
        private Long    internalOrderId;    // our DB order id
    }

    // ── Sent from frontend after Razorpay payment success ───────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentVerifyRequest {
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpaySignature;
        private Long   internalOrderId;
    }

    // ── Response after verification ─────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentVerifyResponse {
        private boolean success;
        private String  message;
        private Long    orderId;
        private String  redirectUrl;
    }
}
