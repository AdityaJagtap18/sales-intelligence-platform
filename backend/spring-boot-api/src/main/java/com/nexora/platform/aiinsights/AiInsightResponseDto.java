package com.nexora.platform.aiinsights;

import java.util.List;

public record AiInsightResponseDto(Long runId, String question, String answer, List<CitationDto> citations,
        String status, String note) {
}
