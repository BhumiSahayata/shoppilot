package com.shoppilot.shoppilot.dto;

import com.shoppilot.shoppilot.model.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiShoppingResponse {

    private String userMessage;
    private String aiMessage;
    private String engineUsed; // "LIVE_AI" or "SAFE_FALLBACK"
    private IntentDto intent;

    @Builder.Default
    private List<ProductRecommendationDto> recommendations = new ArrayList<>();

    private ProductRecommendationDto upsell;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentDto {
        private String category;
        private String useCase;
        private Double maxBudget;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRecommendationDto {
        private Product product;
        private String reason;
        private double confidence;
        private boolean isUpsell;
    }
}
