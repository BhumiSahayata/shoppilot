package com.shoppilot.shoppilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateResponse {

    private String paymentId;
    private String orderId;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private double amount; // in INR
    private long amountInPaise;
    private String currency; // "INR"
    private String gatewayMode; // "RAZORPAY_TEST" or "SIMULATED_OFFLINE_MODE"
    private int attemptCount;
    private String customerName;
    private String customerEmail;
}
