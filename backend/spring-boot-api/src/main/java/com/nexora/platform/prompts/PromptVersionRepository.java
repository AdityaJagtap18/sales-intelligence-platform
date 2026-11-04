package com.nexora.platform.prompts;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {
    List<PromptVersion> findAllByPromptIdOrderByVersionNumberDesc(Long promptId);

    Optional<PromptVersion> findFirstByPromptIdOrderByVersionNumberDesc(Long promptId);
}
