package com.shoppilot.shoppilot.repository;

import com.shoppilot.shoppilot.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InMemoryDataStore {

    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();

    private final AtomicBoolean useMongo = new AtomicBoolean(true);

    public InMemoryDataStore(@Value("${spring.mongodb.uri:}") String mongoUri) {
        log.info("[DATA_STORE] Initialized InMemoryDataStore with dynamic fallback capabilities.");
    }

    public boolean isUseMongo() {
        return useMongo.get();
    }

    public void handleMongoFailure(Throwable t) {
        if (useMongo.compareAndSet(true, false)) {
            log.warn("[DATA_STORE] MongoDB Atlas connection failed ({}). Seamlessly activating high-performance in-memory repository fallback.",
                    t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
    }

    // --- Product Handlers ---

    public Product saveProduct(Product p) {
        if (p.getId() == null || p.getId().trim().isEmpty()) {
            p.setId("PROD_" + UUID.randomUUID().toString().substring(0, 8));
        }
        products.put(p.getId(), p);
        return p;
    }

    public Optional<Product> findProductById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    public List<Product> findAllProducts() {
        return new ArrayList<>(products.values());
    }

    public List<Product> findProductsByActiveTrue() {
        return products.values().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());
    }

    public List<Product> findProductsByCategory(String category) {
        return products.values().stream()
                .filter(Product::isActive)
                .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public List<Product> findProductsByPrice(double maxPrice) {
        return products.values().stream()
                .filter(Product::isActive)
                .filter(p -> p.getPrice() <= maxPrice)
                .collect(Collectors.toList());
    }

    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findProductsByActiveTrue();
        }
        String lower = keyword.toLowerCase();
        return products.values().stream()
                .filter(Product::isActive)
                .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(lower)) ||
                             (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower)) ||
                             (p.getCategory() != null && p.getCategory().toLowerCase().contains(lower)) ||
                             (p.getTags() != null && p.getTags().stream().anyMatch(t -> t.toLowerCase().contains(lower))))
                .collect(Collectors.toList());
    }

    public long productCount() {
        return products.size();
    }

    public void deleteAllProducts() {
        products.clear();
    }

    // --- Order Handlers ---

    public Order saveOrder(Order o) {
        if (o.getId() == null || o.getId().trim().isEmpty()) {
            o.setId("ORD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (o.getCreatedAt() == null) {
            o.setCreatedAt(Instant.now());
        }
        o.setUpdatedAt(Instant.now());
        orders.put(o.getId(), o);
        return o;
    }

    public Optional<Order> findOrderById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public List<Order> findAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public List<Order> findOrdersByCustomerId(String customerId) {
        return orders.values().stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<Order> findOrdersByStatus(OrderStatus status) {
        return orders.values().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Order> findOrdersByAiAssistedTrue() {
        return orders.values().stream()
                .filter(Order::isAiAssisted)
                .collect(Collectors.toList());
    }

    public List<Order> findAllOrdersOrderByCreatedAtDesc() {
        return orders.values().stream()
                .sorted((a, b) -> {
                    Instant ta = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH;
                    Instant tb = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH;
                    return tb.compareTo(ta);
                })
                .collect(Collectors.toList());
    }

    public long orderCount() {
        return orders.size();
    }

    public void deleteAllOrders() {
        orders.clear();
    }

    // --- Payment Handlers ---

    public Payment savePayment(Payment p) {
        if (p.getId() == null || p.getId().trim().isEmpty()) {
            p.setId("PAY_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (p.getCreatedAt() == null) {
            p.setCreatedAt(Instant.now());
        }
        p.setUpdatedAt(Instant.now());
        payments.put(p.getId(), p);
        return p;
    }

    public Optional<Payment> findPaymentById(String id) {
        return Optional.ofNullable(payments.get(id));
    }

    public List<Payment> findPaymentsByOrderId(String orderId) {
        return payments.values().stream()
                .filter(p -> orderId != null && orderId.equals(p.getOrderId()))
                .collect(Collectors.toList());
    }

    public Optional<Payment> findPaymentByRazorpayOrderId(String rzpOrderId) {
        return payments.values().stream()
                .filter(p -> rzpOrderId != null && rzpOrderId.equals(p.getRazorpayOrderId()))
                .findFirst();
    }

    public List<Payment> findPaymentsByStatus(PaymentStatus status) {
        return payments.values().stream()
                .filter(p -> p.getStatus() == status)
                .collect(Collectors.toList());
    }

    public long paymentCount() {
        return payments.size();
    }

    public void deleteAllPayments() {
        payments.clear();
    }

    // --- Audit Log Handlers ---

    public AuditLog saveAuditLog(AuditLog a) {
        if (a.getId() == null || a.getId().trim().isEmpty()) {
            a.setId("AUD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (a.getTimestamp() == null) {
            a.setTimestamp(Instant.now());
        }
        auditLogs.add(a);
        return a;
    }

    public List<AuditLog> findAuditLogsByOrderId(String orderId) {
        return auditLogs.stream()
                .filter(a -> orderId != null && orderId.equals(a.getOrderId()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp))
                .collect(Collectors.toList());
    }

    public List<AuditLog> findAllAuditLogsDesc() {
        return auditLogs.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    public long auditLogCount() {
        return auditLogs.size();
    }

    public void deleteAllAuditLogs() {
        auditLogs.clear();
    }
}
