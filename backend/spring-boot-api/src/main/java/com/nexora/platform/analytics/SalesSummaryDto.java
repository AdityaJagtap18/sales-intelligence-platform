package com.nexora.platform.analytics;

public record SalesSummaryDto(
        double totalRevenue,
        long totalOrders,
        double avgOrderValue,
        long totalCustomers,
        long totalProducts,
        String topCategoryName,
        double topCategoryRevenue,
        double topCategorySharePct) {
}
