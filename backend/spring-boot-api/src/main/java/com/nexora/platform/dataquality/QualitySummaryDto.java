package com.nexora.platform.dataquality;

import java.util.List;

public record QualitySummaryDto(
        String runId,
        int totalChecks,
        int passed,
        int failed,
        int skipped,
        Double qualityScorePct,
        List<DatasetStatusDto> byDataset,
        List<QualityCheckRowDto> checks) {

    public record DatasetStatusDto(String tableName, int total, int passed, int failed, int skipped, String overallStatus) {
    }
}
