package com.nexora.platform.analytics;

import java.util.List;

public record CustomerDetailDto(
        long customerId,
        String companyName,
        String emailAddress,
        String territory,
        long orderCount,
        double lifetimeRevenue,
        double avgOrderValue,
        String segment,
        List<OrderRowDto> orders) {
}
