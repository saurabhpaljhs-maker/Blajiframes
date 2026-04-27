package com.balaji.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "beading_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeadingOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String widthLabel;         // "1 inch", "1.25 inch" etc.

    @Column(nullable = false)
    private String displayWidth;       // "1\"", "1.25\"" etc.

    @Column(nullable = false)
    private Double widthValue;         // 1.0, 1.25, 1.5, 1.75, 2.0

    @Column(nullable = false)
    private BigDecimal additionalPrice;

    @Column(nullable = false)
    private String pattern;            // simple, rope, floral, ornate, premium

    @Column(nullable = false, length = 300)
    private String gradientCss;        // CSS gradient for live preview

    @Column(nullable = false)
    private String description;        // "Classic thin border", "Rope twist" etc.

    @Column(nullable = false)
    private Integer borderPx;          // pixels for preview rendering
}
