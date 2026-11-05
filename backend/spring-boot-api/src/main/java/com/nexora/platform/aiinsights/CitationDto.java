package com.nexora.platform.aiinsights;

import java.util.List;

public record CitationDto(long documentId, String filename, int chunkIndex, double score, String snippet,
        List<RelatedEntityDto> relatedEntities) {
}
