package com.nexora.platform.observability;

public record ComponentHealthDto(String name, String category, String status, Long latencyMs, String message) {
}
