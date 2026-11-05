package com.nexora.platform.datasource;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "data_source")
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "connection_config_json")
    private String connectionConfigJson;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_test_status")
    private String lastTestStatus;

    @Column(name = "last_test_message")
    private String lastTestMessage;

    @Column(name = "last_tested_at")
    private Instant lastTestedAt;

    @Column(name = "last_successful_ingestion_at")
    private Instant lastSuccessfulIngestionAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DataSource() {
    }

    public DataSource(String name, String sourceType, String connectionConfigJson, Instant createdAt) {
        this.name = name;
        this.sourceType = sourceType;
        this.connectionConfigJson = connectionConfigJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getConnectionConfigJson() {
        return connectionConfigJson;
    }

    public void setConnectionConfigJson(String connectionConfigJson) {
        this.connectionConfigJson = connectionConfigJson;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLastTestStatus() {
        return lastTestStatus;
    }

    public String getLastTestMessage() {
        return lastTestMessage;
    }

    public Instant getLastTestedAt() {
        return lastTestedAt;
    }

    public Instant getLastSuccessfulIngestionAt() {
        return lastSuccessfulIngestionAt;
    }

    public void recordTest(String status, String message, Instant now) {
        this.lastTestStatus = status;
        this.lastTestMessage = message;
        this.lastTestedAt = now;
        if ("OK".equals(status)) {
            this.lastSuccessfulIngestionAt = now;
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
