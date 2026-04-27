package com.balaji.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "frame_sizes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrameSize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String size;          // "4x6", "5x7" etc.

    @Column(nullable = false)
    private Integer widthInch;

    @Column(nullable = false)
    private Integer heightInch;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private String popularFor;    // "Wallet / ID", "Portrait" etc.

    @Column(nullable = false)
    private String displayLabel;  // "4 × 6"
}
