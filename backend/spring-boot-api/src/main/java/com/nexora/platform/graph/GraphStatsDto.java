package com.nexora.platform.graph;

import java.util.Map;

public record GraphStatsDto(long nodeCount, long relationshipCount, Map<String, Long> nodeCountsByLabel) {
}
