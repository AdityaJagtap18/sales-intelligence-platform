package com.nexora.platform.modelconfig;

import jakarta.validation.constraints.NotBlank;

public record SetChatModelRequest(@NotBlank String model) {
}
