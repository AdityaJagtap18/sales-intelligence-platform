package com.nexora.platform.observability;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * Metrics explorer (/observability/metrics) over this app's real Micrometer
 * registry — the same registry {@code management.prometheus.metrics.export}
 * already publishes to Prometheus, and the same one
 * {@link DataQualityMetricsPoller} writes nexora_data_quality_check into.
 * No separate metrics store is introduced.
 */
@RestController
@RequestMapping("/observability/metrics")
public class MetricsController {

    private final MeterRegistry registry;

    public MetricsController(MeterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<MetricSummaryDto> list() {
        Map<String, List<Meter>> byName = registry.getMeters().stream()
                .collect(Collectors.groupingBy(m -> m.getId().getName()));
        return byName.entrySet().stream()
                .map(e -> new MetricSummaryDto(e.getKey(), e.getValue().get(0).getId().getType().toString(), e.getValue().size()))
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
    }

    @GetMapping("/{name}")
    public MetricDetailDto detail(@PathVariable String name) {
        List<MetricSampleDto> samples = registry.getMeters().stream()
                .filter(m -> m.getId().getName().equals(name))
                .flatMap(m -> {
                    Map<String, String> tags = m.getId().getTags().stream()
                            .collect(Collectors.toMap(Tag::getKey, Tag::getValue));
                    return java.util.stream.StreamSupport.stream(m.measure().spliterator(), false)
                            .map(measurement -> new MetricSampleDto(tags, measurement.getStatistic().toString(), measurement.getValue()));
                })
                .toList();
        return new MetricDetailDto(name, samples);
    }
}
