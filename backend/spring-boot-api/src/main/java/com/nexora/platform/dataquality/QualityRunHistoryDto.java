package com.nexora.platform.dataquality;

public record QualityRunHistoryDto(String runId, int totalChecks, int passed, int failed, int skipped) {
}
