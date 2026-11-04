package com.nexora.platform.databricks;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "databricks")
public record DatabricksProperties(
        String host,
        String token,
        Long jobId,
        String sqlWarehouseId,
        long qualityPollIntervalMs) {
}
