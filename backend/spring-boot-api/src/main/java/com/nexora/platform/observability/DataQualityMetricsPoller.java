package com.nexora.platform.observability;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nexora.platform.databricks.DatabricksSqlClient;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * Polls workspace.nexora.data_quality_report (written by the Phase 4 Silver
 * notebook) via the SQL Warehouse and republishes the latest run's checks as
 * Prometheus gauges, so Grafana can show pipeline data-quality alongside
 * infra metrics without a second datasource. 1 = PASS, 0 = FAIL, 0.5 = SKIPPED.
 */
@Component
public class DataQualityMetricsPoller {

    private static final Logger log = LoggerFactory.getLogger(DataQualityMetricsPoller.class);
    private static final String LATEST_RUN_CHECKS_SQL = """
            SELECT table_name, check_name, status
            FROM workspace.nexora.data_quality_report
            WHERE run_id = (SELECT max(run_id) FROM workspace.nexora.data_quality_report)
            """;

    private final DatabricksSqlClient sqlClient;
    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicReference<Double>> gaugeValues = new ConcurrentHashMap<>();

    public DataQualityMetricsPoller(DatabricksSqlClient sqlClient, MeterRegistry meterRegistry) {
        this.sqlClient = sqlClient;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${databricks.quality-poll-interval-ms:60000}", initialDelay = 10000)
    public void poll() {
        try {
            List<List<String>> rows = sqlClient.executeQuery(LATEST_RUN_CHECKS_SQL);
            for (List<String> row : rows) {
                String tableName = row.get(0);
                String checkName = row.get(1);
                String status = row.get(2);
                setGauge(tableName, checkName, toValue(status));
            }
            log.debug("Updated {} data quality gauges", rows.size());
        } catch (Exception e) {
            log.warn("Failed to poll data quality report from Databricks: {}", e.getMessage());
        }
    }

    private double toValue(String status) {
        return switch (status) {
            case "PASS" -> 1.0;
            case "SKIPPED" -> 0.5;
            default -> 0.0;
        };
    }

    private void setGauge(String tableName, String checkName, double value) {
        String key = tableName + "::" + checkName;
        AtomicReference<Double> ref = gaugeValues.computeIfAbsent(key, k -> {
            AtomicReference<Double> newRef = new AtomicReference<>(value);
            Gauge.builder("nexora_data_quality_check", newRef, AtomicReference::get)
                    .description("Latest Silver-layer data quality check result (1=PASS, 0.5=SKIPPED, 0=FAIL)")
                    .tags(Tags.of("table_name", tableName, "check_name", checkName))
                    .register(meterRegistry);
            return newRef;
        });
        ref.set(value);
    }
}
