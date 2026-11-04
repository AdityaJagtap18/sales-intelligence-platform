package com.nexora.platform.contextlake;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(String url, String embeddingModel) {
}
