package com.nexora.platform.prompts;

import java.time.Instant;
import java.util.List;

public record PromptDetailDto(Long id, String name, String description, Instant createdAt, List<PromptVersionDto> versions) {
}
