package com.balaji.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationDTO {
    private BigDecimal basePrice;
    private BigDecimal beadingPrice;
    private BigDecimal coverPrice;
    private BigDecimal totalPrice;
    private String sizeLabel;
    private String beadingLabel;
    private String coverLabel;
}
