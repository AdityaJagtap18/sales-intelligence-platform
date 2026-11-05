package com.nexora.platform.databricks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper over the Databricks SQL Statement Execution API, used to read
 * Gold-layer / quality-report data back into the local app (ADR-005: all
 * analytical data stays in Databricks, Spring Boot only queries it).
 *
 * <p>Every page that reads Gold-layer data (Sales Dashboard, Executive
 * Overview, Customer Analytics, Reports, Forecasting, Anomaly Detection,
 * Data Quality, Catalog/Lineage) ultimately calls {@link #executeQuery}, and
 * each call is a real network round trip to the SQL Warehouse (hundreds of
 * ms, more on a cold warehouse). A short in-memory cache keyed by the exact
 * statement text turns repeat reads of the same query -- which is most of
 * them, since the sample dataset is static -- into a map lookup. There is no
 * caching library cached in this offline Maven build, so this is a plain
 * {@code ConcurrentHashMap} rather than Spring Cache + Caffeine.
 */
@Component
public class DatabricksSqlClient {

    private final RestClient restClient;
    private final DatabricksProperties properties;
    private final long cacheTtlMillis;
    private final Map<String, CacheEntry> queryCache = new ConcurrentHashMap<>();

    public DatabricksSqlClient(
            RestClient databricksRestClient,
            DatabricksProperties properties,
            @Value("${databricks.query-cache-ttl-ms:30000}") long cacheTtlMillis) {
        this.restClient = databricksRestClient;
        this.properties = properties;
        this.cacheTtlMillis = cacheTtlMillis;
    }

    /**
     * Runs a statement to completion (polling if needed) and returns its rows
     * as string arrays, serving a cached result when one is fresh enough.
     * "SELECT 1" is never cached -- System Health and Data Sources' "test
     * connection" both use it specifically to probe whether the warehouse is
     * reachable right now, and a cached answer would hide a real outage.
     */
    public List<List<String>> executeQuery(String statement) {
        if (cacheTtlMillis <= 0 || isConnectivityProbe(statement)) {
            return runQuery(statement);
        }
        long now = System.currentTimeMillis();
        CacheEntry cached = queryCache.get(statement);
        if (cached != null && cached.expiresAt() > now) {
            return cached.rows();
        }
        List<List<String>> rows = runQuery(statement);
        queryCache.put(statement, new CacheEntry(rows, now + cacheTtlMillis));
        return rows;
    }

    private boolean isConnectivityProbe(String statement) {
        return "SELECT 1".equalsIgnoreCase(statement.trim());
    }

    /** Drops every cached result so the next read of each statement hits Databricks again. */
    public void evictAll() {
        queryCache.clear();
    }

    /** Bounds cache growth from dynamic filter-driven statements (Sales Dashboard, Reports, Customer Analytics). */
    @Scheduled(fixedRate = 300_000)
    void sweepExpiredEntries() {
        long now = System.currentTimeMillis();
        queryCache.values().removeIf(entry -> entry.expiresAt() <= now);
    }

    @SuppressWarnings("unchecked")
    private List<List<String>> runQuery(String statement) {
        Map<String, Object> response = restClient.post()
                .uri("/api/2.0/sql/statements")
                .body(Map.of(
                        "warehouse_id", properties.sqlWarehouseId(),
                        "statement", statement,
                        "wait_timeout", "30s"))
                .retrieve()
                .body(Map.class);

        String statementId = (String) response.get("statement_id");
        String state = statusOf(response);

        while ("PENDING".equals(state) || "RUNNING".equals(state)) {
            sleep();
            response = restClient.get()
                    .uri("/api/2.0/sql/statements/{id}", statementId)
                    .retrieve()
                    .body(Map.class);
            state = statusOf(response);
        }

        if (!"SUCCEEDED".equals(state)) {
            throw new IllegalStateException("Databricks SQL statement did not succeed: " + state);
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        if (result == null || result.get("data_array") == null) {
            return List.of();
        }
        List<List<Object>> rows = (List<List<Object>>) result.get("data_array");
        return rows.stream()
                .map(row -> row.stream().map(cell -> cell == null ? null : cell.toString()).toList())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private String statusOf(Map<String, Object> response) {
        Map<String, Object> status = (Map<String, Object>) response.get("status");
        return status == null ? null : (String) status.get("state");
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Databricks SQL statement", e);
        }
    }

    private record CacheEntry(List<List<String>> rows, long expiresAt) {}
}
