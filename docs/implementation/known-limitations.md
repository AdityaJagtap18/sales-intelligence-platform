# Phase 3 — Known Limitations

Genuine external-service and sample-data limitations, documented rather than hidden behind
fabricated data.

## Sample data (AdventureWorksLT via Databricks Free Edition)

- **Single Order_Date across the sample.** Revenue Growth (Sales Dashboard) and period-over-period
  trends cannot be computed and are shown as "Not available" rather than 0% or a fabricated figure.
- **Forecasting requires >= 3 distinct calendar periods** to fit a trend; the sample normally has 1,
  so `/api/analytics/forecasting/generate` correctly returns `INSUFFICIENT_DATA` with the real
  reason and the minimum requirement, per the spec's "never present arbitrary generated values as
  real forecasts" rule.
- **No SalesTerritory table.** "Territory" throughout Sales Dashboard, Customer Analytics and
  Reports is Address.State_Province (via Customer -> CustomerAddress -> Address) — the closest real
  geographic dimension AdventureWorksLT has, not a fabricated field.
- **"Customer lifetime value"** is lifetime revenue within this dataset, not a projected/discounted
  CLV model — the UI and docs label it this way rather than implying a forecasting model that
  doesn't exist.

## Infrastructure not yet wired (relevant to later groups)

- **No log/trace backend** (Loki/Tempo/Jaeger/similar) is in `docker-compose.yml` — Logs & Traces
  (Group 3) will need to ship an honest "not configured" state rather than fake traces.
- **No authentication/authorization exists anywhere in the codebase today.** User Management
  (Group 5) requires a net-new minimal Spring Security setup — not a retrofit of hidden existing
  logic, since none exists.

## Data platform (Group 2)

- **Only two source types have a real connection to test.** This app holds exactly one Databricks
  SQL Warehouse connection and one Postgres connection; `DataSourceService#testConnection` pings
  those actual connections for sources of those types. Registering a source with type CSV/JSON/
  Parquet reports `NOT_APPLICABLE`, not a fabricated "connected" status, since those are file
  references rather than live connections this app holds credentials for.
- **Connection config is masked, never returned raw.** `DataSourceDto` redacts any field whose key
  contains password/token/secret/key before it reaches the frontend, so there is no API response
  that can leak a stored credential even to an authenticated caller — relevant ahead of Group 5's
  user management, where callers won't all be the same trust level.
- **Only one real Databricks job exists in this environment** (`DATABRICKS_JOB_ID`). The pipeline
  registry auto-seeds exactly one `PipelineDefinition` from it at startup; the API supports
  registering further definitions, but there is no second job in this architecture for one to
  point at, so that path is untested against a real run.
- **Data Quality has no standalone check-execution step.** Checks run inside the existing
  Bronze-to-Silver Databricks notebook and land in `workspace.nexora.data_quality_report` (the same
  table `DataQualityMetricsPoller` already polls into Prometheus). "Execute Checks" re-triggers that
  whole pipeline rather than running checks in isolation — there is no narrower real action to wire
  it to.

## Forecasting implementation choice

The spec's forecasting section says to "prefer a Python service or worker if the existing
architecture supports it." `backend/python-worker` is a batch ingestion script invoked manually
during Phase 4 setup (`extract_to_databricks.py`, `deploy_notebooks.py`) — it is not a running
service Spring Boot could call at request time, and no HTTP server exists in it. Adding one would
be new infrastructure for a single feature when the forecasting method needed (least-squares linear
regression) has no dependency that Java lacks. Forecasting is implemented directly in
`ForecastingService` (Spring Boot) instead, consistent with "use an appropriate forecasting method
based on data availability."
