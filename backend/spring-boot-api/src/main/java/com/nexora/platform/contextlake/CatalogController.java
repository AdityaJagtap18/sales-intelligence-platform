package com.nexora.platform.contextlake;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogDatasetRepository repository;
    private final CatalogSyncService syncService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CatalogController(CatalogDatasetRepository repository, CatalogSyncService syncService) {
        this.repository = repository;
        this.syncService = syncService;
    }

    @GetMapping("/datasets")
    public List<CatalogDatasetDto> datasets() {
        return repository.findAllByOrderByTableNameAsc().stream().map(this::toDto).toList();
    }

    @PostMapping("/refresh")
    public Map<String, Integer> refresh() {
        return Map.of("synced", syncService.sync());
    }

    private CatalogDatasetDto toDto(CatalogDataset dataset) {
        List<ColumnInfo> columns;
        try {
            columns = objectMapper.readValue(dataset.getColumnsJson(), new TypeReference<List<ColumnInfo>>() {
            });
        } catch (Exception e) {
            columns = List.of();
        }
        return new CatalogDatasetDto(dataset.getId(), dataset.getTableName(), dataset.getRowCount(), columns,
                dataset.getLastRefreshedAt());
    }
}
