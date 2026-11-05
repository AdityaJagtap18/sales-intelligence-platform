# Phase 3 — Integration Matrix

How each module gets its data, and the cross-module links the spec requires.

## Data sources per module (Group 1)

| Module | Data source | Path |
|---|---|---|
| Sales Dashboard | Databricks `workspace.gold.*` | Angular -> `/api/sales/*` -> `SalesDashboardService` -> `DatabricksSqlClient` |
| Reports | Composed from other services (no direct Databricks calls except the Data Quality report) | Angular -> `/api/reports/*` -> `ReportService` -> `SalesDashboardService` / `CustomerAnalyticsService` / `AnalyticsService` / `PipelineRunService` / `DatabricksSqlClient`; persisted to Postgres `report_execution` |
| Customer Analytics | Databricks `workspace.gold.*` | Angular -> `/api/analytics/customers/*` -> `CustomerAnalyticsService` -> `DatabricksSqlClient` |
| Forecasting | Databricks `workspace.gold.salesorderheader` | Angular -> `/api/analytics/forecasting/*` -> `ForecastingService` -> `DatabricksSqlClient`; persisted to Postgres `forecast_run` |
| Anomaly Detection | Same as Executive Overview | Angular -> `/api/analytics/anomalies` -> `AnomalyDetectionController` -> `AnalyticsService#anomalies()` -> `DatabricksSqlClient`; ack/resolve persisted to Postgres `anomaly_acknowledgement` |

## Cross-module workflows implemented (Group 1)

1. **Sales Dashboard -> Customer Analytics**: clicking a Top Customers donut slice navigates to `/analytics/customers?search=<company>`, which pre-fills the search filter.
2. **Reports -> Sales Dashboard**: the Reports page's result panel has an "Open in Sales Dashboard" action.
3. **Executive Overview <-> Anomaly Detection**: identical `AnalyticsService#anomalies()` computation; the dedicated page adds filtering and persistent acknowledge/resolve on top, not a second detector.

## Group 2 additions

| Module | Data source | Path |
|---|---|---|
| Data Sources | Shared Databricks SQL Warehouse / this app's own Postgres pool | Angular -> `/api/sources/*` -> `DataSourceService` -> `DatabricksSqlClient` or `javax.sql.DataSource` |
| Data Pipelines | Databricks Jobs API | Angular -> `/api/pipelines/definitions/*` -> `PipelineDefinitionService` -> `PipelineRunService` -> `DatabricksJobsClient`; history in Postgres `pipeline_run` |
| Data Quality | Databricks `workspace.nexora.data_quality_report` | Angular -> `/api/data-quality/*` -> `DataQualityService` -> `DatabricksSqlClient` |

4. **Data Sources -> Data Pipelines**: a source's detail drawer shows `GET /api/sources/{id}/pipelines` (pipeline definitions referencing that `data_source_id`).
5. **Data Pipelines -> Pipeline Monitoring**: a pipeline's detail drawer links to `/observability/pipeline-monitoring` after triggering a run (built in Group 3, same `pipeline_run` records).
6. **Data Quality -> Data Pipelines**: "Execute checks" on the Data Quality page triggers the same pipeline definition Data Pipelines manages, then navigates to Pipeline Monitoring.

Still to implement in later groups: Data Pipelines -> Data Quality (direct link), Data Quality -> Alerts, Anomaly Detection -> Alerts, AI Insights -> Context Lake/Knowledge Graph, Model Configuration -> AI Insights, System Health -> Integrations.
