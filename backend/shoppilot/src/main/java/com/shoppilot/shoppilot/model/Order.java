package com.shoppilot.shoppilot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;
    private String customerId;
    private String customerName;
    private String customerEmail;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private double subtotal;
    private double upsellAmount;
    private double totalAmount;

    @Builder.Default
    private OrderStatus status = OrderStatus.DISCOVERED;

    private boolean aiAssisted;
    private boolean upsellAccepted;
    private String aiRecommendationReason;

    private Instant approvedAt;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
