package com.nexora.platform.datasource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateDataSourceRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "DATABRICKS|CSV|JSON|PARQUET|POSTGRESQL") String sourceType,
        String connectionConfigJson) {
}
