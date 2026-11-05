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

-- Phase 3, Group 2: pipeline registry. One definition per Databricks job this
-- app can trigger; pipeline_run.pipeline_definition_id links run history back
-- to its definition (nullable: rows created before this column existed, or a
-- run triggered before a matching definition was registered, have none).
CREATE TABLE IF NOT EXISTS pipeline_definition (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    pipeline_type VARCHAR(64) NOT NULL,
    source_name VARCHAR(256) NOT NULL,
    destination_name VARCHAR(256) NOT NULL,
    databricks_job_id BIGINT NOT NULL,
    data_source_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE pipeline_run ADD COLUMN IF NOT EXISTS pipeline_definition_id BIGINT;

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

-- Phase 8: Prompt Library & AI. A prompt has one or more versions (its
-- template text); each playground run records which version it used.
CREATE TABLE IF NOT EXISTS prompt (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS prompt_version (
    id BIGSERIAL PRIMARY KEY,
    prompt_id BIGINT NOT NULL REFERENCES prompt (id),
    version_number INTEGER NOT NULL,
    template TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (prompt_id, version_number)
);

CREATE TABLE IF NOT EXISTS prompt_run (
    id BIGSERIAL PRIMARY KEY,
    prompt_version_id BIGINT NOT NULL REFERENCES prompt_version (id),
    rendered_prompt TEXT NOT NULL,
    output TEXT,
    model VARCHAR(128) NOT NULL,
    rating VARCHAR(16) NOT NULL,
    latency_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

-- Phase 3, Group 1: Forecasting runs (Analytics > Forecasting). A run is
-- COMPLETED with a method/metrics/result, or INSUFFICIENT_DATA with a reason
-- — never a fabricated forecast when the Gold-layer history is too thin.
CREATE TABLE IF NOT EXISTS forecast_run (
    id BIGSERIAL PRIMARY KEY,
    metric VARCHAR(32) NOT NULL,
    horizon_periods INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    method VARCHAR(64),
    mae DOUBLE PRECISION,
    mape DOUBLE PRECISION,
    reason TEXT,
    result_json TEXT,
    requested_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

-- Phase 3, Group 1: persistent acknowledge/resolve state for anomalies
-- surfaced by AnalyticsService.anomalies() (the same source of truth used by
-- Executive Overview and the dedicated Anomaly Detection page).
CREATE TABLE IF NOT EXISTS anomaly_acknowledgement (
    id BIGSERIAL PRIMARY KEY,
    anomaly_key VARCHAR(256) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    note VARCHAR(1024),
    acknowledged_by VARCHAR(128) NOT NULL,
    acknowledged_at TIMESTAMPTZ NOT NULL
);

-- Phase 3, Group 2: registered source systems (Data Platform > Data Sources).
-- Secrets live only in connection_config_json's encrypted-at-rest columns are
-- out of scope for this sample app, but the DTO layer never returns the raw
-- config back to callers — only a masked summary.
CREATE TABLE IF NOT EXISTS data_source (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    connection_config_json TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_test_status VARCHAR(32),
    last_test_message TEXT,
    last_tested_at TIMESTAMPTZ,
    last_successful_ingestion_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

-- Phase 3, Group 3: generated alerts (Observability > Alerts). Rows are
-- upserted by AlertEvaluationService from real conditions elsewhere in the
-- app (failed pipeline runs, failing data-quality checks, high-confidence
-- anomalies, DOWN system-health components) — never inserted by hand or
-- fabricated.
CREATE TABLE IF NOT EXISTS alert (
    id BIGSERIAL PRIMARY KEY,
    source_module VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    title VARCHAR(256) NOT NULL,
    message TEXT NOT NULL,
    dedupe_key VARCHAR(256) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    acknowledged_by VARCHAR(128)
);

-- Phase 3, Group 4: AI Insights question/answer history
-- (Intelligence > AI Insights). answer/citation_count are null/0 for any
-- non-ANSWERED status (NO_SOURCES, INSUFFICIENT_CONTEXT, GENERATION_FAILED)
-- -- there is never a stored answer without the real citations behind it.
CREATE TABLE IF NOT EXISTS ai_insight_run (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(1024) NOT NULL,
    answer TEXT,
    status VARCHAR(32) NOT NULL,
    citation_count INTEGER NOT NULL,
    asked_at TIMESTAMPTZ NOT NULL
);

-- Phase 3, Group 1: Report generation history (Overview > Reports).
CREATE TABLE IF NOT EXISTS report_execution (
    id BIGSERIAL PRIMARY KEY,
    report_key VARCHAR(64) NOT NULL,
    report_name VARCHAR(256) NOT NULL,
    filters_json TEXT,
    status VARCHAR(32) NOT NULL,
    row_count INTEGER,
    error_message TEXT,
    requested_by VARCHAR(128) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL
);
