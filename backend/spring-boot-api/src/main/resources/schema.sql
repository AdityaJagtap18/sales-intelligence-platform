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
