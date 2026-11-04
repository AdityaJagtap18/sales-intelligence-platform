package com.nexora.platform.contextlake;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_dataset")
public class CatalogDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, unique = true)
    private String tableName;

    @Column(name = "row_count", nullable = false)
    private long rowCount;

    /** JSON array of {"name": ..., "dataType": ...} — see ColumnInfo. */
    @Column(name = "columns_json", nullable = false)
    private String columnsJson;

    @Column(name = "last_refreshed_at", nullable = false)
    private Instant lastRefreshedAt;

    protected CatalogDataset() {
    }

    public CatalogDataset(String tableName, long rowCount, String columnsJson, Instant lastRefreshedAt) {
        this.tableName = tableName;
        this.rowCount = rowCount;
        this.columnsJson = columnsJson;
        this.lastRefreshedAt = lastRefreshedAt;
    }

    public void update(long rowCount, String columnsJson, Instant lastRefreshedAt) {
        this.rowCount = rowCount;
        this.columnsJson = columnsJson;
        this.lastRefreshedAt = lastRefreshedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTableName() {
        return tableName;
    }

    public long getRowCount() {
        return rowCount;
    }

    public String getColumnsJson() {
        return columnsJson;
    }

    public Instant getLastRefreshedAt() {
        return lastRefreshedAt;
    }
}
