package com.nexora.platform.dataquality;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexora.platform.pipeline.PipelineDefinitionDto;
import com.nexora.platform.pipeline.PipelineDefinitionService;
import com.nexora.platform.pipeline.PipelineRunDto;

@RestController
@RequestMapping("/data-quality")
public class DataQualityController {

    private final DataQualityService service;
    private final PipelineDefinitionService pipelineDefinitionService;

    public DataQualityController(DataQualityService service, PipelineDefinitionService pipelineDefinitionService) {
        this.service = service;
        this.pipelineDefinitionService = pipelineDefinitionService;
    }

    @GetMapping("/latest")
    public QualitySummaryDto latest() {
        return service.latestSummary();
    }

    @GetMapping("/history")
    public List<QualityRunHistoryDto> history() {
        return service.history();
    }

    @GetMapping("/runs/{runId}")
    public QualitySummaryDto runDetail(@PathVariable String runId) {
        return service.runDetail(runId);
    }

    /**
     * Quality checks run inside the Bronze-to-Silver Databricks notebook, not as a
     * standalone Spring Boot job — "Execute checks" re-triggers that pipeline, the
     * same action Data Pipelines' manual-trigger button performs.
     */
    @PostMapping("/execute")
    public PipelineRunDto execute() {
        List<PipelineDefinitionDto> defs = pipelineDefinitionService.list();
        if (defs.isEmpty()) {
            throw new IllegalStateException("No pipeline is registered to run data quality checks against. "
                    + "Configure DATABRICKS_JOB_ID and register a pipeline under Data Pipelines first.");
        }
        return pipelineDefinitionService.trigger(defs.get(0).id(), "angular-ui-data-quality");
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export() {
        QualitySummaryDto summary = service.latestSummary();
        StringBuilder csv = new StringBuilder("Table,Check,Status\n");
        for (QualityCheckRowDto row : summary.checks()) {
            csv.append(csvField(row.tableName())).append(',').append(csvField(row.checkName())).append(',')
                    .append(csvField(row.status())).append('\n');
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("data-quality-report.csv").build().toString())
                .body(body);
    }

    private static String csvField(String value) {
        return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
