package com.nexora.platform.sales;

/**
 * {@code revenueGrowthPct} is null when the filtered period has no earlier
 * period to compare against (the AdventureWorksLT sample has a single
 * Order_Date, so period-over-period growth is usually not computable) —
 * the UI must show "not available", never a fabricated 0%.
 */
public record SalesSummaryDto(
        double totalRevenue,
        long totalOrders,
        long totalCustomers,
        double avgOrderValue,
        Double revenueGrowthPct,
        String periodLabel) {
}
