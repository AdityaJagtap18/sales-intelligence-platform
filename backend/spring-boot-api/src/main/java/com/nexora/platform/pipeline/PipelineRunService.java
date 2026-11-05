package com.nexora.platform.pipeline;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.nexora.platform.databricks.DatabricksJobsClient;
import com.nexora.platform.databricks.DatabricksJobsClient.RunStatus;
import com.nexora.platform.databricks.DatabricksProperties;
import com.nexora.platform.databricks.DatabricksSqlClient;

@Service
public class PipelineRunService {

    private final DatabricksJobsClient jobsClient;
    private final DatabricksProperties properties;
    private final PipelineRunRepository repository;
    private final DatabricksSqlClient sqlClient;

    public PipelineRunService(DatabricksJobsClient jobsClient, DatabricksProperties properties,
            PipelineRunRepository repository, DatabricksSqlClient sqlClient) {
        this.jobsClient = jobsClient;
        this.properties = properties;
        this.repository = repository;
        this.sqlClient = sqlClient;
    }

    public PipelineRunDto triggerRun(String triggeredBy) {
        long jobId = properties.jobId();
        long databricksRunId = jobsClient.runNow(jobId);
        Instant now = Instant.now();
        PipelineRun run = new PipelineRun(jobId, databricksRunId, "PENDING", triggeredBy, now);
        return PipelineRunDto.from(repository.save(run));
    }

    public PipelineRunDto triggerRun(long jobId, Long pipelineDefinitionId, String triggeredBy) {
        long databricksRunId = jobsClient.runNow(jobId);
        Instant now = Instant.now();
        PipelineRun run = new PipelineRun(jobId, databricksRunId, "PENDING", triggeredBy, now, pipelineDefinitionId);
        return PipelineRunDto.from(repository.save(run));
    }

    public List<PipelineRunDto> listRuns(long pipelineDefinitionId) {
        return repository.findAllByPipelineDefinitionIdOrderByStartedAtDesc(pipelineDefinitionId).stream()
                .map(PipelineRunDto::from).toList();
    }

    public PipelineRunDto refreshRun(Long id) {
        PipelineRun run = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No pipeline run with id " + id));
        if (!isTerminal(run.getLifecycleState())) {
            RunStatus status = jobsClient.getRunStatus(run.getDatabricksRunId());
            run.applyStatus(status.lifecycleState(), status.resultState(), status.stateMessage(), Instant.now());
            run = repository.save(run);
            if (isTerminal(run.getLifecycleState())) {
                // The job just finished writing Gold-layer / quality-report tables --
                // drop cached Databricks reads so the next page load sees the new data
                // immediately rather than waiting out the cache TTL.
                sqlClient.evictAll();
            }
        }
        return PipelineRunDto.from(run);
    }

    public List<PipelineRunDto> listRuns() {
        return repository.findAllByOrderByStartedAtDesc().stream().map(PipelineRunDto::from).toList();
    }

    private boolean isTerminal(String lifecycleState) {
        return "TERMINATED".equals(lifecycleState) || "SKIPPED".equals(lifecycleState)
                || "INTERNAL_ERROR".equals(lifecycleState);
    }
}
