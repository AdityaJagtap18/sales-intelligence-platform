package com.nexora.platform.pipeline;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, Long> {
    List<PipelineRun> findAllByOrderByStartedAtDesc();

    List<PipelineRun> findAllByPipelineDefinitionIdOrderByStartedAtDesc(Long pipelineDefinitionId);

    Optional<PipelineRun> findByDatabricksRunId(Long databricksRunId);
}
