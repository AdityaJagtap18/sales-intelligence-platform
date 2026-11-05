package com.nexora.platform.alerts;

import java.time.Instant;

public record AlertDto(Long id, String sourceModule, String severity, String title, String message,
        String status, Instant createdAt, Instant updatedAt, Instant resolvedAt, String acknowledgedBy) {

    public static AlertDto from(Alert a) {
        return new AlertDto(a.getId(), a.getSourceModule(), a.getSeverity(), a.getTitle(), a.getMessage(),
                a.getStatus(), a.getCreatedAt(), a.getUpdatedAt(), a.getResolvedAt(), a.getAcknowledgedBy());
    }
}
