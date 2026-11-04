package com.nexora.platform.analytics;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/sales-summary")
    public SalesSummaryDto salesSummary() {
        return analyticsService.salesSummary();
    }

    @GetMapping("/revenue-trend")
    public List<RevenueTrendPointDto> revenueTrend() {
        return analyticsService.revenueTrend();
    }

    @GetMapping("/revenue-by-category")
    public List<LabelValueDto> revenueByCategory() {
        return analyticsService.revenueByCategory();
    }

    @GetMapping("/revenue-by-top-customers")
    public List<LabelValueDto> revenueByTopCustomers() {
        return analyticsService.revenueByTopCustomers();
    }

    // Anomaly listing moved to AnomalyDetectionController (/analytics/anomalies),
    // which adds filtering and acknowledge/resolve state on top of the same
    // AnalyticsService#anomalies() computation used here and by Executive Overview.

    @GetMapping("/orders")
    public List<OrderRowDto> orders() {
        return analyticsService.orders();
    }

    @GetMapping("/products")
    public List<ProductPerformanceDto> products() {
        return analyticsService.productPerformance();
    }
}
