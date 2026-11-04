package com.nexora.platform.graph;

import java.util.Map;

public record GraphNodeDto(String id, String label, Map<String, Object> properties) {
}
