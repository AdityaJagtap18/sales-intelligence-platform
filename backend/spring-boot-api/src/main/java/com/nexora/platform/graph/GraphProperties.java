package com.nexora.platform.graph;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "neo4j")
public record GraphProperties(String uri, String user, String password) {
}
