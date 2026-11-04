package com.nexora.platform.pipeline;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pipeline_run")
public class PipelineRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "databricks_run_id", nullable = false, unique = true)
    private Long databricksRunId;

    @Column(name = "lifecycle_state", nullable = false)
    private String lifecycleState;

    @Column(name = "result_state")
    private String resultState;

    @Column(name = "state_message")
    private String stateMessage;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected PipelineRun() {
    }

    public PipelineRun(Long jobId, Long databricksRunId, String lifecycleState, String triggeredBy, Instant startedAt) {
        this.jobId = jobId;
        this.databricksRunId = databricksRunId;
        this.lifecycleState = lifecycleState;
        this.triggeredBy = triggeredBy;
        this.startedAt = startedAt;
        this.updatedAt = startedAt;
    }

    public void applyStatus(String lifecycleState, String resultState, String stateMessage, Instant now) {
        this.lifecycleState = lifecycleState;
        this.resultState = resultState;
        this.stateMessage = stateMessage;
        this.updatedAt = now;
        if (isTerminal(lifecycleState) && this.completedAt == null) {
            this.completedAt = now;
        }
    }

    private static boolean isTerminal(String lifecycleState) {
        return "TERMINATED".equals(lifecycleState) || "SKIPPED".equals(lifecycleState)
                || "INTERNAL_ERROR".equals(lifecycleState);
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getDatabricksRunId() {
        return databricksRunId;
    }

    public String getLifecycleState() {
        return lifecycleState;
    }

    public String getResultState() {
        return resultState;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
