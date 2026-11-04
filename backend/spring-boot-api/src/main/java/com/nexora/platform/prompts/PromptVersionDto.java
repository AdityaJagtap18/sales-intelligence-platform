package com.nexora.platform.prompts;

import java.time.Instant;

public record PromptVersionDto(Long id, int versionNumber, String template, Instant createdAt) {
    public static PromptVersionDto from(PromptVersion version) {
        return new PromptVersionDto(version.getId(), version.getVersionNumber(), version.getTemplate(), version.getCreatedAt());
    }
}
