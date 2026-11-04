package com.nexora.platform.contextlake;

public record SearchResultDto(long documentId, String filename, int chunkIndex, String text, double score) {
}
