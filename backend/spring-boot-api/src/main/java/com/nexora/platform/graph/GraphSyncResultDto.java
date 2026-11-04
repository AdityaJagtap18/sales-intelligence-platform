package com.nexora.platform.graph;

public record GraphSyncResultDto(
        int customers,
        int addresses,
        int products,
        int categories,
        int orders,
        int orderLines,
        int documents,
        int mentions) {
}
