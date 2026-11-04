package com.nexora.platform.databricks;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper over the Databricks Jobs API (run-now / runs-get). Spring Boot
 * is the only component that holds the Databricks host/token (ADR-005); this
 * client never leaks either to a caller.
 */
@Component
public class DatabricksJobsClient {

    private final RestClient restClient;

    public DatabricksJobsClient(RestClient databricksRestClient) {
        this.restClient = databricksRestClient;
    }

    public long runNow(long jobId) {
        Map<String, Object> response = restClient.post()
                .uri("/api/2.2/jobs/run-now")
                .body(Map.of("job_id", jobId))
                .retrieve()
                .body(Map.class);
        return ((Number) response.get("run_id")).longValue();
    }

    public RunStatus getRunStatus(long runId) {
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/2.2/jobs/runs/get").queryParam("run_id", runId).build())
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) response.get("state");
        String lifecycleState = state == null ? "UNKNOWN" : (String) state.get("life_cycle_state");
        String resultState = state == null ? null : (String) state.get("result_state");
        String stateMessage = state == null ? null : (String) state.get("state_message");
        return new RunStatus(lifecycleState, resultState, stateMessage);
    }

    public record RunStatus(String lifecycleState, String resultState, String stateMessage) {
        public boolean isTerminal() {
            return "TERMINATED".equals(lifecycleState)
                    || "SKIPPED".equals(lifecycleState)
                    || "INTERNAL_ERROR".equals(lifecycleState);
        }
    }
}
