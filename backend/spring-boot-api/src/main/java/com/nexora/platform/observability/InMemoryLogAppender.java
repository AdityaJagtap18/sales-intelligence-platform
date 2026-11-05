package com.nexora.platform.observability;

import java.time.Instant;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Logback appender that mirrors every log event into {@link LogRingBuffer}
 * so the Logs & Traces page can show this process's real log output.
 * Registered programmatically by {@link ObservabilityLoggingConfig}.
 */
public class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {
        LogRingBuffer.add(new LogEntryDto(
                Instant.ofEpochMilli(event.getTimeStamp()),
                event.getLevel().toString(),
                event.getLoggerName(),
                event.getThreadName(),
                event.getFormattedMessage()));
    }
}
