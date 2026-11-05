package com.nexora.platform.observability;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Fixed-capacity in-memory store of this process's own recent log events,
 * fed by {@link InMemoryLogAppender}. There is no Loki/Tempo/Jaeger in
 * docker-compose.yml (see docs/implementation/known-limitations.md), so this
 * is real application-log data rather than a fabricated log stream — scoped
 * to this one process, not a cross-service aggregator.
 */
final class LogRingBuffer {

    private static final int CAPACITY = 500;
    private static final Deque<LogEntryDto> BUFFER = new ArrayDeque<>(CAPACITY);

    private LogRingBuffer() {
    }

    static synchronized void add(LogEntryDto entry) {
        BUFFER.addFirst(entry);
        while (BUFFER.size() > CAPACITY) {
            BUFFER.removeLast();
        }
    }

    static synchronized List<LogEntryDto> recent() {
        return List.copyOf(BUFFER);
    }
}
