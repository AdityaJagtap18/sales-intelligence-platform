package com.nexora.platform.observability;

import java.util.List;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Logs & Traces (/observability/logs-traces). There is no Loki/Tempo/Jaeger
 * in docker-compose.yml (docs/implementation/known-limitations.md), so this
 * surfaces this process's own real log events ({@link LogRingBuffer}) and
 * real recent HTTP exchanges (Spring Boot Actuator's own httpexchanges
 * model) rather than a fabricated distributed-tracing view.
 */
@RestController
@RequestMapping("/observability")
public class LogsTracesController {

    private final HttpExchangeRepository exchangeRepository;

    public LogsTracesController(HttpExchangeRepository exchangeRepository) {
        this.exchangeRepository = exchangeRepository;
    }

    @GetMapping("/logs")
    public List<LogEntryDto> logs() {
        return LogRingBuffer.recent();
    }

    @GetMapping("/traces")
    public List<TraceDto> traces() {
        List<HttpExchange> exchanges = exchangeRepository.findAll();
        return exchanges.stream()
                .map(e -> new TraceDto(
                        e.getTimestamp(),
                        e.getRequest().getMethod(),
                        e.getRequest().getUri().getPath(),
                        e.getResponse() == null ? null : e.getResponse().getStatus(),
                        e.getTimeTaken() == null ? null : e.getTimeTaken().toMillis()))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .toList();
    }
}
