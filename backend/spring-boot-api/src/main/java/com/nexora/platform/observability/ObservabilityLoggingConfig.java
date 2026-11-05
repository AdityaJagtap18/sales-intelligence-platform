package com.nexora.platform.observability;

import jakarta.annotation.PostConstruct;

import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@Configuration
public class ObservabilityLoggingConfig {

    @PostConstruct
    void attachInMemoryAppender() {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        InMemoryLogAppender appender = new InMemoryLogAppender();
        appender.setContext(root.getLoggerContext());
        appender.setName("IN_MEMORY_RING_BUFFER");
        appender.start();
        root.addAppender(appender);
        root.setLevel(root.getLevel() == null ? Level.INFO : root.getLevel());
    }

    /** Backs the Logs & Traces page's real HTTP traces (actuator's own httpexchanges model). */
    @Bean
    public HttpExchangeRepository httpExchangeRepository() {
        InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
        repository.setCapacity(200);
        return repository;
    }
}
