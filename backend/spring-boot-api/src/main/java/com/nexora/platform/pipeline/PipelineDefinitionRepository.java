package com.nexora.platform.pipeline;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineDefinitionRepository extends JpaRepository<PipelineDefinition, Long> {
    List<PipelineDefinition> findAllByOrderByNameAsc();

    List<PipelineDefinition> findAllByDataSourceId(Long dataSourceId);
}
