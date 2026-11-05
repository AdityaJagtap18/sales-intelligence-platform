package com.nexora.platform.observability;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexora.platform.pipeline.PipelineDefinitionService;
import com.nexora.platform.pipeline.PipelineRunDto;

@RestController
@RequestMapping("/observability/pipeline-monitoring")
public class PipelineMonitoringController {

    private final PipelineMonitoringService service;
    private final PipelineDefinitionService definitionService;

    public PipelineMonitoringController(PipelineMonitoringService service, PipelineDefinitionService definitionService) {
        this.service = service;
        this.definitionService = definitionService;
    }

    @GetMapping("/reliability")
    public List<PipelineReliabilityDto> reliability() {
        return service.reliability();
    }

    @GetMapping("/runs")
    public List<PipelineRunDto> runs() {
        return service.allRuns();
    }

    @PostMapping("/trigger/{definitionId}")
    public ResponseEntity<PipelineRunDto> trigger(@PathVariable long definitionId) {
        PipelineRunDto dto = definitionService.trigger(definitionId, "pipeline-monitoring-ui");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }
}
