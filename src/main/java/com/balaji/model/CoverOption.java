package com.balaji.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cover_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String coverType;       // MATTE, GLOSS, GLASS, ACRYLIC, NONE

    @Column(nullable = false)
    private String displayName;     // "Matte Lamination"

    @Column(nullable = false)
    private String emoji;           // 🌫️ ✨ 🪟 💎 🚫

    @Column(nullable = false)
    private String description;     // "Soft, anti-glare finish"

    @Column(nullable = false)
    private BigDecimal additionalPrice;
}
