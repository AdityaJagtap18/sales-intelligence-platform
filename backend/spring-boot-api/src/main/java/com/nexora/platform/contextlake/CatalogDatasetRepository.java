package com.nexora.platform.contextlake;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogDatasetRepository extends JpaRepository<CatalogDataset, Long> {
    List<CatalogDataset> findAllByOrderByTableNameAsc();

    Optional<CatalogDataset> findByTableName(String tableName);
}
