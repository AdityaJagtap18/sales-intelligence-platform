package com.nexora.platform.forecasting;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastRunRepository extends JpaRepository<ForecastRun, Long> {
    List<ForecastRun> findAllByOrderByCreatedAtDesc();
}
