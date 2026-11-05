package com.nexora.platform.analytics;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/sales-summary")
    public SalesSummaryDto salesSummary(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return analyticsService.salesSummary(parseDate(from), parseDate(to));
    }

    @GetMapping("/revenue-trend")
    public List<RevenueTrendPointDto> revenueTrend() {
        return analyticsService.revenueTrend();
    }

    @GetMapping("/revenue-by-category")
    public List<LabelValueDto> revenueByCategory(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return analyticsService.revenueByCategory(parseDate(from), parseDate(to));
    }

    @GetMapping("/revenue-by-top-customers")
    public List<LabelValueDto> revenueByTopCustomers(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return analyticsService.revenueByTopCustomers(parseDate(from), parseDate(to));
    }

    // Anomaly listing moved to AnomalyDetectionController (/analytics/anomalies),
    // which adds filtering and acknowledge/resolve state on top of the same
    // AnalyticsService#anomalies() computation used here and by Executive Overview.

    @GetMapping("/orders")
    public List<OrderRowDto> orders(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return analyticsService.orders(parseDate(from), parseDate(to));
    }

    @GetMapping("/products")
    public List<ProductPerformanceDto> products(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return analyticsService.productPerformance(parseDate(from), parseDate(to));
    }

    private static LocalDate parseDate(String value) {
        return (value == null || value.isBlank()) ? null : LocalDate.parse(value.trim());
    }
}
