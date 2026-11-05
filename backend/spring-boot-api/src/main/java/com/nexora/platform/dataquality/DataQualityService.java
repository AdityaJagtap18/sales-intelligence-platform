package com.nexora.platform.dataquality;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.nexora.platform.databricks.DatabricksSqlClient;

/**
 * Data Quality center (/data-platform/quality). Reads the same
 * workspace.nexora.data_quality_report table {@code DataQualityMetricsPoller}
 * already polls into Prometheus gauges — this is the one real quality-check
 * source this pipeline produces (written by the Phase 4 Silver notebook), not
 * a second quality engine. "Execute checks" re-runs that notebook's job via
 * {@code PipelineDefinitionService}, since the checks run as part of the
 * Bronze-to-Silver transform rather than as a standalone callable step.
 */
@Service
public class DataQualityService {

    private static final String LATEST_RUN_SQL = """
            SELECT run_id, table_name, check_name, status
            FROM workspace.nexora.data_quality_report
            WHERE run_id = (SELECT max(run_id) FROM workspace.nexora.data_quality_report)
            """;

    private static final String HISTORY_SQL = """
            SELECT run_id, status, COUNT(*)
            FROM workspace.nexora.data_quality_report
            GROUP BY run_id, status
            ORDER BY run_id DESC
            """;

    private final DatabricksSqlClient sqlClient;

    public DataQualityService(DatabricksSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public QualitySummaryDto latestSummary() {
        List<List<String>> rows = sqlClient.executeQuery(LATEST_RUN_SQL);
        if (rows.isEmpty()) {
            return new QualitySummaryDto(null, 0, 0, 0, 0, null, List.of(), List.of());
        }
        String runId = rows.get(0).get(0);
        return summarize(runId, rows.stream().map(r -> new QualityCheckRowDto(r.get(1), r.get(2), r.get(3))).toList());
    }

    public List<QualityRunHistoryDto> history() {
        Map<String, int[]> byRun = new LinkedHashMap<>(); // [total, passed, failed, skipped]
        for (List<String> row : sqlClient.executeQuery(HISTORY_SQL)) {
            String runId = row.get(0);
            String status = row.get(1);
            int count = Integer.parseInt(row.get(2));
            int[] counts = byRun.computeIfAbsent(runId, k -> new int[4]);
            counts[0] += count;
            if ("PASS".equals(status)) counts[1] += count;
            else if ("FAIL".equals(status)) counts[2] += count;
            else counts[3] += count;
        }
        List<QualityRunHistoryDto> result = new ArrayList<>();
        byRun.forEach((runId, c) -> result.add(new QualityRunHistoryDto(runId, c[0], c[1], c[2], c[3])));
        return result;
    }

    public QualitySummaryDto runDetail(String runId) {
        String escaped = runId.replace("'", "''");
        List<List<String>> rows = sqlClient.executeQuery(
                "SELECT table_name, check_name, status FROM workspace.nexora.data_quality_report WHERE run_id = '" + escaped + "'");
        return summarize(runId, rows.stream().map(r -> new QualityCheckRowDto(r.get(0), r.get(1), r.get(2))).toList());
    }

    private QualitySummaryDto summarize(String runId, List<QualityCheckRowDto> checks) {
        int passed = (int) checks.stream().filter(c -> "PASS".equals(c.status())).count();
        int failed = (int) checks.stream().filter(c -> "FAIL".equals(c.status())).count();
        int skipped = (int) checks.stream().filter(c -> "SKIPPED".equals(c.status())).count();
        Double score = checks.isEmpty() ? null : round2((passed * 100.0) / checks.size());

        Map<String, List<QualityCheckRowDto>> byTable = new LinkedHashMap<>();
        for (QualityCheckRowDto c : checks) {
            byTable.computeIfAbsent(c.tableName(), k -> new ArrayList<>()).add(c);
        }
        List<QualitySummaryDto.DatasetStatusDto> datasets = new ArrayList<>();
        byTable.forEach((table, rows) -> {
            int p = (int) rows.stream().filter(c -> "PASS".equals(c.status())).count();
            int f = (int) rows.stream().filter(c -> "FAIL".equals(c.status())).count();
            int s = (int) rows.stream().filter(c -> "SKIPPED".equals(c.status())).count();
            String overall = f > 0 ? "FAIL" : s > 0 ? "SKIPPED" : "PASS";
            datasets.add(new QualitySummaryDto.DatasetStatusDto(table, rows.size(), p, f, s, overall));
        });

        return new QualitySummaryDto(runId, checks.size(), passed, failed, skipped, score, datasets, checks);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
