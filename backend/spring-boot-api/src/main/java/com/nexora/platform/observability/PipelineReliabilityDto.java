package com.nexora.platform.observability;

public record PipelineReliabilityDto(long definitionId, String name, int totalRuns, int successfulRuns,
        int failedRuns, Double successRatePct, String lastRunStatus) {
}
