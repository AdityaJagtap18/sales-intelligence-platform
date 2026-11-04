package com.nexora.platform.reports;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_execution")
public class ReportExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_key", nullable = false)
    private String reportKey;

    @Column(name = "report_name", nullable = false)
    private String reportName;

    @Column(name = "filters_json")
    private String filtersJson;

    @Column(nullable = false)
    private String status;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected ReportExecution() {
    }

    public ReportExecution(String reportKey, String reportName, String filtersJson, String status, Integer rowCount,
            String errorMessage, String requestedBy, Instant generatedAt) {
        this.reportKey = reportKey;
        this.reportName = reportName;
        this.filtersJson = filtersJson;
        this.status = status;
        this.rowCount = rowCount;
        this.errorMessage = errorMessage;
        this.requestedBy = requestedBy;
        this.generatedAt = generatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getReportKey() {
        return reportKey;
    }

    public String getReportName() {
        return reportName;
    }

    public String getStatus() {
        return status;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
