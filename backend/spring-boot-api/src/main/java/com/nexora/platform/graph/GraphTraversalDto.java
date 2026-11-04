package com.nexora.platform.graph;

import java.util.List;

public record GraphTraversalDto(List<GraphNodeDto> nodes, List<GraphEdgeDto> edges) {
}
