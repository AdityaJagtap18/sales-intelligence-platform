package com.nexora.platform.prompts;

import jakarta.validation.constraints.NotBlank;

public record CreateVersionRequest(@NotBlank String template) {
}
