# Phase 3 — Module Implementation Status

Updated after each implementation group. Status values: `DONE`, `IN PROGRESS`, `NOT STARTED`.

## Group 1 — Core business modules (DONE)

| Page | Route | Backend | Frontend | Integration | Tests | Known limitations |
|---|---|---|---|---|---|---|
| Sales Dashboard | /overview/sales-dashboard | `sales` package: `SalesDashboardController/Service`, `/api/sales/*` | `SalesDashboardComponent` + `SalesApiService` | Databricks Gold layer (salesorderheader/detail, customer, product, productcategory, address, customeraddress) | Backend compiles + existing suite green; frontend build + existing Karma suite green (no new Karma specs added for this page yet) | Revenue growth is "Not available" — sample has one Order_Date, no prior period to compare. Territory = Address.State_Province (no SalesTerritory table in AdventureWorksLT). |
| Reports | /overview/reports | `reports` package: `ReportController/Service`, `report_execution` table | `ReportsComponent` + `ReportsApiService` | Composes SalesDashboardService, CustomerAnalyticsService, AnalyticsService, PipelineRunService, DatabricksSqlClient (data quality) | Same as above | Data Quality report reads `workspace.nexora.data_quality_report` directly (Group 2 builds the dedicated quality center on the same source). |
| Customer Analytics | /analytics/customers | `analytics` package additions: `CustomerAnalyticsController/Service` | `CustomerAnalyticsComponent` + `CustomerAnalyticsApiService` | Databricks Gold layer (customer, salesorderheader, address, customeraddress) | Same as above | "Lifetime value" = lifetime revenue within the sample dataset, not a projected/discounted CLV model — labeled as such in the UI. |
| Forecasting | /analytics/forecasting | `forecasting` package: `ForecastingController/Service`, `forecast_run` table | `ForecastingComponent` + `ForecastingApiService` | Databricks Gold layer (salesorderheader, grouped by Order_Date); linear regression implemented directly in Spring Boot (no ML lib/Python worker service added) | Same as above | AdventureWorksLT sample has 1 distinct Order_Date; forecasting requires >= 3, so most runs correctly report `INSUFFICIENT_DATA` with the real reason rather than a fabricated forecast. |
| Anomaly Detection | /analytics/anomaly-detection | `AnomalyDetectionController` reuses `AnalyticsService#anomalies()` (same method Executive Overview calls) + new `anomaly_acknowledgement` table | `AnomalyDetectionComponent` + extended `AnalyticsApiService` | Same source of truth as Executive Overview | Same as above | Acknowledge/resolve persists by a derived key (`metric|order`), not a stored detection-event id, since anomalies are computed on demand rather than stored as discrete events. |

## Group 2 — Data platform (NOT STARTED)
## Group 3 — Observability (NOT STARTED)
## Group 4 — Intelligence (NOT STARTED)
## Group 5 — Administration (NOT STARTED)
