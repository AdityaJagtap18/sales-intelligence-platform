package com.nexora.platform.observability;

import java.util.List;

import org.springframework.stereotype.Service;

import com.nexora.platform.pipeline.PipelineDefinitionDto;
import com.nexora.platform.pipeline.PipelineDefinitionService;
import com.nexora.platform.pipeline.PipelineRunDto;
import com.nexora.platform.pipeline.PipelineRunService;

/**
 * Observability view over the pipeline runs the Data Pipelines module
 * already tracks (reuses {@link PipelineDefinitionService} /
 * {@link PipelineRunService} rather than a second pipeline-tracking engine).
 */
@Service
public class PipelineMonitoringService {

    private final PipelineDefinitionService definitionService;
    private final PipelineRunService runService;

    public PipelineMonitoringService(PipelineDefinitionService definitionService, PipelineRunService runService) {
        this.definitionService = definitionService;
        this.runService = runService;
    }

    public List<PipelineReliabilityDto> reliability() {
        return definitionService.list().stream().map(this::toReliability).toList();
    }

    public List<PipelineRunDto> allRuns() {
        return runService.listRuns();
    }

    private PipelineReliabilityDto toReliability(PipelineDefinitionDto def) {
        List<PipelineRunDto> runs = definitionService.runs(def.id());
        int total = runs.size();
        int succeeded = (int) runs.stream().filter(r -> "SUCCESS".equals(r.resultState())).count();
        int failed = (int) runs.stream()
                .filter(r -> r.resultState() != null && !"SUCCESS".equals(r.resultState())).count();
        int terminalRuns = succeeded + failed;
        Double successRate = terminalRuns == 0 ? null : (succeeded * 100.0) / terminalRuns;
        return new PipelineReliabilityDto(def.id(), def.name(), total, succeeded, failed, successRate,
                def.lastRunStatus());
    }
}
