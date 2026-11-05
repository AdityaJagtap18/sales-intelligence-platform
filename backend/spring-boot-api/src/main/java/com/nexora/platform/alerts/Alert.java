package com.nexora.platform.alerts;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "alert")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_module", nullable = false)
    private String sourceModule;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "dedupe_key", nullable = false, unique = true)
    private String dedupeKey;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    protected Alert() {
    }

    public Alert(String sourceModule, String severity, String title, String message, String dedupeKey, Instant now) {
        this.sourceModule = sourceModule;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.dedupeKey = dedupeKey;
        this.status = "OPEN";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void refresh(String title, String message, Instant now) {
        this.title = title;
        this.message = message;
        this.updatedAt = now;
        if ("RESOLVED".equals(this.status)) {
            this.status = "OPEN";
            this.resolvedAt = null;
        }
    }

    public void acknowledge(String by, Instant now) {
        this.status = "ACKNOWLEDGED";
        this.acknowledgedBy = by;
        this.updatedAt = now;
    }

    public void resolve(Instant now) {
        this.status = "RESOLVED";
        this.resolvedAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }
}
