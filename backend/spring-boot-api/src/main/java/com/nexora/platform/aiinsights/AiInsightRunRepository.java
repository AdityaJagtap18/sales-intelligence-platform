package com.nexora.platform.aiinsights;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiInsightRunRepository extends JpaRepository<AiInsightRun, Long> {
    List<AiInsightRun> findAllByOrderByAskedAtDesc();

    /** Used by the scheduled data-retention sweep (GDPR Art. 5(1)(e) storage limitation). */
    long deleteByAskedAtBefore(Instant cutoff);
}
