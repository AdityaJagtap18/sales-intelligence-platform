package com.nexora.platform.reports;

import java.util.List;
import java.util.Map;

public record ReportResultDto(Long executionId, String reportKey, String reportName, String status,
        List<String> columns, List<Map<String, Object>> rows, String generatedAt, String errorMessage) {
}
