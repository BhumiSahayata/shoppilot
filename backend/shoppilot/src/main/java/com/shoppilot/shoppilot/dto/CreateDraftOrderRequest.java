package com.shoppilot.shoppilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDraftOrderRequest {

    private String customerId;
    private String customerName;
    private String customerEmail;

    @NotEmpty(message = "Product IDs cannot be empty")
    private List<String> selectedProductIds;

    private String upsellProductId;
    private boolean upsellAccepted;
    private String aiRecommendationReason;
}
