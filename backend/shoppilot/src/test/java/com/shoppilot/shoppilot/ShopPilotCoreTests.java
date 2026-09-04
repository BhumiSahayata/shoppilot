package com.shoppilot.shoppilot;

import com.shoppilot.shoppilot.ai.DeterministicFallbackEngine;
import com.shoppilot.shoppilot.audit.AuditService;
import com.shoppilot.shoppilot.dto.*;
import com.shoppilot.shoppilot.exception.InvalidOrderStateException;
import com.shoppilot.shoppilot.exception.PaymentException;
import com.shoppilot.shoppilot.model.*;
import com.shoppilot.shoppilot.payment.PaymentGateway;
import com.shoppilot.shoppilot.payment.PaymentOrderResult;
import com.shoppilot.shoppilot.repository.AuditLogRepository;
import com.shoppilot.shoppilot.repository.OrderRepository;
import com.shoppilot.shoppilot.repository.PaymentRepository;
import com.shoppilot.shoppilot.repository.ProductRepository;
import com.shoppilot.shoppilot.service.DashboardService;
import com.shoppilot.shoppilot.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopPilotCoreTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private PaymentGateway paymentGateway;

    private DeterministicFallbackEngine fallbackEngine;
    private OrderService orderService;
    private DashboardService dashboardService;

    private Product laptop;
    private Product bag;

    @BeforeEach
    void setUp() {
        fallbackEngine = new DeterministicFallbackEngine(productRepository, auditService);
        orderService = new OrderService(orderRepository, productRepository, paymentRepository, paymentGateway, auditService);
        dashboardService = new DashboardService(orderRepository, auditService);

        bag = Product.builder()
                .id("P_BAG")
                .name("Pro Laptop Bag")
                .category("accessory")
                .price(2499.0)
                .stock(10)
                .active(true)
                .build();

        laptop = Product.builder()
                .id("P_LAPTOP")
                .name("Coding Laptop 16GB")
                .category("laptop")
                .price(49999.0)
                .stock(5)
                .tags(List.of("coding", "student"))
                .complementaryProductIds(List.of("P_BAG"))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("1 & 2. Natural language intent extraction & budget filtering")
    void testIntentExtractionAndBudget() {
        AiShoppingResponse.IntentDto intent = fallbackEngine.parseIntent("I need a laptop under 60k for coding");
        assertEquals("laptop", intent.getCategory());
        assertEquals("coding", intent.getUseCase());
        assertEquals(60000.0, intent.getMaxBudget());

        when(productRepository.findByActiveTrue()).thenReturn(List.of(laptop, bag));
        when(productRepository.findById("P_BAG")).thenReturn(Optional.of(bag));

        AiShoppingResponse response = fallbackEngine.executeFallback("I need a laptop under 60k for coding");
        assertNotNull(response);
        assertEquals("SAFE_FALLBACK", response.getEngineUsed());
        assertFalse(response.getRecommendations().isEmpty());
        assertEquals(laptop.getId(), response.getRecommendations().get(0).getProduct().getId());
        assertTrue(response.getRecommendations().get(0).getReason().contains("coding"));
    }

    @Test
    @DisplayName("3. Bounded upsell constraint (max 1 accessory, strictly verified)")
    void testBoundedUpsellLimit() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of(laptop));
        when(productRepository.findById("P_BAG")).thenReturn(Optional.of(bag));

        AiShoppingResponse response = fallbackEngine.executeFallback("laptop for coding under 60000");
        assertNotNull(response.getUpsell());
        assertEquals("P_BAG", response.getUpsell().getProduct().getId());
        assertTrue(response.getUpsell().isUpsell());
    }

    @Test
    @DisplayName("4. Customer approval requirement: Payment cannot start before approval")
    void testCustomerApprovalRequirement() {
        Order draftOrder = Order.builder()
                .id("ORD_101")
                .totalAmount(52498.0)
                .status(OrderStatus.PENDING_APPROVAL) // Not yet approved!
                .build();

        when(orderRepository.findById("ORD_101")).thenReturn(Optional.of(draftOrder));

        // Attempt to initiate payment before approval must fail
        assertThrows(InvalidOrderStateException.class, () -> orderService.initiatePayment("ORD_101"));

        // Now test approving the order
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Order approved = orderService.approveOrder("ORD_101", OrderApprovalRequest.builder().approved(true).build());
        assertEquals(OrderStatus.APPROVED, approved.getStatus());
    }

    @Test
    @DisplayName("5. Maximum payment amount bounding (rejects above limit)")
    void testMaxPaymentAmountBounding() {
        Product expensiveServer = Product.builder()
                .id("P_EXPENSIVE")
                .name("Enterprise Cluster")
                .category("server")
                .price(999999.0) // Exceeds ₹5 Lakhs bound
                .stock(1)
                .active(true)
                .build();

        when(productRepository.findById("P_EXPENSIVE")).thenReturn(Optional.of(expensiveServer));

        CreateDraftOrderRequest request = CreateDraftOrderRequest.builder()
                .selectedProductIds(List.of("P_EXPENSIVE"))
                .build();

        assertThrows(PaymentException.class, () -> orderService.createDraftOrder(request));
    }

    @Test
    @DisplayName("6. Safe stopping rule: Maximum 3 payment retries")
    void testPaymentRetryLimitStoppingRule() {
        Order approvedOrder = Order.builder()
                .id("ORD_RETRY")
                .totalAmount(49999.0)
                .status(OrderStatus.APPROVED)
                .build();

        when(orderRepository.findById("ORD_RETRY")).thenReturn(Optional.of(approvedOrder));

        // 3 existing attempts already made
        List<Payment> existingPayments = List.of(
                Payment.builder().id("PAY_1").build(),
                Payment.builder().id("PAY_2").build(),
                Payment.builder().id("PAY_3").build()
        );
        when(paymentRepository.findByOrderId("ORD_RETRY")).thenReturn(existingPayments);

        // 4th attempt must be stopped by safety rule
        PaymentException ex = assertThrows(PaymentException.class, () -> orderService.initiatePayment("ORD_RETRY"));
        assertTrue(ex.getMessage().contains("Maximum payment attempts"));
    }

    @Test
    @DisplayName("7. Payment verification success & stock deduction")
    void testPaymentSuccessAndStockDeduction() {
        Order approvedOrder = Order.builder()
                .id("ORD_SUCCESS")
                .totalAmount(49999.0)
                .status(OrderStatus.PAYMENT_PENDING)
                .items(List.of(OrderItem.builder().productId("P_LAPTOP").quantity(1).build()))
                .build();

        Payment pendingPayment = Payment.builder()
                .id("PAY_100")
                .orderId("ORD_SUCCESS")
                .razorpayOrderId("order_test_123")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById("PAY_100")).thenReturn(Optional.of(pendingPayment));
        when(orderRepository.findById("ORD_SUCCESS")).thenReturn(Optional.of(approvedOrder));
        when(paymentGateway.verifySignature(eq("order_test_123"), eq("pay_abc"), eq("sig_valid"))).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findById("P_LAPTOP")).thenReturn(Optional.of(laptop));

        PaymentVerificationRequest req = PaymentVerificationRequest.builder()
                .paymentId("PAY_100")
                .razorpayOrderId("order_test_123")
                .razorpayPaymentId("pay_abc")
                .razorpaySignature("sig_valid")
                .build();

        Payment verified = orderService.verifyPayment(req);
        assertEquals(PaymentStatus.SUCCESS, verified.getStatus());
        assertEquals(OrderStatus.PAID, approvedOrder.getStatus());
        assertEquals(4, laptop.getStock()); // Deducted from 5 to 4
    }

    @Test
    @DisplayName("8. Graceful payment failure handling (no false success)")
    void testGracefulPaymentFailure() {
        Order approvedOrder = Order.builder()
                .id("ORD_FAIL")
                .totalAmount(49999.0)
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        Payment pendingPayment = Payment.builder()
                .id("PAY_FAIL")
                .orderId("ORD_FAIL")
                .razorpayOrderId("order_test_fail")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById("PAY_FAIL")).thenReturn(Optional.of(pendingPayment));
        when(orderRepository.findById("ORD_FAIL")).thenReturn(Optional.of(approvedOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // Simulation failure triggered
        PaymentVerificationRequest req = PaymentVerificationRequest.builder()
                .paymentId("PAY_FAIL")
                .simulateFailure(true)
                .build();

        Payment result = orderService.verifyPayment(req);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertEquals(OrderStatus.PAYMENT_FAILED, approvedOrder.getStatus());
        assertNotEquals(OrderStatus.PAID, approvedOrder.getStatus()); // Strict no false success!
    }

    @Test
    @DisplayName("9. Dashboard analytics calculation from real orders")
    void testDashboardCalculation() {
        Order o1 = Order.builder().totalAmount(50000.0).upsellAmount(2500.0).status(OrderStatus.PAID).aiAssisted(true).upsellAccepted(true).build();
        Order o2 = Order.builder().totalAmount(20000.0).upsellAmount(0.0).status(OrderStatus.PAID).aiAssisted(false).upsellAccepted(false).build();

        when(orderRepository.findByStatus(OrderStatus.PAID)).thenReturn(List.of(o1, o2));
        when(orderRepository.findByAiAssistedTrue()).thenReturn(List.of(o1));
        when(auditService.getRecentLogs()).thenReturn(new ArrayList<>());

        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
        assertEquals(70000.0, summary.getTotalRevenue());
        assertEquals(50000.0, summary.getAiAssistedRevenue());
        assertEquals(2500.0, summary.getUpsellRevenue());
        assertEquals(2, summary.getTotalOrders());
        assertEquals(1, summary.getAiAssistedOrders());
        assertEquals(35000.0, summary.getAverageOrderValue());
        assertEquals(100.0, summary.getUpsellAcceptanceRate());
    }
}
