package com.nexora.platform.observability;

import java.time.Instant;

public record TraceDto(Instant timestamp, String method, String uri, Integer status, Long durationMs) {
}
