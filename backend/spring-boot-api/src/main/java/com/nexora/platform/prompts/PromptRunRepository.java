package com.nexora.platform.prompts;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRunRepository extends JpaRepository<PromptRun, Long> {
    List<PromptRun> findAllByPromptVersionIdInOrderByCreatedAtDesc(List<Long> promptVersionIds);

    /** Used by the scheduled data-retention sweep (GDPR Art. 5(1)(e) storage limitation). */
    long deleteByCreatedAtBefore(Instant cutoff);
}
