package com.nexora.platform.contextlake;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaEmbeddingClient {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaEmbeddingClient(RestClient ollamaRestClient, OllamaProperties properties) {
        this.restClient = ollamaRestClient;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public List<Double> embed(String text) {
        Map<String, Object> response = restClient.post()
                .uri("/api/embeddings")
                .body(Map.of("model", properties.embeddingModel(), "prompt", text))
                .retrieve()
                .body(Map.class);
        return (List<Double>) response.get("embedding");
    }
}
