package com.nexora.platform.analytics;

public record RevenueTrendPointDto(String label, double value, double expected, boolean anomaly) {
}
