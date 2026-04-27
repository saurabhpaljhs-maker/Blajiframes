package com.balaji.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "razorpay_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Razorpay's order id (e.g. order_xxxxxxxxxxxxx)
    @Column(nullable = false, unique = true)
    private String razorpayOrderId;

    // Razorpay's payment id (filled after payment success)
    @Column
    private String razorpayPaymentId;

    // Razorpay's signature (for verification)
    @Column
    private String razorpaySignature;

    // Linked to our internal Order
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;        // in INR

    @Column(nullable = false)
    private Integer amountInPaise;    // amount × 100 (Razorpay works in paise)

    @Column(nullable = false)
    private String currency;          // INR

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime paidAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.currency  = "INR";
    }

    public enum PaymentStatus {
        CREATED,    // Razorpay order created, not yet paid
        PAID,       // Payment successful & verified
        FAILED,     // Payment failed
        REFUNDED    // Payment refunded
    }
}
