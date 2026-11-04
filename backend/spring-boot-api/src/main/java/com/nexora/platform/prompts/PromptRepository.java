package com.nexora.platform.prompts;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
    List<Prompt> findAllByOrderByCreatedAtDesc();
}
