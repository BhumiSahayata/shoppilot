package com.shoppilot.shoppilot.service;

import com.shoppilot.shoppilot.audit.AuditService;
import com.shoppilot.shoppilot.dto.DashboardSummaryResponse;
import com.shoppilot.shoppilot.model.Order;
import com.shoppilot.shoppilot.model.OrderStatus;
import com.shoppilot.shoppilot.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final AuditService auditService;

    public DashboardSummaryResponse getDashboardSummary() {
        List<Order> paidOrders = orderRepository.findByStatus(OrderStatus.PAID);

        double totalRevenue = 0.0;
        double aiAssistedRevenue = 0.0;
        double upsellRevenue = 0.0;
        long aiOrdersCount = 0;
        long upsellOrdersCount = 0;

        for (Order order : paidOrders) {
            totalRevenue += order.getTotalAmount();
            if (order.isAiAssisted()) {
                aiAssistedRevenue += order.getTotalAmount();
                aiOrdersCount++;
            }
            if (order.isUpsellAccepted()) {
                upsellRevenue += order.getUpsellAmount();
                upsellOrdersCount++;
            }
        }

        double nonAiRevenue = Math.max(0, totalRevenue - aiAssistedRevenue);
        double aov = paidOrders.isEmpty() ? 0.0 : Math.round((totalRevenue / paidOrders.size()) * 100.0) / 100.0;
        double upsellAcceptanceRate = aiOrdersCount == 0 ? 0.0 :
                Math.round(((double) upsellOrdersCount / aiOrdersCount) * 100.0 * 10.0) / 10.0;

        List<Order> recentAiOrders = orderRepository.findByAiAssistedTrue();
        if (recentAiOrders.size() > 5) {
            recentAiOrders = recentAiOrders.subList(0, 5);
        }

        return DashboardSummaryResponse.builder()
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .aiAssistedRevenue(Math.round(aiAssistedRevenue * 100.0) / 100.0)
                .upsellRevenue(Math.round(upsellRevenue * 100.0) / 100.0)
                .nonAiRevenue(Math.round(nonAiRevenue * 100.0) / 100.0)
                .totalOrders(paidOrders.size())
                .aiAssistedOrders(aiOrdersCount)
                .upsellAcceptedOrders(upsellOrdersCount)
                .averageOrderValue(aov)
                .upsellAcceptanceRate(upsellAcceptanceRate)
                .recentAiOrders(recentAiOrders)
                .recentAuditLogs(auditService.getRecentLogs().stream().limit(10).toList())
                .build();
    }
}
