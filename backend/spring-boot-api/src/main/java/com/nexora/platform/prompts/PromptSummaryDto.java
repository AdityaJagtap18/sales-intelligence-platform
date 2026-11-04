package com.nexora.platform.prompts;

import java.time.Instant;

public record PromptSummaryDto(Long id, String name, String description, int latestVersionNumber, Instant createdAt) {
}
