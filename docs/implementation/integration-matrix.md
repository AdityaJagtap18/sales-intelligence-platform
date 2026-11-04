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

Still to implement in later groups: Data Sources -> Data Pipelines, Data Pipelines -> Pipeline Monitoring, Data Pipelines -> Data Quality, Data Quality -> Alerts, Anomaly Detection -> Alerts, AI Insights -> Context Lake/Knowledge Graph, Model Configuration -> AI Insights, System Health -> Integrations.
