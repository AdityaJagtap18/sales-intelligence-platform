package com.nexora.platform.analytics;

public record CustomerRowDto(
        long customerId,
        String companyName,
        String territory,
        long orderCount,
        double lifetimeRevenue,
        double avgOrderValue,
        String segment) {
}
