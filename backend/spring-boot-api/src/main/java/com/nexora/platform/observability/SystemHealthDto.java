package com.nexora.platform.observability;

import java.time.Instant;
import java.util.List;

public record SystemHealthDto(String overallStatus, Instant checkedAt, List<ComponentHealthDto> components) {
}
