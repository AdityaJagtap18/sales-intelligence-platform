package com.nexora.platform.datasource;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.nexora.platform.pipeline.PipelineDefinitionDto;
import com.nexora.platform.pipeline.PipelineDefinitionService;

@RestController
@RequestMapping("/sources")
public class DataSourceController {

    private final DataSourceService service;
    private final PipelineDefinitionService pipelineDefinitionService;

    public DataSourceController(DataSourceService service, PipelineDefinitionService pipelineDefinitionService) {
        this.service = service;
        this.pipelineDefinitionService = pipelineDefinitionService;
    }

    @GetMapping("/{id}/pipelines")
    public List<PipelineDefinitionDto> pipelines(@PathVariable long id) {
        return pipelineDefinitionService.byDataSource(id);
    }

    @GetMapping
    public List<DataSourceDto> list() {
        return service.list();
    }

    @PostMapping
    public DataSourceDto register(@Valid @RequestBody CreateDataSourceRequest request) {
        return service.register(request);
    }

    @PutMapping("/{id}")
    public DataSourceDto update(@PathVariable long id, @Valid @RequestBody CreateDataSourceRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/disable")
    public DataSourceDto disable(@PathVariable long id) {
        return service.setEnabled(id, false);
    }

    @PostMapping("/{id}/enable")
    public DataSourceDto enable(@PathVariable long id) {
        return service.setEnabled(id, true);
    }

    @PostMapping("/{id}/test-connection")
    public DataSourceDto testConnection(@PathVariable long id) {
        return service.testConnection(id);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
