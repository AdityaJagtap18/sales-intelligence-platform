package com.nexora.platform.contextlake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.platform.databricks.DatabricksSqlClient;

/**
 * Keeps catalog_dataset in sync with workspace.gold.* in Unity Catalog, so
 * the Context Lake catalog page reflects real metadata rather than a
 * snapshot taken once at ingestion time (ADR-005: Databricks remains the
 * source of truth for analytical data; Spring Boot only mirrors metadata).
 */
@Service
public class CatalogSyncService {

    private static final Logger log = LoggerFactory.getLogger(CatalogSyncService.class);
    private static final String COLUMNS_SQL = """
            SELECT table_name, column_name, data_type
            FROM workspace.information_schema.columns
            WHERE table_schema = 'gold'
            ORDER BY table_name, ordinal_position
            """;

    private final DatabricksSqlClient sqlClient;
    private final CatalogDatasetRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CatalogSyncService(DatabricksSqlClient sqlClient, CatalogDatasetRepository repository) {
        this.sqlClient = sqlClient;
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${databricks.catalog-sync-interval-ms:300000}", initialDelay = 15000)
    public void scheduledSync() {
        try {
            sync();
        } catch (Exception e) {
            log.warn("Failed to sync the Context Lake catalog from Databricks: {}", e.getMessage());
        }
    }

    public int sync() {
        Map<String, List<ColumnInfo>> columnsByTable = new LinkedHashMap<>();
        for (List<String> row : sqlClient.executeQuery(COLUMNS_SQL)) {
            String tableName = row.get(0);
            columnsByTable.computeIfAbsent(tableName, k -> new ArrayList<>())
                    .add(new ColumnInfo(row.get(1), row.get(2)));
        }
        if (columnsByTable.isEmpty()) {
            return 0;
        }

        Map<String, Long> rowCounts = fetchRowCounts(columnsByTable.keySet());
        Instant now = Instant.now();

        for (Map.Entry<String, List<ColumnInfo>> entry : columnsByTable.entrySet()) {
            String catalogTableName = "gold." + entry.getKey();
            long rowCount = rowCounts.getOrDefault(entry.getKey(), 0L);
            String columnsJson = writeColumnsJson(entry.getValue());

            repository.findByTableName(catalogTableName)
                    .map(existing -> {
                        existing.update(rowCount, columnsJson, now);
                        return repository.save(existing);
                    })
                    .orElseGet(() -> repository.save(new CatalogDataset(catalogTableName, rowCount, columnsJson, now)));
        }
        log.debug("Synced {} Context Lake catalog datasets", columnsByTable.size());
        return columnsByTable.size();
    }

    private Map<String, Long> fetchRowCounts(java.util.Set<String> tableNames) {
        String unionSql = tableNames.stream()
                .map(table -> "SELECT '" + table.replace("'", "''") + "' AS table_name, COUNT(*) AS cnt FROM workspace.gold." + table)
                .reduce((a, b) -> a + " UNION ALL " + b)
                .orElseThrow();

        Map<String, Long> rowCounts = new LinkedHashMap<>();
        for (List<String> row : sqlClient.executeQuery(unionSql)) {
            rowCounts.put(row.get(0), Long.parseLong(row.get(1)));
        }
        return rowCounts;
    }

    private String writeColumnsJson(List<ColumnInfo> columns) {
        try {
            return objectMapper.writeValueAsString(columns);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize column metadata", e);
        }
    }
}
