package com.nexora.platform.datasource;

import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.platform.databricks.DatabricksProperties;
import com.nexora.platform.databricks.DatabricksSqlClient;

/**
 * Source registration (/data-platform/sources). This app has exactly two live
 * connections it can actually test — the shared Databricks SQL Warehouse
 * ({@link DatabricksSqlClient}) and this app's own Postgres connection pool —
 * so every registered source of type DATABRICKS or POSTGRESQL is tested
 * against that one shared connection (there is no per-source credential
 * store; ADR-005 keeps Databricks as the only cloud dependency). CSV/JSON/
 * PARQUET sources are file-based and have no live connection to test —
 * their status says so rather than faking a ping.
 */
@Service
public class DataSourceService {

    private final DataSourceRepository repository;
    private final DatabricksSqlClient sqlClient;
    private final javax.sql.DataSource jdbcDataSource;
    private final DatabricksProperties databricksProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataSourceService(DataSourceRepository repository, DatabricksSqlClient sqlClient, javax.sql.DataSource jdbcDataSource,
            DatabricksProperties databricksProperties) {
        this.repository = repository;
        this.sqlClient = sqlClient;
        this.jdbcDataSource = jdbcDataSource;
        this.databricksProperties = databricksProperties;
    }

    /**
     * Seeds one entry for the Databricks connection every other page already reads through
     * (configured via env, not through this registry) — otherwise this page shows "nothing
     * registered" even though a real source is live, the same gap {@code PipelineDefinitionService}
     * closes for the Pipelines registry.
     */
    @PostConstruct
    void seedDefaultSource() {
        if (databricksProperties.host() == null || !repository.findAllByOrderByNameAsc().isEmpty()) {
            return;
        }
        String config = "{\"host\": \"" + databricksProperties.host() + "\"}";
        DataSource source = new DataSource("Databricks SQL Warehouse", "DATABRICKS", config, Instant.now());
        repository.save(source);
    }

    public List<DataSourceDto> list() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public DataSourceDto register(CreateDataSourceRequest request) {
        DataSource source = new DataSource(request.name(), request.sourceType(), request.connectionConfigJson(), Instant.now());
        return toDto(repository.save(source));
    }

    public DataSourceDto update(long id, CreateDataSourceRequest request) {
        DataSource source = find(id);
        source.setName(request.name());
        source.setConnectionConfigJson(request.connectionConfigJson());
        return toDto(repository.save(source));
    }

    public DataSourceDto setEnabled(long id, boolean enabled) {
        DataSource source = find(id);
        source.setEnabled(enabled);
        return toDto(repository.save(source));
    }

    public DataSourceDto testConnection(long id) {
        DataSource source = find(id);
        Instant now = Instant.now();
        switch (source.getSourceType()) {
            case "DATABRICKS" -> {
                try {
                    sqlClient.executeQuery("SELECT 1");
                    source.recordTest("OK", "Connected via the shared Databricks SQL Warehouse.", now);
                } catch (Exception e) {
                    source.recordTest("FAILED", "Databricks SQL Warehouse unreachable: " + e.getMessage(), now);
                }
            }
            case "POSTGRESQL" -> {
                try (Connection c = jdbcDataSource.getConnection()) {
                    c.createStatement().execute("SELECT 1");
                    source.recordTest("OK", "Connected via the application's own Postgres pool.", now);
                } catch (Exception e) {
                    source.recordTest("FAILED", "Postgres unreachable: " + e.getMessage(), now);
                }
            }
            default -> source.recordTest("NOT_APPLICABLE",
                    source.getSourceType() + " is a file-based source format with no live connection to test.", now);
        }
        return toDto(repository.save(source));
    }

    private DataSource find(long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("No data source with id " + id));
    }

    private DataSourceDto toDto(DataSource s) {
        return new DataSourceDto(s.getId(), s.getName(), s.getSourceType(), maskedSummary(s), s.isEnabled(),
                s.getLastTestStatus(), s.getLastTestMessage(),
                s.getLastTestedAt() == null ? null : s.getLastTestedAt().toString(),
                s.getLastSuccessfulIngestionAt() == null ? null : s.getLastSuccessfulIngestionAt().toString());
    }

    @SuppressWarnings("unchecked")
    private String maskedSummary(DataSource s) {
        if (s.getConnectionConfigJson() == null || s.getConnectionConfigJson().isBlank()) {
            return "(no configuration)";
        }
        try {
            var map = objectMapper.readValue(s.getConnectionConfigJson(), java.util.Map.class);
            StringBuilder sb = new StringBuilder();
            map.forEach((k, v) -> {
                String key = String.valueOf(k);
                boolean sensitive = key.toLowerCase().contains("password") || key.toLowerCase().contains("token")
                        || key.toLowerCase().contains("secret") || key.toLowerCase().contains("key");
                sb.append(key).append('=').append(sensitive ? "••••••" : v).append("; ");
            });
            return sb.toString().strip();
        } catch (Exception e) {
            return "(invalid configuration)";
        }
    }
}
