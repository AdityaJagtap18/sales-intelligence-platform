package com.nexora.platform.contextlake;

import java.time.Instant;
import java.util.List;

public record CatalogDatasetDto(
        Long id,
        String tableName,
        long rowCount,
        List<ColumnInfo> columns,
        Instant lastRefreshedAt) {
}
