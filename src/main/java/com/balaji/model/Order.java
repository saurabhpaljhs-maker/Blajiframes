package com.balaji.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Customer Info
    @NotBlank
    @Column(nullable = false)
    private String customerName;

    @NotBlank
    @Column(nullable = false)
    private String customerPhone;

    @Column
    private String customerEmail;

    @Column(length = 500)
    private String deliveryAddress;

    // Order Details
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "frame_size_id")
    private FrameSize frameSize;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "beading_option_id")
    private BeadingOption beadingOption;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cover_option_id")
    private CoverOption coverOption;

    @Column
    private String uploadedPhotoPath;   // path to uploaded image

    // Pricing
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal beadingPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal coverPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // Payment
    @Column(nullable = false)
    private String paymentMethod;       // UPI, CARD, CASH

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, IN_PROGRESS, READY, DELIVERED, CANCELLED
    }
}
