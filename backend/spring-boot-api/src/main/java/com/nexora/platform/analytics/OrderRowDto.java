package com.nexora.platform.analytics;

public record OrderRowDto(String orderNumber, String orderDate, String customerName, double totalDue, long itemCount) {
}
