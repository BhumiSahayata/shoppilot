package com.shoppilot.shoppilot.demo;

import com.shoppilot.shoppilot.audit.AuditService;
import com.shoppilot.shoppilot.model.*;
import com.shoppilot.shoppilot.repository.AuditLogRepository;
import com.shoppilot.shoppilot.repository.OrderRepository;
import com.shoppilot.shoppilot.repository.PaymentRepository;
import com.shoppilot.shoppilot.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    @Override
    public void run(String... args) {
        try {
            if (productRepository.count() == 0) {
                log.info("[SEEDER] Database is empty. Seeding initial catalog and demo data...");
                seedDemoData();
            }
        } catch (Exception e) {
            log.warn("[SEEDER] Initial MongoDB check encountered error: {}. Seeding catalog directly...", e.getMessage());
            seedDemoData();
        }
    }

    public synchronized void resetAndSeed() {
        log.info("[SEEDER] Resetting all data in MongoDB Atlas...");
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        auditLogRepository.deleteAll();
        productRepository.deleteAll();
        seedDemoData();
        log.info("[SEEDER] Database reset and seeded successfully.");
    }

    public synchronized void seedDemoData() {
        if (productRepository.count() > 0) {
            log.info("[SEEDER] Products already present. Skipping catalog seed.");
            return;
        }

        // 1. Accessories first (so we have IDs for complementary product links)
        Product bag = productRepository.save(Product.builder()
                .name("ShopPilot Pro Waterproof Laptop Backpack (30L)")
                .category("accessory")
                .description("Anti-theft water-resistant travel backpack with padded 16\" laptop compartment.")
                .price(2499.0)
                .stock(45)
                .imageUrl("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600")
                .tags(List.of("travel", "accessory", "student", "office"))
                .active(true)
                .build());

        Product mouse = productRepository.save(Product.builder()
                .name("Logi Precision Wireless Ergonomic Mouse")
                .category("mouse")
                .description("Silent wireless optical mouse with ergonomic grip, 4000 DPI, and multi-device pairing.")
                .price(1499.0)
                .stock(60)
                .imageUrl("https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600")
                .tags(List.of("accessory", "coding", "office", "wireless"))
                .active(true)
                .build());

        Product phoneCase = productRepository.save(Product.builder()
                .name("ArmorShield Drop-Protection MagSafe Case")
                .category("accessory")
                .description("Military-grade shockproof bumper case with MagSafe magnetic ring.")
                .price(999.0)
                .stock(80)
                .imageUrl("https://images.unsplash.com/photo-1580910051074-3eb694886505?w=600")
                .tags(List.of("accessory", "phone", "protection"))
                .active(true)
                .build());

        Product memoryCard = productRepository.save(Product.builder()
                .name("UltraSpeed 128GB V30 4K MicroSD / SD Card")
                .category("accessory")
                .description("High-speed 160MB/s UHS-I memory card with full-size SD adapter for 4K video recording.")
                .price(1299.0)
                .stock(50)
                .imageUrl("https://images.unsplash.com/photo-1598256989800-fe5f95da9787?w=600")
                .tags(List.of("accessory", "camera", "storage"))
                .active(true)
                .build());

        // 2. Primary Products with complementary accessory references
        Product laptopA = productRepository.save(Product.builder()
                .name("Dell Vostro 15 Developer Edition (16GB RAM / 512GB SSD)")
                .category("laptop")
                .description("Intel Core i5 13th Gen, 16GB DDR4 RAM, 512GB NVMe SSD, 15.6\" FHD Display. Ideal for Java, Docker, and web development.")
                .price(49999.0)
                .stock(18)
                .imageUrl("https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=600")
                .tags(List.of("coding", "developer", "student", "laptop"))
                .complementaryProductIds(List.of(bag.getId(), mouse.getId()))
                .active(true)
                .build());

        Product laptopB = productRepository.save(Product.builder()
                .name("HP Pavilion 14 Slim (16GB RAM / 1TB SSD)")
                .category("laptop")
                .description("AMD Ryzen 5 7530U, 16GB RAM, 1TB NVMe High-Speed SSD, Backlit keyboard, lightweight 1.4kg chassis.")
                .price(57999.0)
                .stock(12)
                .imageUrl("https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600")
                .tags(List.of("coding", "developer", "student", "portable", "laptop"))
                .complementaryProductIds(List.of(bag.getId(), mouse.getId()))
                .active(true)
                .build());

        Product laptopC = productRepository.save(Product.builder()
                .name("ASUS Vivobook 16 Studio Edition")
                .category("laptop")
                .description("Intel Core i7 12th Gen, 16GB RAM, 512GB SSD, 16-inch 16:10 display.")
                .price(68999.0)
                .stock(8)
                .imageUrl("https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=600")
                .tags(List.of("coding", "office", "content creation", "laptop"))
                .complementaryProductIds(List.of(bag.getId()))
                .active(true)
                .build());

        Product phoneA = productRepository.save(Product.builder()
                .name("OnePlus Nord CE4 5G (8GB RAM / 128GB)")
                .category("phone")
                .description("Snapdragon 7 Gen 3, 100W SUPERVOOC charging, 50MP Sony LYT-600 OIS camera.")
                .price(24999.0)
                .stock(25)
                .imageUrl("https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600")
                .tags(List.of("phone", "5g", "fast charging"))
                .complementaryProductIds(List.of(phoneCase.getId()))
                .active(true)
                .build());

        Product phoneB = productRepository.save(Product.builder()
                .name("Samsung Galaxy A35 5G (8GB / 256GB)")
                .category("phone")
                .description("Super AMOLED 120Hz display, IP67 water resistance, 50MP triple camera setup.")
                .price(28999.0)
                .stock(20)
                .imageUrl("https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=600")
                .tags(List.of("phone", "amoled", "samsung"))
                .complementaryProductIds(List.of(phoneCase.getId()))
                .active(true)
                .build());

        Product monitorA = productRepository.save(Product.builder()
                .name("LG 27\" QHD IPS Coding Monitor (USB-C)")
                .category("monitor")
                .description("2560x1440 2K QHD, 99% sRGB, HDR10, USB-C 65W power delivery for laptops, pivot ergonomic stand.")
                .price(22499.0)
                .stock(15)
                .imageUrl("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600")
                .tags(List.of("coding", "monitor", "office", "developer"))
                .complementaryProductIds(List.of(mouse.getId()))
                .active(true)
                .build());

        Product keyboardA = productRepository.save(Product.builder()
                .name("Keychron K2 V2 Mechanical Keyboard (Hot-swappable)")
                .category("keyboard")
                .description("75% compact layout, Gateron Brown tactile switches, Bluetooth 5.1 & wired, Mac/Windows layout.")
                .price(6999.0)
                .stock(30)
                .imageUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600")
                .tags(List.of("coding", "developer", "keyboard", "mechanical"))
                .complementaryProductIds(List.of(mouse.getId()))
                .active(true)
                .build());

        Product headphonesA = productRepository.save(Product.builder()
                .name("Sony WH-CH720N Noise Cancelling Headphones")
                .category("audio")
                .description("Active Noise Cancellation, Dual Noise Sensor technology, 35-hour battery life with mic.")
                .price(8999.0)
                .stock(22)
                .imageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600")
                .tags(List.of("audio", "anc", "coding", "music", "travel"))
                .complementaryProductIds(List.of(bag.getId()))
                .active(true)
                .build());

        Product cameraA = productRepository.save(Product.builder()
                .name("Sony Alpha ZV-E10 Mirrorless Creator Camera")
                .category("camera")
                .description("24.2MP APS-C sensor, 4K video, interchangeable lens system, directional 3-capsule microphone.")
                .price(61499.0)
                .stock(9)
                .imageUrl("https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600")
                .tags(List.of("camera", "video", "content creation"))
                .complementaryProductIds(List.of(memoryCard.getId(), bag.getId()))
                .active(true)
                .build());

        // 3. Seed Realistic Historical Orders & Metrics
        seedHistoricalOrders(laptopA, laptopB, bag, mouse, phoneA, phoneCase, monitorA, keyboardA);

        log.info("[SEEDER] Seeded catalog with {} products and realistic historical orders.", productRepository.count());
    }

    private void seedHistoricalOrders(Product laptopA, Product laptopB, Product bag, Product mouse,
                                     Product phoneA, Product phoneCase, Product monitorA, Product keyboardA) {
        Instant baseTime = Instant.now().minus(5, ChronoUnit.DAYS);

        // Order 1: AI-assisted + Upsell accepted (Laptop A + Bag)
        createHistoricalOrder("cust_rahul", "Rahul Sharma", "rahul.s@example.com",
                List.of(
                        OrderItem.builder().productId(laptopA.getId()).name(laptopA.getName()).price(laptopA.getPrice()).quantity(1).isUpsell(false).build(),
                        OrderItem.builder().productId(bag.getId()).name(bag.getName()).price(bag.getPrice()).quantity(1).isUpsell(true).build()
                ),
                laptopA.getPrice(), bag.getPrice(), laptopA.getPrice() + bag.getPrice(),
                OrderStatus.PAID, true, true, "Coding laptop within ₹60k budget with complementary laptop bag",
                baseTime.plus(4, ChronoUnit.HOURS));

        // Order 2: AI-assisted + Upsell accepted (Phone A + Case)
        createHistoricalOrder("cust_priya", "Priya Nair", "priya.n@example.com",
                List.of(
                        OrderItem.builder().productId(phoneA.getId()).name(phoneA.getName()).price(phoneA.getPrice()).quantity(1).isUpsell(false).build(),
                        OrderItem.builder().productId(phoneCase.getId()).name(phoneCase.getName()).price(phoneCase.getPrice()).quantity(1).isUpsell(true).build()
                ),
                phoneA.getPrice(), phoneCase.getPrice(), phoneA.getPrice() + phoneCase.getPrice(),
                OrderStatus.PAID, true, true, "Recommended fast-charging phone under ₹30k with protective armor case",
                baseTime.plus(12, ChronoUnit.HOURS));

        // Order 3: Direct non-AI purchase (Monitor A)
        createHistoricalOrder("cust_amit", "Amit Verma", "amit.v@example.com",
                List.of(
                        OrderItem.builder().productId(monitorA.getId()).name(monitorA.getName()).price(monitorA.getPrice()).quantity(1).isUpsell(false).build()
                ),
                monitorA.getPrice(), 0.0, monitorA.getPrice(),
                OrderStatus.PAID, false, false, null,
                baseTime.plus(24, ChronoUnit.HOURS));

        // Order 4: AI-assisted without upsell (Laptop B only)
        createHistoricalOrder("cust_sneha", "Sneha Roy", "sneha.r@example.com",
                List.of(
                        OrderItem.builder().productId(laptopB.getId()).name(laptopB.getName()).price(laptopB.getPrice()).quantity(1).isUpsell(false).build()
                ),
                laptopB.getPrice(), 0.0, laptopB.getPrice(),
                OrderStatus.PAID, true, false, "High-storage developer laptop with 1TB SSD under ₹60k",
                baseTime.plus(36, ChronoUnit.HOURS));

        // Order 5: AI-assisted + Upsell accepted (Keyboard + Mouse)
        createHistoricalOrder("cust_karan", "Karan Mehta", "karan.m@example.com",
                List.of(
                        OrderItem.builder().productId(keyboardA.getId()).name(keyboardA.getName()).price(keyboardA.getPrice()).quantity(1).isUpsell(false).build(),
                        OrderItem.builder().productId(mouse.getId()).name(mouse.getName()).price(mouse.getPrice()).quantity(1).isUpsell(true).build()
                ),
                keyboardA.getPrice(), mouse.getPrice(), keyboardA.getPrice() + mouse.getPrice(),
                OrderStatus.PAID, true, true, "Mechanical keyboard for coding with ergonomic companion mouse",
                baseTime.plus(48, ChronoUnit.HOURS));

        // Order 6: Failed Payment demo record
        createHistoricalOrder("cust_vikram", "Vikram Patel", "vikram.p@example.com",
                List.of(
                        OrderItem.builder().productId(laptopB.getId()).name(laptopB.getName()).price(laptopB.getPrice()).quantity(1).isUpsell(false).build()
                ),
                laptopB.getPrice(), 0.0, laptopB.getPrice(),
                OrderStatus.PAYMENT_FAILED, true, false, "Demo failed payment record",
                baseTime.plus(60, ChronoUnit.HOURS));
    }

    private void createHistoricalOrder(String custId, String name, String email,
                                       List<OrderItem> items, double subtotal, double upsell, double total,
                                       OrderStatus status, boolean aiAssisted, boolean upsellAccepted,
                                       String reason, Instant timestamp) {
        Order order = orderRepository.save(Order.builder()
                .customerId(custId)
                .customerName(name)
                .customerEmail(email)
                .items(items)
                .subtotal(subtotal)
                .upsellAmount(upsell)
                .totalAmount(total)
                .status(status)
                .aiAssisted(aiAssisted)
                .upsellAccepted(upsellAccepted)
                .aiRecommendationReason(reason)
                .approvedAt(timestamp)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build());

        if (status == OrderStatus.PAID) {
            paymentRepository.save(Payment.builder()
                    .orderId(order.getId())
                    .amount(total)
                    .currency("INR")
                    .status(PaymentStatus.SUCCESS)
                    .razorpayOrderId("order_seed_" + order.getId().substring(0, Math.min(6, order.getId().length())))
                    .razorpayPaymentId("pay_seed_" + System.currentTimeMillis() % 1000000)
                    .attemptCount(1)
                    .gatewayMode("RAZORPAY_TEST")
                    .createdAt(timestamp)
                    .updatedAt(timestamp)
                    .build());

            auditService.logEvent(
                    AuditEventType.ORDER_COMPLETED,
                    ActorType.SYSTEM,
                    order.getId(),
                    null,
                    total,
                    "Historical completed order verified",
                    1.0,
                    "SUCCESS",
                    "Customer: " + name
            );
        } else if (status == OrderStatus.PAYMENT_FAILED) {
            paymentRepository.save(Payment.builder()
                    .orderId(order.getId())
                    .amount(total)
                    .currency("INR")
                    .status(PaymentStatus.FAILED)
                    .razorpayOrderId("order_seed_fail_" + order.getId().substring(0, Math.min(6, order.getId().length())))
                    .attemptCount(1)
                    .failureReason("Customer bank declined transaction")
                    .gatewayMode("RAZORPAY_TEST")
                    .createdAt(timestamp)
                    .updatedAt(timestamp)
                    .build());

            auditService.logEvent(
                    AuditEventType.PAYMENT_FAILURE,
                    ActorType.SYSTEM,
                    order.getId(),
                    null,
                    total,
                    "Bank gateway failure: customer card declined",
                    1.0,
                    "FAILED",
                    "Customer: " + name
            );
        }
    }
}
