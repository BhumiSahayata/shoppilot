package com.shoppilot.shoppilot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String productId;
    private String name;
    private double price;
    private int quantity;
    private boolean isUpsell;
    private String imageUrl;
}
