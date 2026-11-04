package com.nexora.platform.sales;

public record SalesOrderDetailDto(
        String orderNumber,
        String orderDate,
        String customer,
        String territory,
        String status,
        double totalDue,
        long lineItemCount) {
}
