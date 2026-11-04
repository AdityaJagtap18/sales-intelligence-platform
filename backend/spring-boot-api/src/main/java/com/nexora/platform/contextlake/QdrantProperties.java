package com.nexora.platform.contextlake;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qdrant")
public record QdrantProperties(String url, String collection, int vectorSize) {
}
