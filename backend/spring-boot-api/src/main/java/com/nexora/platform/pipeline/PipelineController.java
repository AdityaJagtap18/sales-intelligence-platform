package com.nexora.platform.pipeline;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Angular-facing control-plane endpoints for the Databricks pipeline (ADR-005).
 * Angular never calls Databricks directly and never sees its credentials —
 * it only talks to this controller.
 */
@RestController
@RequestMapping("/pipelines")
public class PipelineController {

    private final PipelineRunService service;

    public PipelineController(PipelineRunService service) {
        this.service = service;
    }

    @PostMapping("/run")
    public ResponseEntity<PipelineRunDto> run() {
        PipelineRunDto dto = service.triggerRun("angular-ui");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    @GetMapping("/runs")
    public List<PipelineRunDto> runs() {
        return service.listRuns();
    }

    @GetMapping("/runs/{id}")
    public PipelineRunDto run(@PathVariable Long id) {
        return service.refreshRun(id);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }
}
