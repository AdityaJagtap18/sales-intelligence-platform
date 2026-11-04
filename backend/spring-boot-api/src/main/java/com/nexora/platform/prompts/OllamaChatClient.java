package com.nexora.platform.prompts;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.nexora.platform.contextlake.OllamaProperties;

/**
 * Non-streaming generate call against the same Ollama instance/properties
 * Context Lake already uses for embeddings (Phase 6's {@code ollamaRestClient}
 * bean) — no second Ollama client, just a second call against it.
 */
@Component
public class OllamaChatClient {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaChatClient(RestClient ollamaRestClient, OllamaProperties properties) {
        this.restClient = ollamaRestClient;
        this.properties = properties;
    }

    public String chatModel() {
        return properties.chatModel();
    }

    @SuppressWarnings("unchecked")
    public String generate(String prompt) {
        Map<String, Object> response = restClient.post()
                .uri("/api/generate")
                .body(Map.of("model", properties.chatModel(), "prompt", prompt, "stream", false))
                .retrieve()
                .body(Map.class);
        Object text = response.get("response");
        return text == null ? "" : text.toString();
    }
}
