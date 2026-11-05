package com.nexora.platform.settings;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.nexora.platform.contextlake.MinioProperties;
import com.nexora.platform.contextlake.OllamaProperties;
import com.nexora.platform.contextlake.QdrantProperties;
import com.nexora.platform.databricks.DatabricksProperties;
import com.nexora.platform.graph.GraphProperties;
import com.nexora.platform.settings.SettingsSectionDto.SettingEntryDto;

/**
 * Settings (/admin/settings): a read-only view of this app's actual
 * effective configuration, read from the same {@code @ConfigurationProperties}
 * beans every other module already uses — not a second config store. Every
 * value shown here is startup-bound (environment variables / application.
 * properties); there is no runtime override for any of it, so the page is
 * intentionally read-only rather than implying a save button that doesn't
 * really change anything.
 */
@Service
public class SettingsService {

    private final DatabricksProperties databricksProperties;
    private final MinioProperties minioProperties;
    private final QdrantProperties qdrantProperties;
    private final OllamaProperties ollamaProperties;
    private final GraphProperties graphProperties;
    private final Environment environment;

    public SettingsService(DatabricksProperties databricksProperties, MinioProperties minioProperties,
            QdrantProperties qdrantProperties, OllamaProperties ollamaProperties, GraphProperties graphProperties,
            Environment environment) {
        this.databricksProperties = databricksProperties;
        this.minioProperties = minioProperties;
        this.qdrantProperties = qdrantProperties;
        this.ollamaProperties = ollamaProperties;
        this.graphProperties = graphProperties;
        this.environment = environment;
    }

    public List<SettingsSectionDto> effectiveConfiguration() {
        return List.of(
                new SettingsSectionDto("Databricks (ADR-005)", List.of(
                        entry("Host", databricksProperties.host()),
                        entry("Job ID", String.valueOf(databricksProperties.jobId())),
                        entry("SQL Warehouse ID", databricksProperties.sqlWarehouseId()),
                        entry("Token", mask(databricksProperties.token())))),
                new SettingsSectionDto("Context Lake", List.of(
                        entry("MinIO Endpoint", minioProperties.endpoint()),
                        entry("MinIO Bucket", minioProperties.bucket()),
                        entry("MinIO Secret Key", mask(minioProperties.secretKey())),
                        entry("Qdrant URL", qdrantProperties.url()),
                        entry("Qdrant Collection", qdrantProperties.collection()),
                        entry("Qdrant Vector Size", String.valueOf(qdrantProperties.vectorSize())),
                        entry("Ollama URL", ollamaProperties.url()),
                        entry("Embedding Model", ollamaProperties.embeddingModel()),
                        entry("Default Chat Model", ollamaProperties.chatModel()))),
                new SettingsSectionDto("Knowledge Graph", List.of(
                        entry("Neo4j URI", graphProperties.uri()),
                        entry("Neo4j User", graphProperties.user()),
                        entry("Neo4j Password", mask(graphProperties.password())))),
                new SettingsSectionDto("Scheduled Jobs", List.of(
                        entry("Data Quality Poll Interval", millisToReadable(databricksProperties.qualityPollIntervalMs())),
                        entry("Catalog Sync Interval", millisToReadable(environment.getProperty(
                                "databricks.catalog-sync-interval-ms", Long.class, 300000L))),
                        entry("Knowledge Graph Sync Interval", millisToReadable(environment.getProperty(
                                "graph.sync-interval-ms", Long.class, 600000L))),
                        entry("Alert Evaluation Interval", millisToReadable(environment.getProperty(
                                "alerts.evaluation-interval-ms", Long.class, 60000L))))));
    }

    private SettingEntryDto entry(String key, String value) {
        return new SettingEntryDto(key, value == null || value.isBlank() ? "(not set)" : value);
    }

    private String mask(String secret) {
        return secret == null || secret.isBlank() ? "(not set)" : "••••••••";
    }

    private String millisToReadable(long millis) {
        if (millis % 60000 == 0) {
            return (millis / 60000) + " min";
        }
        return (millis / 1000) + " sec";
    }
}
