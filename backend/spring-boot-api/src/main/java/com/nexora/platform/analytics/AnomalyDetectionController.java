package com.nexora.platform.analytics;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

/**
 * Dedicated Anomaly Detection page (/analytics/anomaly-detection). Reuses
 * {@link AnalyticsService#anomalies()} — the exact same z-score computation
 * Executive Overview uses — and layers filtering plus persistent
 * acknowledge/resolve state on top, rather than a second detection engine.
 */
@RestController
@RequestMapping("/analytics/anomalies")
public class AnomalyDetectionController {

    private final AnalyticsService analyticsService;
    private final AnomalyAcknowledgementRepository ackRepository;

    public AnomalyDetectionController(AnalyticsService analyticsService, AnomalyAcknowledgementRepository ackRepository) {
        this.analyticsService = analyticsService;
        this.ackRepository = ackRepository;
    }

    @GetMapping
    public List<AnomalyRowDto> list(@RequestParam(required = false) String severity,
            @RequestParam(required = false) String metric) {
        List<AnomalyRowDto> rows = analyticsService.anomalies().stream()
                .map(a -> {
                    var ack = ackRepository.findByAnomalyKey(a.key());
                    return ack.isPresent() ? a.withAcknowledgement(ack.get().getStatus(), ack.get().getNote()) : a;
                })
                .toList();
        if (severity != null && !severity.isBlank()) {
            rows = rows.stream().filter(a -> a.impact().equalsIgnoreCase(severity)).toList();
        }
        if (metric != null && !metric.isBlank()) {
            rows = rows.stream().filter(a -> a.metric().equalsIgnoreCase(metric)).toList();
        }
        return rows;
    }

    @PostMapping("/{key}/acknowledge")
    public AnomalyAcknowledgement acknowledge(@PathVariable String key, @Valid @RequestBody AcknowledgeAnomalyRequest request) {
        boolean exists = analyticsService.anomalies().stream().anyMatch(a -> a.key().equals(key));
        if (!exists) {
            throw new NoSuchElementException("No anomaly with key " + key);
        }
        AnomalyAcknowledgement ack = ackRepository.findByAnomalyKey(key).orElse(
                new AnomalyAcknowledgement(key, request.status(), request.note(), "angular-ui", Instant.now()));
        ack.setStatus(request.status());
        ack.setNote(request.note());
        ack.setAcknowledgedAt(Instant.now());
        return ackRepository.save(ack);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
