package com.nexora.platform.pipeline;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pipeline_definition")
public class PipelineDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "pipeline_type", nullable = false)
    private String pipelineType;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "destination_name", nullable = false)
    private String destinationName;

    @Column(name = "databricks_job_id", nullable = false)
    private Long databricksJobId;

    @Column(name = "data_source_id")
    private Long dataSourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PipelineDefinition() {
    }

    public PipelineDefinition(String name, String description, String pipelineType, String sourceName,
            String destinationName, Long databricksJobId, Long dataSourceId, Instant createdAt) {
        this.name = name;
        this.description = description;
        this.pipelineType = pipelineType;
        this.sourceName = sourceName;
        this.destinationName = destinationName;
        this.databricksJobId = databricksJobId;
        this.dataSourceId = dataSourceId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPipelineType() {
        return pipelineType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public Long getDatabricksJobId() {
        return databricksJobId;
    }

    public Long getDataSourceId() {
        return dataSourceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
