package com.nexora.platform.alerts;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nexora.platform.analytics.AnalyticsService;
import com.nexora.platform.analytics.AnomalyRowDto;
import com.nexora.platform.dataquality.DataQualityService;
import com.nexora.platform.dataquality.QualitySummaryDto;
import com.nexora.platform.observability.ComponentHealthDto;
import com.nexora.platform.observability.SystemHealthService;
import com.nexora.platform.pipeline.PipelineRunDto;
import com.nexora.platform.pipeline.PipelineRunService;

/**
 * Evaluates the real conditions the spec's cross-module workflows call for
 * (Data Quality -> Alerts, Anomaly Detection -> Alerts, pipeline failures,
 * system health) and keeps the {@code alert} table in sync with them —
 * reusing {@link AnalyticsService#anomalies()}, {@link DataQualityService},
 * {@link PipelineRunService} and {@link SystemHealthService} rather than a
 * second detection engine. An alert is opened the first time a condition is
 * observed true, left alone while it stays true (no duplicate spam), and
 * auto-resolved the first cycle it is no longer true.
 */
@Service
public class AlertEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationService.class);
    private static final int ANOMALY_CONFIDENCE_THRESHOLD = 70;

    private final AlertRepository repository;
    private final PipelineRunService pipelineRunService;
    private final DataQualityService dataQualityService;
    private final AnalyticsService analyticsService;
    private final SystemHealthService systemHealthService;

    public AlertEvaluationService(AlertRepository repository, PipelineRunService pipelineRunService,
            DataQualityService dataQualityService, AnalyticsService analyticsService,
            SystemHealthService systemHealthService) {
        this.repository = repository;
        this.pipelineRunService = pipelineRunService;
        this.dataQualityService = dataQualityService;
        this.analyticsService = analyticsService;
        this.systemHealthService = systemHealthService;
    }

    @Scheduled(fixedDelayString = "${alerts.evaluation-interval-ms:60000}", initialDelay = 15000)
    public void evaluate() {
        Instant now = Instant.now();
        Set<String> activeKeys = new HashSet<>();
        try {
            evaluatePipelineFailures(now, activeKeys);
        } catch (Exception e) {
            log.warn("Alert evaluation: pipeline run check failed: {}", e.getMessage());
        }
        try {
            evaluateDataQuality(now, activeKeys);
        } catch (Exception e) {
            log.warn("Alert evaluation: data quality check failed: {}", e.getMessage());
        }
        try {
            evaluateAnomalies(now, activeKeys);
        } catch (Exception e) {
            log.warn("Alert evaluation: anomaly check failed: {}", e.getMessage());
        }
        try {
            evaluateSystemHealth(now, activeKeys);
        } catch (Exception e) {
            log.warn("Alert evaluation: system health check failed: {}", e.getMessage());
        }
        autoResolveStale(now, activeKeys);
    }

    private void evaluatePipelineFailures(Instant now, Set<String> activeKeys) {
        List<PipelineRunDto> runs = pipelineRunService.listRuns();
        for (PipelineRunDto run : runs) {
            if (run.resultState() == null || "SUCCESS".equals(run.resultState())) {
                continue;
            }
            String key = "pipeline-run-" + run.id();
            activeKeys.add(key);
            upsert("PIPELINE", "CRITICAL", key,
                    "Pipeline run " + run.databricksRunId() + " " + run.resultState(),
                    "Databricks run " + run.databricksRunId() + " finished as " + run.resultState()
                            + (run.stateMessage() != null ? ": " + run.stateMessage() : "") + ".",
                    now);
        }
    }

    private void evaluateDataQuality(Instant now, Set<String> activeKeys) {
        QualitySummaryDto summary = dataQualityService.latestSummary();
        if (summary.runId() == null || summary.failed() <= 0) {
            return;
        }
        String key = "data-quality-run-" + summary.runId();
        activeKeys.add(key);
        upsert("DATA_QUALITY", "WARNING", key,
                summary.failed() + " data quality check(s) failing",
                "Run " + summary.runId() + " has " + summary.failed() + " failed check(s) out of "
                        + summary.totalChecks() + " (quality score "
                        + (summary.qualityScorePct() == null ? "n/a" : summary.qualityScorePct() + "%") + ").",
                now);
    }

    private void evaluateAnomalies(Instant now, Set<String> activeKeys) {
        List<AnomalyRowDto> anomalies = analyticsService.anomalies();
        for (AnomalyRowDto a : anomalies) {
            if (a.confidence() < ANOMALY_CONFIDENCE_THRESHOLD) {
                continue;
            }
            String key = "anomaly-" + a.key();
            activeKeys.add(key);
            upsert("ANOMALY_DETECTION", "WARNING", key,
                    a.metric() + " anomaly on " + a.date(),
                    a.direction() + " " + a.impact() + " impact on " + a.date() + " (observed "
                            + a.observedValue() + ", expected " + a.expectedValue() + ", confidence "
                            + a.confidence() + "%).",
                    now);
        }
    }

    private void evaluateSystemHealth(Instant now, Set<String> activeKeys) {
        for (ComponentHealthDto component : systemHealthService.check().components()) {
            if (!"DOWN".equals(component.status())) {
                continue;
            }
            String key = "health-" + component.name();
            activeKeys.add(key);
            upsert("SYSTEM_HEALTH", "CRITICAL", key,
                    component.name() + " is unreachable",
                    component.message() == null ? component.name() + " failed its connectivity check."
                            : component.message(),
                    now);
        }
    }

    private void upsert(String sourceModule, String severity, String dedupeKey, String title, String message,
            Instant now) {
        repository.findByDedupeKey(dedupeKey).ifPresentOrElse(
                existing -> {
                    existing.refresh(title, message, now);
                    repository.save(existing);
                },
                () -> repository.save(new Alert(sourceModule, severity, title, message, dedupeKey, now)));
    }

    private void autoResolveStale(Instant now, Set<String> activeKeys) {
        for (Alert alert : repository.findAllByStatusNot("RESOLVED")) {
            if (!activeKeys.contains(alert.getDedupeKey())) {
                alert.resolve(now);
                repository.save(alert);
            }
        }
    }
}
