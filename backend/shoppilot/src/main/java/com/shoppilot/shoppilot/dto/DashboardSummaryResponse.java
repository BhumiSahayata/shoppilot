package com.shoppilot.shoppilot.dto;

import com.shoppilot.shoppilot.model.AuditLog;
import com.shoppilot.shoppilot.model.Order;
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
public class DashboardSummaryResponse {

    private double totalRevenue;
    private double aiAssistedRevenue;
    private double upsellRevenue;
    private double nonAiRevenue;

    private long totalOrders;
    private long aiAssistedOrders;
    private long upsellAcceptedOrders;

    private double averageOrderValue;
    private double upsellAcceptanceRate;

    @Builder.Default
    private List<Order> recentAiOrders = new ArrayList<>();

    @Builder.Default
    private List<AuditLog> recentAuditLogs = new ArrayList<>();
}
