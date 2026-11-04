package com.nexora.platform.reports;

public record ReportExecutionDto(Long id, String reportKey, String reportName, String status, Integer rowCount,
        String errorMessage, String generatedAt) {
}
