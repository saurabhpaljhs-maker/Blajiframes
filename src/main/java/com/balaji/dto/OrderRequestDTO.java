package com.balaji.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String customerName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit Indian mobile number")
    private String customerPhone;

    @Email(message = "Enter a valid email address")
    private String customerEmail;

    @Size(max = 500, message = "Address too long")
    private String deliveryAddress;

    @NotNull(message = "Frame size is required")
    @Positive(message = "Invalid frame size")
    private Long frameSizeId;

    @NotNull(message = "Beading option is required")
    @Positive(message = "Invalid beading option")
    private Long beadingOptionId;

    @NotNull(message = "Cover option is required")
    @Positive(message = "Invalid cover option")
    private Long coverOptionId;

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(UPI|CARD|CASH|RAZORPAY)$", message = "Invalid payment method")
    private String paymentMethod;

    private String uploadedPhotoPath;
}
