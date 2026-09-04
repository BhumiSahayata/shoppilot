package com.shoppilot.shoppilot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;
    private String orderId;
    private double amount;

    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    @Builder.Default
    private int attemptCount = 0;

    private String failureReason;
    private String gatewayMode; // "RAZORPAY_TEST" or "SIMULATED_OFFLINE_MODE"

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
