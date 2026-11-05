package com.nexora.platform.observability;

import java.util.Map;

public record MetricSampleDto(Map<String, String> tags, String statistic, double value) {
}
