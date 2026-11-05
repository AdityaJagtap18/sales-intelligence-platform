package com.nexora.platform.privacy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nexora.platform.aiinsights.AiInsightRunRepository;
import com.nexora.platform.prompts.PromptRunRepository;

/**
 * Bounds how long this app keeps a record of what was asked of the local AI
 * (AI Insights questions/answers, Prompt Playground runs) -- GDPR Art.
 * 5(1)(e) storage limitation: these tables are the app's own audit trail of
 * AI usage, and an audit trail with no retention limit becomes an
 * unbounded store of whatever personal data a user happened to type into a
 * question or prompt input. Runs once at startup and once a day thereafter.
 */
@Service
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final AiInsightRunRepository aiInsightRunRepository;
    private final PromptRunRepository promptRunRepository;
    private final long retentionDays;

    public DataRetentionService(
            AiInsightRunRepository aiInsightRunRepository,
            PromptRunRepository promptRunRepository,
            @Value("${privacy.ai-log-retention-days:90}") long retentionDays) {
        this.aiInsightRunRepository = aiInsightRunRepository;
        this.promptRunRepository = promptRunRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedRate = 86_400_000)
    public void purgeExpiredAiLogs() {
        if (retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long insightsPurged = aiInsightRunRepository.deleteByAskedAtBefore(cutoff);
        long promptsPurged = promptRunRepository.deleteByCreatedAtBefore(cutoff);
        if (insightsPurged > 0 || promptsPurged > 0) {
            log.info("Data retention: purged {} AI Insights run(s) and {} prompt run(s) older than {} days",
                    insightsPurged, promptsPurged, retentionDays);
        }
    }
}
