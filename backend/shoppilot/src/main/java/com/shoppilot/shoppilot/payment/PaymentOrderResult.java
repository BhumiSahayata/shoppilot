package com.shoppilot.shoppilot.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResult {
    private String orderId;
    private long amountInPaise;
    private String currency;
    private String status;
    private String gatewayMode; // "RAZORPAY_TEST" or "SIMULATED_OFFLINE_MODE"
    private String keyId;
}
