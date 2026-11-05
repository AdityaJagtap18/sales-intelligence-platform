package com.nexora.platform.datasource;

/** Never carries the raw connection_config_json — only a masked summary ({@code configSummary}). */
public record DataSourceDto(
        Long id,
        String name,
        String sourceType,
        String configSummary,
        boolean enabled,
        String lastTestStatus,
        String lastTestMessage,
        String lastTestedAt,
        String lastSuccessfulIngestionAt) {
}
