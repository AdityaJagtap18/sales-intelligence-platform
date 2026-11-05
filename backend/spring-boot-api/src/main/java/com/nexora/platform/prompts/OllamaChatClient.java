package com.nexora.platform.prompts;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.nexora.platform.modelconfig.ModelConfigurationService;

/**
 * Non-streaming generate call against the same Ollama instance Context Lake
 * already uses for embeddings (Phase 6's {@code ollamaRestClient} bean) — no
 * second Ollama client, just a second call against it. Which model to call
 * is read from {@link ModelConfigurationService} (Phase 3 Group 5's Model
 * Configuration page) rather than the static {@code ollama.chat-model}
 * property, so an admin's model choice there takes effect immediately.
 */
@Component
public class OllamaChatClient {

    private final RestClient restClient;
    private final ModelConfigurationService modelConfigurationService;

    public OllamaChatClient(RestClient ollamaRestClient, ModelConfigurationService modelConfigurationService) {
        this.restClient = ollamaRestClient;
        this.modelConfigurationService = modelConfigurationService;
    }

    public String chatModel() {
        return modelConfigurationService.activeChatModel();
    }

    @SuppressWarnings("unchecked")
    public String generate(String prompt) {
        Map<String, Object> response = restClient.post()
                .uri("/api/generate")
                .body(Map.of("model", chatModel(), "prompt", prompt, "stream", false))
                .retrieve()
                .body(Map.class);
        Object text = response.get("response");
        return text == null ? "" : text.toString();
    }
}
