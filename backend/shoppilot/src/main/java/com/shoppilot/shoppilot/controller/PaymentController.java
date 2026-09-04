package com.shoppilot.shoppilot.controller;

import com.shoppilot.shoppilot.dto.ApiResponse;
import com.shoppilot.shoppilot.dto.PaymentInitiateResponse;
import com.shoppilot.shoppilot.dto.PaymentVerificationRequest;
import com.shoppilot.shoppilot.model.Payment;
import com.shoppilot.shoppilot.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;

    @PostMapping("/initiate/{orderId}")
    public ResponseEntity<ApiResponse<PaymentInitiateResponse>> initiatePayment(@PathVariable String orderId) {
        PaymentInitiateResponse response = orderService.initiatePayment(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Razorpay test order initialized", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Payment>> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {
        Payment payment = orderService.verifyPayment(request);
        String msg = (payment.getStatus() == com.shoppilot.shoppilot.model.PaymentStatus.SUCCESS)
                ? "Payment verified successfully"
                : "Payment verification failed: " + payment.getFailureReason();
        return ResponseEntity.ok(ApiResponse.ok(msg, payment));
    }
}
