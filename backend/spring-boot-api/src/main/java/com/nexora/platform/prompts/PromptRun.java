package com.nexora.platform.prompts;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prompt_run")
public class PromptRun {

    public static final String RATING_UNRATED = "UNRATED";
    public static final String RATING_PASS = "PASS";
    public static final String RATING_FAIL = "FAIL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt_version_id", nullable = false)
    private Long promptVersionId;

    @Column(name = "rendered_prompt", nullable = false)
    private String renderedPrompt;

    private String output;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String rating;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PromptRun() {
    }

    public PromptRun(Long promptVersionId, String renderedPrompt, String model, Instant createdAt) {
        this.promptVersionId = promptVersionId;
        this.renderedPrompt = renderedPrompt;
        this.model = model;
        this.rating = RATING_UNRATED;
        this.createdAt = createdAt;
    }

    public void markSucceeded(String output, long latencyMs) {
        this.output = output;
        this.latencyMs = latencyMs;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public String getRenderedPrompt() {
        return renderedPrompt;
    }

    public String getOutput() {
        return output;
    }

    public String getModel() {
        return model;
    }

    public String getRating() {
        return rating;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
