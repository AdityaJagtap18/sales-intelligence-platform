package com.nexora.platform.pipeline;

import java.time.Instant;

public record PipelineRunDto(
        Long id,
        Long jobId,
        Long databricksRunId,
        String lifecycleState,
        String resultState,
        String stateMessage,
        String triggeredBy,
        Instant startedAt,
        Instant updatedAt,
        Instant completedAt) {

    public static PipelineRunDto from(PipelineRun run) {
        return new PipelineRunDto(
                run.getId(),
                run.getJobId(),
                run.getDatabricksRunId(),
                run.getLifecycleState(),
                run.getResultState(),
                run.getStateMessage(),
                run.getTriggeredBy(),
                run.getStartedAt(),
                run.getUpdatedAt(),
                run.getCompletedAt());
    }
}
