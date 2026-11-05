package com.nexora.platform.observability;

import java.time.Instant;

public record LogEntryDto(Instant timestamp, String level, String logger, String thread, String message) {
}
