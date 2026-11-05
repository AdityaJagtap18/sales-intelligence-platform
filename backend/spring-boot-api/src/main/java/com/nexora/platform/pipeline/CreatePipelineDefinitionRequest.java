package com.nexora.platform.pipeline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePipelineDefinitionRequest(
        @NotBlank String name,
        String description,
        @NotBlank String pipelineType,
        @NotBlank String sourceName,
        @NotBlank String destinationName,
        @NotNull Long databricksJobId,
        Long dataSourceId) {
}
