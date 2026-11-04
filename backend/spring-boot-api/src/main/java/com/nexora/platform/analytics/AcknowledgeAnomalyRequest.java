package com.nexora.platform.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AcknowledgeAnomalyRequest(
        @NotBlank @Pattern(regexp = "ACKNOWLEDGED|RESOLVED") String status,
        String note) {
}
