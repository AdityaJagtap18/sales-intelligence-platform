package com.nexora.platform.alerts;

import jakarta.validation.constraints.NotBlank;

public record AcknowledgeAlertRequest(@NotBlank String acknowledgedBy) {
}
