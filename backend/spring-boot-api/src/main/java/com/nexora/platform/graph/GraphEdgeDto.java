package com.nexora.platform.graph;

import java.util.Map;

public record GraphEdgeDto(String id, String type, String startNodeId, String endNodeId, Map<String, Object> properties) {
}
