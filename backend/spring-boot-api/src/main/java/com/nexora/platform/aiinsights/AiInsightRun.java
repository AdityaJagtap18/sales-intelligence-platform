package com.nexora.platform.aiinsights;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_insight_run")
public class AiInsightRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question", nullable = false)
    private String question;

    @Column(name = "answer")
    private String answer;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "citation_count", nullable = false)
    private int citationCount;

    @Column(name = "asked_at", nullable = false)
    private Instant askedAt;

    protected AiInsightRun() {
    }

    public AiInsightRun(String question, String answer, String status, int citationCount, Instant askedAt) {
        this.question = question;
        this.answer = answer;
        this.status = status;
        this.citationCount = citationCount;
        this.askedAt = askedAt;
    }

    public Long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getStatus() {
        return status;
    }

    public int getCitationCount() {
        return citationCount;
    }

    public Instant getAskedAt() {
        return askedAt;
    }
}
