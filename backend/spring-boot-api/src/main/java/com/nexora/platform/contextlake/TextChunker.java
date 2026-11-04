package com.nexora.platform.contextlake;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/** Fixed-size chunking — enough for semantic search over short documents without an NLP dependency. */
@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 1000;

    public List<String> chunk(String text) {
        String normalized = text.strip();
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < normalized.length(); start += CHUNK_SIZE) {
            String chunk = normalized.substring(start, Math.min(start + CHUNK_SIZE, normalized.length())).strip();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }
}
