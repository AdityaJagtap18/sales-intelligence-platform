package com.nexora.platform.analytics;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "anomaly_acknowledgement")
public class AnomalyAcknowledgement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anomaly_key", nullable = false, unique = true)
    private String anomalyKey;

    @Column(nullable = false)
    private String status;

    private String note;

    @Column(name = "acknowledged_by", nullable = false)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at", nullable = false)
    private Instant acknowledgedAt;

    protected AnomalyAcknowledgement() {
    }

    public AnomalyAcknowledgement(String anomalyKey, String status, String note, String acknowledgedBy, Instant acknowledgedAt) {
        this.anomalyKey = anomalyKey;
        this.status = status;
        this.note = note;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = acknowledgedAt;
    }

    public Long getId() {
        return id;
    }

    public String getAnomalyKey() {
        return anomalyKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }
}
