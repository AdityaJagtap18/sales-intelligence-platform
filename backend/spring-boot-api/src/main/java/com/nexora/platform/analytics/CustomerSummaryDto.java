package com.nexora.platform.analytics;

public record CustomerSummaryDto(
        long totalCustomers,
        long customersWithOrders,
        long returningCustomers,
        long inactiveCustomers,
        double avgPurchaseValue,
        double avgOrdersPerCustomer) {
}
