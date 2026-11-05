package com.nexora.platform.aiinsights;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiInsightRunRepository extends JpaRepository<AiInsightRun, Long> {
    List<AiInsightRun> findAllByOrderByAskedAtDesc();
}
