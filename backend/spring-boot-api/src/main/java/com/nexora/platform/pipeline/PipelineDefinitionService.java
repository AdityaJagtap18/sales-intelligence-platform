package com.nexora.platform.pipeline;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.nexora.platform.databricks.DatabricksProperties;

/**
 * Pipeline registry (/data-platform/pipelines). Seeds one definition for the
 * single Databricks job this app is configured against (databricks.job-id) —
 * the Bronze/Silver/Gold notebook job Phases 4-5 already run — so the
 * registry reflects what's actually configured rather than an empty list or
 * an invented second pipeline.
 */
@Service
public class PipelineDefinitionService {

    private final PipelineDefinitionRepository repository;
    private final PipelineRunService runService;
    private final DatabricksProperties properties;

    public PipelineDefinitionService(PipelineDefinitionRepository repository, PipelineRunService runService,
            DatabricksProperties properties) {
        this.repository = repository;
        this.runService = runService;
        this.properties = properties;
    }

    @PostConstruct
    void seedDefaultDefinition() {
        if (properties.jobId() == null || !repository.findAllByOrderByNameAsc().isEmpty()) {
            return;
        }
        repository.save(new PipelineDefinition(
                "AdventureWorksLT Bronze/Silver/Gold",
                "Extracts SalesLT tables to Bronze, cleans/validates to Silver, renames to Gold (databricks/notebooks/01-03).",
                "BATCH_ETL", "AdventureWorksLT2022 (SQL Server)", "workspace.gold.*",
                properties.jobId(), null, Instant.now()));
    }

    public List<PipelineDefinitionDto> list() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public PipelineDefinitionDto create(CreatePipelineDefinitionRequest request) {
        PipelineDefinition def = new PipelineDefinition(request.name(), request.description(), request.pipelineType(),
                request.sourceName(), request.destinationName(), request.databricksJobId(), request.dataSourceId(),
                Instant.now());
        return toDto(repository.save(def));
    }

    public List<PipelineRunDto> runs(long definitionId) {
        find(definitionId);
        return runService.listRuns(definitionId);
    }

    public PipelineRunDto trigger(long definitionId, String triggeredBy) {
        PipelineDefinition def = find(definitionId);
        return runService.triggerRun(def.getDatabricksJobId(), definitionId, triggeredBy);
    }

    public List<PipelineDefinitionDto> byDataSource(long dataSourceId) {
        return repository.findAllByDataSourceId(dataSourceId).stream().map(this::toDto).toList();
    }

    private PipelineDefinition find(long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("No pipeline definition with id " + id));
    }

    private PipelineDefinitionDto toDto(PipelineDefinition def) {
        List<PipelineRunDto> runs = runService.listRuns(def.getId());
        PipelineRunDto last = runs.isEmpty() ? null : runs.get(0);
        Long durationMs = null;
        if (last != null && last.completedAt() != null) {
            durationMs = last.completedAt().toEpochMilli() - last.startedAt().toEpochMilli();
        }
        return new PipelineDefinitionDto(def.getId(), def.getName(), def.getDescription(), def.getPipelineType(),
                def.getSourceName(), def.getDestinationName(), def.getDataSourceId(),
                last == null ? null : (last.resultState() != null ? last.resultState() : last.lifecycleState()),
                last == null ? null : last.startedAt().toString(), durationMs);
    }
}
