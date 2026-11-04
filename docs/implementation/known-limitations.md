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

## Forecasting implementation choice

The spec's forecasting section says to "prefer a Python service or worker if the existing
architecture supports it." `backend/python-worker` is a batch ingestion script invoked manually
during Phase 4 setup (`extract_to_databricks.py`, `deploy_notebooks.py`) — it is not a running
service Spring Boot could call at request time, and no HTTP server exists in it. Adding one would
be new infrastructure for a single feature when the forecasting method needed (least-squares linear
regression) has no dependency that Java lacks. Forecasting is implemented directly in
`ForecastingService` (Spring Boot) instead, consistent with "use an appropriate forecasting method
based on data availability."
