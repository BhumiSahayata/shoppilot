package com.shoppilot.shoppilot.service;

import com.shoppilot.shoppilot.audit.AuditService;
import com.shoppilot.shoppilot.dto.*;
import com.shoppilot.shoppilot.exception.InvalidOrderStateException;
import com.shoppilot.shoppilot.exception.PaymentException;
import com.shoppilot.shoppilot.exception.ResourceNotFoundException;
import com.shoppilot.shoppilot.model.*;
import com.shoppilot.shoppilot.payment.PaymentGateway;
import com.shoppilot.shoppilot.payment.PaymentOrderResult;
import com.shoppilot.shoppilot.payment.RazorpayPaymentGateway;
import com.shoppilot.shoppilot.repository.OrderRepository;
import com.shoppilot.shoppilot.repository.PaymentRepository;
import com.shoppilot.shoppilot.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;

    private static final int MAX_PAYMENT_ATTEMPTS = 3;
    private static final double MAX_ORDER_AMOUNT_LIMIT = 500000.0; // ₹5 Lakhs bound

    public Order createDraftOrder(CreateDraftOrderRequest request) {
        log.info("[ORDER] Creating draft order for customer: {}", request.getCustomerEmail());

        List<OrderItem> items = new ArrayList<>();
        double subtotal = 0.0;
        double upsellAmount = 0.0;

        // Bounded deterministic validation: Fetch actual prices and stock from MongoDB
        for (String productId : request.getSelectedProductIds()) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

            if (!product.isActive() || product.getStock() <= 0) {
                throw new InvalidOrderStateException("Product '" + product.getName() + "' is out of stock.");
            }

            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .price(product.getPrice()) // Bounded: pulled strictly from DB
                    .quantity(1)
                    .isUpsell(false)
                    .imageUrl(product.getImageUrl())
                    .build();
            items.add(item);
            subtotal += product.getPrice();
        }

        // Bounded upsell: Exactly max 1 upsell item allowed
        if (request.isUpsellAccepted() && request.getUpsellProductId() != null) {
            Product upsellProduct = productRepository.findById(request.getUpsellProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Upsell product not found: " + request.getUpsellProductId()));

            if (upsellProduct.isActive() && upsellProduct.getStock() > 0) {
                OrderItem upsellItem = OrderItem.builder()
                        .productId(upsellProduct.getId())
                        .name(upsellProduct.getName())
                        .price(upsellProduct.getPrice()) // Bounded strictly from DB
                        .quantity(1)
                        .isUpsell(true)
                        .imageUrl(upsellProduct.getImageUrl())
                        .build();
                items.add(upsellItem);
                upsellAmount = upsellProduct.getPrice();

                auditService.logEvent(
                        AuditEventType.UPSELL_ACCEPTED,
                        ActorType.CUSTOMER,
                        null,
                        upsellProduct.getId(),
                        upsellProduct.getPrice(),
                        "Customer accepted complementary upsell recommendation",
                        1.0,
                        "ACCEPTED",
                        "Added accessory: " + upsellProduct.getName()
                );
            }
        } else if (request.getUpsellProductId() != null) {
            auditService.logEvent(
                    AuditEventType.UPSELL_REJECTED,
                    ActorType.CUSTOMER,
                    null,
                    request.getUpsellProductId(),
                    null,
                    "Customer declined complementary upsell",
                    1.0,
                    "DECLINED",
                    "Upsell product: " + request.getUpsellProductId()
            );
        }

        double totalAmount = subtotal + upsellAmount;

        // Safety bounding
        if (totalAmount > MAX_ORDER_AMOUNT_LIMIT) {
            throw new PaymentException(String.format("Order amount ₹%,.0f exceeds maximum safety limit of ₹%,.0f",
                    totalAmount, MAX_ORDER_AMOUNT_LIMIT));
        }

        Order order = Order.builder()
                .customerId(request.getCustomerId() != null ? request.getCustomerId() : "cust_guest")
                .customerName(request.getCustomerName() != null ? request.getCustomerName() : "Demo Customer")
                .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail() : "customer@example.com")
                .items(items)
                .subtotal(subtotal)
                .upsellAmount(upsellAmount)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING_APPROVAL) // GATED: Awaiting customer approval
                .aiAssisted(true)
                .upsellAccepted(request.isUpsellAccepted())
                .aiRecommendationReason(request.getAiRecommendationReason())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Order saved = orderRepository.save(order);

        auditService.logEvent(
                AuditEventType.PRODUCT_RECOMMENDED,
                ActorType.SYSTEM,
                saved.getId(),
                null,
                totalAmount,
                "Draft order prepared with bounded items, awaiting customer approval",
                1.0,
                "PENDING_APPROVAL",
                "Items count: " + items.size()
        );

        return saved;
    }

    public Order approveOrder(String orderId, OrderApprovalRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_APPROVAL && order.getStatus() != OrderStatus.RECOMMENDED) {
            throw new InvalidOrderStateException("Order cannot be approved in current status: " + order.getStatus());
        }

        if (!Boolean.TRUE.equals(request.getApproved())) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(Instant.now());
            Order saved = orderRepository.save(order);

            auditService.logEvent(
                    AuditEventType.CUSTOMER_APPROVAL,
                    ActorType.CUSTOMER,
                    order.getId(),
                    null,
                    order.getTotalAmount(),
                    "Customer explicitly declined the order",
                    1.0,
                    "CANCELLED",
                    request.getCustomerNote()
            );
            return saved;
        }

        order.setStatus(OrderStatus.APPROVED);
        order.setApprovedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        Order approved = orderRepository.save(order);

        auditService.logEvent(
                AuditEventType.CUSTOMER_APPROVAL,
                ActorType.CUSTOMER,
                approved.getId(),
                null,
                approved.getTotalAmount(),
                "Customer explicitly approved order total of ₹" + approved.getTotalAmount(),
                1.0,
                "APPROVED",
                "Gated approval verified by backend"
        );

        return approved;
    }

    public PaymentInitiateResponse initiatePayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Enforce gated approval rule: order MUST be approved first!
        if (order.getStatus() != OrderStatus.APPROVED && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new InvalidOrderStateException("Payment cannot be initiated. Order must be APPROVED first. Current status: " + order.getStatus());
        }

        List<Payment> existingPayments = paymentRepository.findByOrderId(orderId);
        int currentAttempt = existingPayments.size() + 1;

        // Enforce safe stopping rule: Max 3 payment attempts
        if (currentAttempt > MAX_PAYMENT_ATTEMPTS) {
            String stopReason = String.format("Stopping rule triggered: Maximum payment attempts (%d) reached for order %s.",
                    MAX_PAYMENT_ATTEMPTS, orderId);
            auditService.logEvent(
                    AuditEventType.PAYMENT_FAILURE,
                    ActorType.SYSTEM,
                    order.getId(),
                    null,
                    order.getTotalAmount(),
                    stopReason,
                    1.0,
                    "STOPPING_RULE_ENFORCED",
                    "Max retries exceeded"
            );
            throw new PaymentException(stopReason + " Please contact merchant support.");
        }

        // Call Razorpay Payment Gateway
        PaymentOrderResult rzpResult = paymentGateway.createOrder("rcpt_" + orderId.substring(0, Math.min(8, orderId.length())), order.getTotalAmount());

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .razorpayOrderId(rzpResult.getOrderId())
                .attemptCount(currentAttempt)
                .gatewayMode(rzpResult.getGatewayMode())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        auditService.logEvent(
                AuditEventType.RAZORPAY_ORDER_CREATED,
                ActorType.SYSTEM,
                order.getId(),
                null,
                order.getTotalAmount(),
                String.format("Created payment order: %s via %s (Attempt %d/%d)",
                        rzpResult.getOrderId(), rzpResult.getGatewayMode(), currentAttempt, MAX_PAYMENT_ATTEMPTS),
                1.0,
                "ORDER_CREATED",
                "Razorpay Order ID: " + rzpResult.getOrderId()
        );

        return PaymentInitiateResponse.builder()
                .paymentId(savedPayment.getId())
                .orderId(order.getId())
                .razorpayOrderId(rzpResult.getOrderId())
                .razorpayKeyId(rzpResult.getKeyId())
                .amount(order.getTotalAmount())
                .amountInPaise(rzpResult.getAmountInPaise())
                .currency(rzpResult.getCurrency())
                .gatewayMode(rzpResult.getGatewayMode())
                .attemptCount(currentAttempt)
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .build();
    }

    public Payment verifyPayment(PaymentVerificationRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + request.getPaymentId()));

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated order not found: " + payment.getOrderId()));

        // Check for intentional demo failure simulation
        if (request.isSimulateFailure()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Simulated payment gateway failure (demonstrating graceful recovery and retry limits)");
            payment.setUpdatedAt(Instant.now());
            Payment failedPayment = paymentRepository.save(payment);

            order.setStatus(OrderStatus.PAYMENT_FAILED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);

            auditService.logEvent(
                    AuditEventType.PAYMENT_FAILURE,
                    ActorType.SYSTEM,
                    order.getId(),
                    null,
                    order.getTotalAmount(),
                    "Payment failure simulation: transaction was NOT authorized",
                    1.0,
                    "PAYMENT_FAILED",
                    "Order remains unpaid. Safe failure handling confirmed."
            );

            log.warn("[PAYMENT] Payment {} marked as FAILED due to simulation request.", payment.getId());
            return failedPayment;
        }

        // Verify Razorpay signature
        boolean isValid = paymentGateway.verifySignature(
                request.getRazorpayOrderId() != null ? request.getRazorpayOrderId() : payment.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (isValid) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setUpdatedAt(Instant.now());
            Payment successfulPayment = paymentRepository.save(payment);

            order.setStatus(OrderStatus.PAID);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);

            // Deduct product stock in MongoDB
            for (OrderItem item : order.getItems()) {
                productRepository.findById(item.getProductId()).ifPresent(p -> {
                    p.setStock(Math.max(0, p.getStock() - item.getQuantity()));
                    productRepository.save(p);
                });
            }

            auditService.logEvent(
                    AuditEventType.PAYMENT_SUCCESS,
                    ActorType.SYSTEM,
                    order.getId(),
                    null,
                    order.getTotalAmount(),
                    String.format("Payment verified successfully via %s. Payment ID: %s",
                            payment.getGatewayMode(), request.getRazorpayPaymentId()),
                    1.0,
                    "SUCCESS",
                    "Razorpay Payment ID: " + request.getRazorpayPaymentId()
            );

            auditService.logEvent(
                    AuditEventType.ORDER_COMPLETED,
                    ActorType.SYSTEM,
                    order.getId(),
                    null,
                    order.getTotalAmount(),
                    "Order completed. Inventory updated. Merchant revenue credited.",
                    1.0,
                    "COMPLETED",
                    "Total revenue added: ₹" + order.getTotalAmount()
            );

            return successfulPayment;
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay signature verification failed");
            payment.setUpdatedAt(Instant.now());
            Payment failedPayment = paymentRepository.save(payment);

            order.setStatus(OrderStatus.PAYMENT_FAILED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);

            auditService.logEvent(
                    AuditEventType.PAYMENT_FAILURE,
                    ActorType.SYSTEM,
                    order.getId(),
                    null,
                    order.getTotalAmount(),
                    "Payment signature verification failed. Order not marked as paid.",
                    1.0,
                    "FAILED",
                    "Invalid signature for payment: " + request.getRazorpayPaymentId()
            );

            return failedPayment;
        }
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }
}
