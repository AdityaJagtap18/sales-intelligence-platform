package com.nexora.platform.analytics;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyAcknowledgementRepository extends JpaRepository<AnomalyAcknowledgement, Long> {
    Optional<AnomalyAcknowledgement> findByAnomalyKey(String anomalyKey);
}
