package com.nexora.platform.contextlake;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.nexora.platform.databricks.DatabricksSqlClient;

/**
 * Renders the known, static Source -> Bronze -> Silver -> Gold -> Catalog
 * chain (Phase 4 built this pipeline; this just makes it visible) with live
 * row counts pulled from Databricks for the three Unity Catalog layers.
 */
@Service
public class LineageService {

    public static final List<String> SALESLT_TABLES = List.of(
            "Address", "Customer", "CustomerAddress", "Product", "ProductCategory",
            "ProductDescription", "ProductModel", "ProductModelProductDescription",
            "SalesOrderDetail", "SalesOrderHeader");

    private final DatabricksSqlClient sqlClient;
    private final CatalogDatasetRepository catalogRepository;

    public LineageService(DatabricksSqlClient sqlClient, CatalogDatasetRepository catalogRepository) {
        this.sqlClient = sqlClient;
        this.catalogRepository = catalogRepository;
    }

    public LineageDto lineageFor(String table) {
        String canonical = SALESLT_TABLES.stream()
                .filter(t -> t.equalsIgnoreCase(table))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Unknown SalesLT table: " + table));
        String lower = canonical.toLowerCase();

        String unionSql = "SELECT 'bronze' AS layer, COUNT(*) AS cnt FROM workspace.bronze." + canonical
                + " UNION ALL SELECT 'silver', COUNT(*) FROM workspace.silver." + canonical
                + " UNION ALL SELECT 'gold', COUNT(*) FROM workspace.gold." + canonical;

        long bronzeCount = 0, silverCount = 0, goldCount = 0;
        for (List<String> row : sqlClient.executeQuery(unionSql)) {
            long count = Long.parseLong(row.get(1));
            switch (row.get(0)) {
                case "bronze" -> bronzeCount = count;
                case "silver" -> silverCount = count;
                case "gold" -> goldCount = count;
            }
        }

        var catalogEntry = catalogRepository.findByTableName("gold." + lower);
        String catalogDetail = catalogEntry
                .map(c -> "Cataloged, last refreshed " + c.getLastRefreshedAt())
                .orElse("Not yet synced — POST /catalog/refresh");

        List<LineageStageDto> stages = List.of(
                new LineageStageDto("Source", "SQL Server (local)", "SalesLT." + canonical, null,
                        "Extracted via backend/python-worker/extract_to_databricks.py (Phase 4)"),
                new LineageStageDto("Bronze", "Databricks / Unity Catalog", "workspace.bronze." + canonical,
                        bronzeCount, "Raw ingest, unmodified"),
                new LineageStageDto("Silver", "Databricks / Unity Catalog", "workspace.silver." + canonical,
                        silverCount, "Date normalization + data quality checks"),
                new LineageStageDto("Gold", "Databricks / Unity Catalog", "workspace.gold." + canonical,
                        goldCount, "PascalCase -> underscore column renaming"),
                new LineageStageDto("Catalog", "Context Lake (Postgres)", "gold." + lower,
                        catalogEntry.map(CatalogDataset::getRowCount).orElse(null), catalogDetail));

        return new LineageDto(canonical, stages);
    }
}
