package com.nexora.platform.contextlake;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Minimal REST wrapper over Qdrant (collections/points) — avoids pulling in
 * the official gRPC-based client for what this app needs: upsert and search.
 */
@Component
public class QdrantClient {

    private final RestClient restClient;
    private final QdrantProperties properties;
    private final AtomicBoolean collectionEnsured = new AtomicBoolean(false);

    public QdrantClient(RestClient qdrantRestClient, QdrantProperties properties) {
        this.restClient = qdrantRestClient;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public void ensureCollection() {
        if (collectionEnsured.get()) {
            return;
        }
        Map<String, Object> existsResponse = restClient.get()
                .uri("/collections/{name}/exists", properties.collection())
                .retrieve()
                .body(Map.class);
        Object result = existsResponse.get("result");
        boolean exists = result instanceof Map<?, ?> resultMap && Boolean.TRUE.equals(resultMap.get("exists"));

        if (!exists) {
            restClient.put()
                    .uri("/collections/{name}", properties.collection())
                    .body(Map.of("vectors", Map.of("size", properties.vectorSize(), "distance", "Cosine")))
                    .retrieve()
                    .toBodilessEntity();
        }
        collectionEnsured.set(true);
    }

    public void upsert(List<Map<String, Object>> points) {
        ensureCollection();
        restClient.put()
                .uri("/collections/{name}/points", properties.collection())
                .body(Map.of("points", points))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(List<Double> vector, int limit) {
        ensureCollection();
        Map<String, Object> response = restClient.post()
                .uri("/collections/{name}/points/search", properties.collection())
                .body(Map.of("vector", vector, "limit", limit, "with_payload", true))
                .retrieve()
                .body(Map.class);
        Object result = response.get("result");
        return result instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    public void deleteByDocumentId(long documentId) {
        ensureCollection();
        restClient.post()
                .uri("/collections/{name}/points/delete", properties.collection())
                .body(Map.of("filter", Map.of(
                        "must", List.of(Map.of("key", "documentId", "match", Map.of("value", documentId))))))
                .retrieve()
                .toBodilessEntity();
    }
}
