package com.nexora.platform.pipeline;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/pipelines/definitions")
public class PipelineDefinitionController {

    private final PipelineDefinitionService service;

    public PipelineDefinitionController(PipelineDefinitionService service) {
        this.service = service;
    }

    @GetMapping
    public List<PipelineDefinitionDto> list() {
        return service.list();
    }

    @PostMapping
    public PipelineDefinitionDto create(@Valid @RequestBody CreatePipelineDefinitionRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}/runs")
    public List<PipelineRunDto> runs(@PathVariable long id) {
        return service.runs(id);
    }

    @PostMapping("/{id}/trigger")
    public PipelineRunDto trigger(@PathVariable long id) {
        return service.trigger(id, "angular-ui");
    }

    @PostMapping("/{id}/retry")
    public PipelineRunDto retry(@PathVariable long id) {
        return service.trigger(id, "angular-ui-retry");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
