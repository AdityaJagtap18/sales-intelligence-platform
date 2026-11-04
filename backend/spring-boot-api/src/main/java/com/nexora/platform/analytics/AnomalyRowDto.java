package com.nexora.platform.analytics;

public record AnomalyRowDto(String metric, String date, String impact, String direction, int confidence) {
}
