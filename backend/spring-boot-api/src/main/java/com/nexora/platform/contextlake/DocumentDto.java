package com.nexora.platform.contextlake;

import java.time.Instant;

public record DocumentDto(
        Long id,
        String filename,
        String contentType,
        long sizeBytes,
        String status,
        int chunkCount,
        String errorMessage,
        Instant uploadedAt) {

    public static DocumentDto from(Document document) {
        return new DocumentDto(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getStatus(),
                document.getChunkCount(),
                document.getErrorMessage(),
                document.getUploadedAt());
    }
}
