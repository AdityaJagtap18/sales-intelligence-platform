package com.nexora.platform.contextlake;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class DocumentSearchService {

    private final OllamaEmbeddingClient embeddingClient;
    private final QdrantClient qdrantClient;

    public DocumentSearchService(OllamaEmbeddingClient embeddingClient, QdrantClient qdrantClient) {
        this.embeddingClient = embeddingClient;
        this.qdrantClient = qdrantClient;
    }

    @SuppressWarnings("unchecked")
    public List<SearchResultDto> search(String query, int limit) {
        List<Double> vector = embeddingClient.embed(query);
        return qdrantClient.search(vector, limit).stream()
                .map(point -> {
                    Map<String, Object> payload = (Map<String, Object>) point.get("payload");
                    double score = ((Number) point.get("score")).doubleValue();
                    return new SearchResultDto(
                            ((Number) payload.get("documentId")).longValue(),
                            (String) payload.get("filename"),
                            ((Number) payload.get("chunkIndex")).intValue(),
                            (String) payload.get("text"),
                            score);
                })
                .toList();
    }
}
