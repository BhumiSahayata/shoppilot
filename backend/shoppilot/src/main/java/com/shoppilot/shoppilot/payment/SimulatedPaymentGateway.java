package com.shoppilot.shoppilot.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public PaymentOrderResult createOrder(String receipt, double amountInInr) {
        String simOrderId = "sim_order_" + UUID.randomUUID().toString().substring(0, 10);
        long amountInPaise = Math.round(amountInInr * 100);

        log.warn("[OFFLINE_FALLBACK] Razorpay keys not detected. Generated offline test order: {} (Amount: ₹{})",
                simOrderId, amountInInr);

        return PaymentOrderResult.builder()
                .orderId(simOrderId)
                .amountInPaise(amountInPaise)
                .currency("INR")
                .status("created")
                .gatewayMode("SIMULATED_OFFLINE_MODE")
                .keyId("simulated_test_key_offline")
                .build();
    }

    @Override
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        // Safe offline verification: checks that test identifiers are present and non-empty
        boolean valid = razorpayOrderId != null && !razorpayOrderId.isBlank()
                && razorpayPaymentId != null && !razorpayPaymentId.isBlank();
        log.info("[OFFLINE_FALLBACK] Verifying simulated payment: orderId={}, paymentId={}, valid={}",
                razorpayOrderId, razorpayPaymentId, valid);
        return valid;
    }

    @Override
    public String getGatewayMode() {
        return "SIMULATED_OFFLINE_MODE";
    }

    @Override
    public String getKeyId() {
        return "simulated_test_key_offline";
    }
}
