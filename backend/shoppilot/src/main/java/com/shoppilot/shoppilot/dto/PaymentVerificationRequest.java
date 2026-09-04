package com.shoppilot.shoppilot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationRequest {

    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    private boolean simulateFailure; // For hackathon demonstration of failure handling
}
