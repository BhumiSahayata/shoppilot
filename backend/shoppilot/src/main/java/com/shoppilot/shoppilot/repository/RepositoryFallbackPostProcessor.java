package com.shoppilot.shoppilot.repository;

import com.shoppilot.shoppilot.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryFallbackPostProcessor implements BeanPostProcessor {

    private final InMemoryDataStore dataStore;
    private final Set<String> wrappedBeans = new HashSet<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (wrappedBeans.contains(beanName)) {
            return bean;
        }

        if (bean instanceof ProductRepository) {
            wrappedBeans.add(beanName);
            log.info("[FALLBACK] Installing resilient fallback proxy on ProductRepository (bean: {})", beanName);
            return wrapProductRepo((ProductRepository) bean);
        }
        if (bean instanceof OrderRepository) {
            wrappedBeans.add(beanName);
            log.info("[FALLBACK] Installing resilient fallback proxy on OrderRepository (bean: {})", beanName);
            return wrapOrderRepo((OrderRepository) bean);
        }
        if (bean instanceof PaymentRepository) {
            wrappedBeans.add(beanName);
            log.info("[FALLBACK] Installing resilient fallback proxy on PaymentRepository (bean: {})", beanName);
            return wrapPaymentRepo((PaymentRepository) bean);
        }
        if (bean instanceof AuditLogRepository) {
            wrappedBeans.add(beanName);
            log.info("[FALLBACK] Installing resilient fallback proxy on AuditLogRepository (bean: {})", beanName);
            return wrapAuditRepo((AuditLogRepository) bean);
        }
        return bean;
    }

    private ProductRepository wrapProductRepo(ProductRepository real) {
        return (ProductRepository) Proxy.newProxyInstance(
                ProductRepository.class.getClassLoader(),
                new Class<?>[]{ProductRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("equals".equals(name)) return proxy == args[0];
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("toString".equals(name)) return "Proxy[ProductRepository]";

                    if (dataStore.isUseMongo()) {
                        try {
                            Object res = method.invoke(real, args);
                            syncProduct(name, args, res);
                            return res;
                        } catch (Exception e) {
                            dataStore.handleMongoFailure(e.getCause() != null ? e.getCause() : e);
                        }
                    }
                    return handleProductFallback(name, args);
                }
        );
    }

    private Object handleProductFallback(String name, Object[] args) {
        switch (name) {
            case "save":
                return dataStore.saveProduct((Product) args[0]);
            case "saveAll":
                Iterable<?> list = (Iterable<?>) args[0];
                for (Object item : list) dataStore.saveProduct((Product) item);
                return list;
            case "findById":
                return dataStore.findProductById((String) args[0]);
            case "findAll":
                return dataStore.findAllProducts();
            case "findByActiveTrue":
                return dataStore.findProductsByActiveTrue();
            case "findByCategoryIgnoreCaseAndActiveTrue":
                return dataStore.findProductsByCategory((String) args[0]);
            case "findByPriceLessThanEqualAndActiveTrue":
                return dataStore.findProductsByPrice((Double) args[0]);
            case "searchProducts":
                return dataStore.searchProducts((String) args[0]);
            case "count":
                return dataStore.productCount();
            case "deleteAll":
                dataStore.deleteAllProducts();
                return null;
            case "existsById":
                return dataStore.findProductById((String) args[0]).isPresent();
            default:
                return (methodReturnsList(name)) ? List.of() : null;
        }
    }

    private void syncProduct(String name, Object[] args, Object res) {
        if ("save".equals(name) && res instanceof Product) {
            dataStore.saveProduct((Product) res);
        } else if ("deleteAll".equals(name)) {
            dataStore.deleteAllProducts();
        }
    }

    private OrderRepository wrapOrderRepo(OrderRepository real) {
        return (OrderRepository) Proxy.newProxyInstance(
                OrderRepository.class.getClassLoader(),
                new Class<?>[]{OrderRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("equals".equals(name)) return proxy == args[0];
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("toString".equals(name)) return "Proxy[OrderRepository]";

                    if (dataStore.isUseMongo()) {
                        try {
                            Object res = method.invoke(real, args);
                            syncOrder(name, args, res);
                            return res;
                        } catch (Exception e) {
                            dataStore.handleMongoFailure(e.getCause() != null ? e.getCause() : e);
                        }
                    }
                    return handleOrderFallback(name, args);
                }
        );
    }

    private Object handleOrderFallback(String name, Object[] args) {
        switch (name) {
            case "save":
                return dataStore.saveOrder((Order) args[0]);
            case "findById":
                return dataStore.findOrderById((String) args[0]);
            case "findAll":
                return dataStore.findAllOrders();
            case "findByCustomerIdOrderByCreatedAtDesc":
                return dataStore.findOrdersByCustomerId((String) args[0]);
            case "findByStatus":
                return dataStore.findOrdersByStatus((OrderStatus) args[0]);
            case "findByAiAssistedTrue":
                return dataStore.findOrdersByAiAssistedTrue();
            case "findAllByOrderByCreatedAtDesc":
                return dataStore.findAllOrdersOrderByCreatedAtDesc();
            case "count":
                return dataStore.orderCount();
            case "deleteAll":
                dataStore.deleteAllOrders();
                return null;
            default:
                return (methodReturnsList(name)) ? List.of() : null;
        }
    }

    private void syncOrder(String name, Object[] args, Object res) {
        if ("save".equals(name) && res instanceof Order) {
            dataStore.saveOrder((Order) res);
        } else if ("deleteAll".equals(name)) {
            dataStore.deleteAllOrders();
        }
    }

    private PaymentRepository wrapPaymentRepo(PaymentRepository real) {
        return (PaymentRepository) Proxy.newProxyInstance(
                PaymentRepository.class.getClassLoader(),
                new Class<?>[]{PaymentRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("equals".equals(name)) return proxy == args[0];
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("toString".equals(name)) return "Proxy[PaymentRepository]";

                    if (dataStore.isUseMongo()) {
                        try {
                            Object res = method.invoke(real, args);
                            syncPayment(name, args, res);
                            return res;
                        } catch (Exception e) {
                            dataStore.handleMongoFailure(e.getCause() != null ? e.getCause() : e);
                        }
                    }
                    return handlePaymentFallback(name, args);
                }
        );
    }

    private Object handlePaymentFallback(String name, Object[] args) {
        switch (name) {
            case "save":
                return dataStore.savePayment((Payment) args[0]);
            case "findById":
                return dataStore.findPaymentById((String) args[0]);
            case "findAll":
                return dataStore.findPaymentsByOrderId(null);
            case "findByOrderId":
                return dataStore.findPaymentsByOrderId((String) args[0]);
            case "findByRazorpayOrderId":
                return dataStore.findPaymentByRazorpayOrderId((String) args[0]);
            case "findByStatus":
                return dataStore.findPaymentsByStatus((PaymentStatus) args[0]);
            case "count":
                return dataStore.paymentCount();
            case "deleteAll":
                dataStore.deleteAllPayments();
                return null;
            default:
                return (methodReturnsList(name)) ? List.of() : null;
        }
    }

    private void syncPayment(String name, Object[] args, Object res) {
        if ("save".equals(name) && res instanceof Payment) {
            dataStore.savePayment((Payment) res);
        } else if ("deleteAll".equals(name)) {
            dataStore.deleteAllPayments();
        }
    }

    private AuditLogRepository wrapAuditRepo(AuditLogRepository real) {
        return (AuditLogRepository) Proxy.newProxyInstance(
                AuditLogRepository.class.getClassLoader(),
                new Class<?>[]{AuditLogRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("equals".equals(name)) return proxy == args[0];
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("toString".equals(name)) return "Proxy[AuditLogRepository]";

                    if (dataStore.isUseMongo()) {
                        try {
                            Object res = method.invoke(real, args);
                            syncAudit(name, args, res);
                            return res;
                        } catch (Exception e) {
                            dataStore.handleMongoFailure(e.getCause() != null ? e.getCause() : e);
                        }
                    }
                    return handleAuditFallback(name, args);
                }
        );
    }

    private Object handleAuditFallback(String name, Object[] args) {
        switch (name) {
            case "save":
                return dataStore.saveAuditLog((AuditLog) args[0]);
            case "findAllByOrderByTimestampDesc":
            case "findAll":
                return dataStore.findAllAuditLogsDesc();
            case "findByOrderIdOrderByTimestampAsc":
                return dataStore.findAuditLogsByOrderId((String) args[0]);
            case "count":
                return dataStore.auditLogCount();
            case "deleteAll":
                dataStore.deleteAllAuditLogs();
                return null;
            default:
                return (methodReturnsList(name)) ? List.of() : null;
        }
    }

    private void syncAudit(String name, Object[] args, Object res) {
        if ("save".equals(name) && res instanceof AuditLog) {
            dataStore.saveAuditLog((AuditLog) res);
        } else if ("deleteAll".equals(name)) {
            dataStore.deleteAllAuditLogs();
        }
    }

    private boolean methodReturnsList(String name) {
        return name.startsWith("find") || name.startsWith("get") || name.startsWith("search");
    }
}
