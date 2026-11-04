package com.nexora.platform.reports;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportExecutionRepository extends JpaRepository<ReportExecution, Long> {
    List<ReportExecution> findAllByOrderByGeneratedAtDesc();
}
