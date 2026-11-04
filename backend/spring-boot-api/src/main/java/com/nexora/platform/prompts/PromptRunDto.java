package com.nexora.platform.prompts;

import java.time.Instant;

public record PromptRunDto(
        Long id,
        Long promptVersionId,
        int versionNumber,
        String renderedPrompt,
        String output,
        String model,
        String rating,
        Long latencyMs,
        String errorMessage,
        Instant createdAt) {
}
