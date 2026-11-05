package com.nexora.platform.aiinsights;

import java.time.Instant;

public record AiInsightHistoryDto(Long id, String question, String status, int citationCount, Instant askedAt) {

    public static AiInsightHistoryDto from(AiInsightRun run) {
        return new AiInsightHistoryDto(run.getId(), run.getQuestion(), run.getStatus(), run.getCitationCount(),
                run.getAskedAt());
    }
}
