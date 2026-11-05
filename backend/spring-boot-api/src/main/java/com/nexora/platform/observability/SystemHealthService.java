package com.nexora.platform.observability;

import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;

import org.neo4j.driver.Driver;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.nexora.platform.contextlake.MinioProperties;
import com.nexora.platform.contextlake.OllamaProperties;
import com.nexora.platform.contextlake.QdrantProperties;
import com.nexora.platform.databricks.DatabricksProperties;
import com.nexora.platform.databricks.DatabricksSqlClient;
import com.nexora.platform.graph.GraphProperties;

import io.minio.ListBucketsArgs;
import io.minio.MinioClient;

/**
 * Real connectivity checks against every external dependency this app
 * actually holds a client for (ADR-005's local-plus-Databricks stack), reused
 * from the same beans the rest of the app uses to talk to them — never a
 * fabricated "all systems operational" screen.
 */
@Service
public class SystemHealthService {

    private final javax.sql.DataSource jdbcDataSource;
    private final DatabricksSqlClient databricksSqlClient;
    private final DatabricksProperties databricksProperties;
    private final Driver neo4jDriver;
    private final RestClient qdrantRestClient;
    private final QdrantProperties qdrantProperties;
    private final RestClient ollamaRestClient;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final GraphProperties graphProperties;

    public SystemHealthService(javax.sql.DataSource jdbcDataSource, DatabricksSqlClient databricksSqlClient,
            DatabricksProperties databricksProperties, Driver neo4jDriver, RestClient qdrantRestClient,
            QdrantProperties qdrantProperties, RestClient ollamaRestClient, MinioClient minioClient,
            MinioProperties minioProperties, GraphProperties graphProperties) {
        this.jdbcDataSource = jdbcDataSource;
        this.databricksSqlClient = databricksSqlClient;
        this.databricksProperties = databricksProperties;
        this.neo4jDriver = neo4jDriver;
        this.qdrantRestClient = qdrantRestClient;
        this.qdrantProperties = qdrantProperties;
        this.ollamaRestClient = ollamaRestClient;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.graphProperties = graphProperties;
    }

    public SystemHealthDto check() {
        List<ComponentHealthDto> components = List.of(
                checkPostgres(),
                checkDatabricks(),
                checkNeo4j(),
                checkQdrant(),
                checkOllama(),
                checkMinio());
        String overall = components.stream().anyMatch(c -> "DOWN".equals(c.status())) ? "DOWN"
                : components.stream().anyMatch(c -> "DEGRADED".equals(c.status())) ? "DEGRADED" : "UP";
        return new SystemHealthDto(overall, Instant.now(), components);
    }

    private ComponentHealthDto checkPostgres() {
        return timed("Postgres", "database", () -> {
            try (Connection c = jdbcDataSource.getConnection()) {
                c.createStatement().execute("SELECT 1");
            }
            return "This application's own operational database (pipeline runs, alerts, acknowledgements).";
        });
    }

    private ComponentHealthDto checkDatabricks() {
        if (databricksProperties.host() == null || databricksProperties.host().isBlank()
                || databricksProperties.sqlWarehouseId() == null || databricksProperties.sqlWarehouseId().isBlank()) {
            return new ComponentHealthDto("Databricks SQL Warehouse", "cloud", "NOT_CONFIGURED", null,
                    "DATABRICKS_HOST / DATABRICKS_SQL_WAREHOUSE_ID is not set.");
        }
        return timed("Databricks SQL Warehouse", "cloud", () -> {
            databricksSqlClient.executeQuery("SELECT 1");
            return "Reachable via the SQL Statement Execution API (ADR-005's only cloud dependency).";
        });
    }

    private ComponentHealthDto checkNeo4j() {
        return timed("Neo4j", "database", () -> {
            neo4jDriver.verifyConnectivity();
            return "Knowledge graph store at " + graphProperties.uri();
        });
    }

    private ComponentHealthDto checkQdrant() {
        return timed("Qdrant", "vector-store", () -> {
            qdrantRestClient.get().uri("/collections/" + qdrantProperties.collection()).retrieve().toBodilessEntity();
            return "Context Lake vector collection '" + qdrantProperties.collection() + "' is reachable.";
        });
    }

    private ComponentHealthDto checkOllama() {
        return timed("Ollama", "model-runtime", () -> {
            ollamaRestClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return "Local embedding/chat model runtime is reachable.";
        });
    }

    private ComponentHealthDto checkMinio() {
        return timed("MinIO", "object-store", () -> {
            minioClient.listBuckets(ListBucketsArgs.builder().build());
            return "Context Lake document bucket '" + minioProperties.bucket() + "' endpoint is reachable.";
        });
    }

    private ComponentHealthDto timed(String name, String category, Callable<String> check) {
        long start = System.nanoTime();
        try {
            String message = check.call();
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            return new ComponentHealthDto(name, category, "UP", latencyMs, message);
        } catch (Exception e) {
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            return new ComponentHealthDto(name, category, "DOWN", latencyMs, e.getMessage());
        }
    }
}
