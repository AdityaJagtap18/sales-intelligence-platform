package com.nexora.platform.prompts;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRunRepository extends JpaRepository<PromptRun, Long> {
    List<PromptRun> findAllByPromptVersionIdInOrderByCreatedAtDesc(List<Long> promptVersionIds);
}
