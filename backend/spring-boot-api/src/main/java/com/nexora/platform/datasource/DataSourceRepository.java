package com.nexora.platform.datasource;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
    List<DataSource> findAllByOrderByNameAsc();
}
