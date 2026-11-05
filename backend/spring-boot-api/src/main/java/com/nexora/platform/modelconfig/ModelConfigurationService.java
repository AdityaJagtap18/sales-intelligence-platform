package com.nexora.platform.modelconfig;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.nexora.platform.contextlake.OllamaProperties;

/**
 * Model Configuration (/admin/model-configuration). Lets an admin switch
 * which locally-pulled Ollama chat model {@link
 * com.nexora.platform.prompts.OllamaChatClient} uses for Prompt Playground
 * and AI Insights — reusing the same {@code ollamaRestClient} bean Context
 * Lake already uses for embeddings, not a second Ollama client.
 */
@Service
public class ModelConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigurationService.class);
    private static final long ROW_ID = 1L;

    private final ModelConfigurationRepository repository;
    private final RestClient ollamaRestClient;
    private final OllamaProperties properties;

    public ModelConfigurationService(ModelConfigurationRepository repository, RestClient ollamaRestClient,
            OllamaProperties properties) {
        this.repository = repository;
        this.ollamaRestClient = ollamaRestClient;
        this.properties = properties;
    }

    /** The model {@link com.nexora.platform.prompts.OllamaChatClient} should actually call. */
    public String activeChatModel() {
        return repository.findById(ROW_ID).map(ModelConfiguration::getActiveChatModel)
                .filter(m -> m != null && !m.isBlank())
                .orElse(properties.chatModel());
    }

    public ModelConfigurationDto current() {
        List<String> available;
        String status;
        try {
            available = availableModels();
            status = "UP";
        } catch (Exception e) {
            log.debug("Model configuration: could not list Ollama models: {}", e.getMessage());
            available = List.of();
            status = "DOWN";
        }
        ModelConfiguration row = repository.findById(ROW_ID).orElse(null);
        return new ModelConfigurationDto(activeChatModel(), properties.chatModel(), properties.embeddingModel(),
                available, status, row == null ? null : row.getUpdatedAt());
    }

    public ModelConfigurationDto setActiveChatModel(String model) {
        Instant now = Instant.now();
        ModelConfiguration row = repository.findById(ROW_ID)
                .orElse(new ModelConfiguration(ROW_ID, null, now));
        row.setActiveChatModel(model, now);
        repository.save(row);
        return current();
    }

    @SuppressWarnings("unchecked")
    private List<String> availableModels() {
        Map<String, Object> response = ollamaRestClient.get().uri("/api/tags").retrieve().body(Map.class);
        if (response == null || response.get("models") == null) {
            return List.of();
        }
        List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");
        return models.stream().map(m -> (String) m.get("name")).toList();
    }
}
