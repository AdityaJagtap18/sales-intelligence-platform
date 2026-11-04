package com.nexora.platform.forecasting;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "forecast_run")
public class ForecastRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String metric;

    @Column(name = "horizon_periods", nullable = false)
    private Integer horizonPeriods;

    @Column(nullable = false)
    private String status;

    private String method;

    private Double mae;

    private Double mape;

    private String reason;

    @Column(name = "result_json")
    private String resultJson;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ForecastRun() {
    }

    public ForecastRun(String metric, Integer horizonPeriods, String status, String method, Double mae, Double mape,
            String reason, String resultJson, String requestedBy, Instant createdAt) {
        this.metric = metric;
        this.horizonPeriods = horizonPeriods;
        this.status = status;
        this.method = method;
        this.mae = mae;
        this.mape = mape;
        this.reason = reason;
        this.resultJson = resultJson;
        this.requestedBy = requestedBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getMetric() {
        return metric;
    }

    public Integer getHorizonPeriods() {
        return horizonPeriods;
    }

    public String getStatus() {
        return status;
    }

    public String getMethod() {
        return method;
    }

    public Double getMae() {
        return mae;
    }

    public Double getMape() {
        return mape;
    }

    public String getReason() {
        return reason;
    }

    public String getResultJson() {
        return resultJson;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
