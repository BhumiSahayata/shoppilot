package com.shoppilot.shoppilot.payment;

public interface PaymentGateway {

    PaymentOrderResult createOrder(String receipt, double amountInInr);

    boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature);

    String getGatewayMode();

    String getKeyId();
}
