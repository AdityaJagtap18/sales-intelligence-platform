package com.nexora.platform.pipeline;

public record PipelineDefinitionDto(
        Long id,
        String name,
        String description,
        String pipelineType,
        String sourceName,
        String destinationName,
        Long dataSourceId,
        String lastRunStatus,
        String lastRunAt,
        Long lastRunDurationMs) {
}
