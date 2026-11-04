CREATE TABLE IF NOT EXISTS pipeline_run (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    databricks_run_id BIGINT NOT NULL UNIQUE,
    lifecycle_state VARCHAR(32) NOT NULL,
    result_state VARCHAR(32),
    state_message TEXT,
    triggered_by VARCHAR(128) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

-- Phase 6: Context Lake dataset catalog (workspace.gold.* metadata, synced
-- from Unity Catalog — see CatalogSyncService) and document registry
-- (uploaded files stored in MinIO, chunked/embedded into Qdrant).
CREATE TABLE IF NOT EXISTS catalog_dataset (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(256) NOT NULL UNIQUE,
    row_count BIGINT NOT NULL,
    columns_json TEXT NOT NULL,
    last_refreshed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS document (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(512) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    uploaded_at TIMESTAMPTZ NOT NULL
);
