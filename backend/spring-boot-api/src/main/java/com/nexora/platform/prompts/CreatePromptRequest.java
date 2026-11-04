package com.nexora.platform.prompts;

import jakarta.validation.constraints.NotBlank;

public record CreatePromptRequest(@NotBlank String name, String description, @NotBlank String template) {
}
