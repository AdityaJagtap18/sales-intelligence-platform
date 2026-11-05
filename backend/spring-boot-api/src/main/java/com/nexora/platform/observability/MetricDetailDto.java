package com.nexora.platform.observability;

import java.util.List;

public record MetricDetailDto(String name, List<MetricSampleDto> samples) {
}
