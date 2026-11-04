package com.nexora.platform.analytics;

/**
 * {@code key} is a stable identifier (metric + order) for acknowledge/resolve
 * persistence in {@code anomaly_acknowledgement} — it is not a database id,
 * since anomalies are computed on demand from the Gold layer rather than
 * stored as discrete detection events.
 */
public record AnomalyRowDto(String key, String metric, String date, String impact, String direction, int confidence,
        double expectedValue, double observedValue, String acknowledgementStatus, String acknowledgementNote) {

    public AnomalyRowDto withAcknowledgement(String status, String note) {
        return new AnomalyRowDto(key, metric, date, impact, direction, confidence, expectedValue, observedValue, status, note);
    }
}
