package com.nexora.platform.alerts;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findAllByOrderByCreatedAtDesc();

    Optional<Alert> findByDedupeKey(String dedupeKey);

    List<Alert> findAllByStatusNot(String status);
}
