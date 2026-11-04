package com.nexora.platform.databricks;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper over the Databricks SQL Statement Execution API, used to read
 * Gold-layer / quality-report data back into the local app (ADR-005: all
 * analytical data stays in Databricks, Spring Boot only queries it).
 */
@Component
public class DatabricksSqlClient {

    private final RestClient restClient;
    private final DatabricksProperties properties;

    public DatabricksSqlClient(RestClient databricksRestClient, DatabricksProperties properties) {
        this.restClient = databricksRestClient;
        this.properties = properties;
    }

    /** Runs a statement to completion (polling if needed) and returns its rows as string arrays. */
    @SuppressWarnings("unchecked")
    public List<List<String>> executeQuery(String statement) {
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
}
