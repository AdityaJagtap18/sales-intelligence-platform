package com.nexora.platform.prompts;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prompt_version")
public class PromptVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt_id", nullable = false)
    private Long promptId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private String template;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PromptVersion() {
    }

    public PromptVersion(Long promptId, int versionNumber, String template, Instant createdAt) {
        this.promptId = promptId;
        this.versionNumber = versionNumber;
        this.template = template;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getPromptId() {
        return promptId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getTemplate() {
        return template;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
