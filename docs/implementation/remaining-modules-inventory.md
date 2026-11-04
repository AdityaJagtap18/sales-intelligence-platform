# Phase 3 — Remaining Modules Implementation Inventory

Repository inspection performed before implementation (per task instructions).

## 1. Existing application shape

- **Frontend**: Angular 20 standalone components under `frontend/angular-app/src/app`.
  - `core/layout/shell` — app shell (sidebar nav + header). `core/nav` — nav config.
  - `core/services/*-api.service.ts` — one HttpClient-based service per backend domain
    (`analytics-api`, `catalog-api`, `context-lake-api`, `graph-api`, `lineage-api`,
    `pipeline-api`, `prompt-api`). Base URL convention: `${environment.apiUrl}` + path
    matching the Spring controller `@RequestMapping` (controllers are rooted under the
    `/api` servlet context-path, so e.g. `AnalyticsController` at `/analytics` is called
    as `/api/analytics/...`).
  - `shared/components/*` — reusable: `kpi-card`, `panel`, `badge`, `icon`, `filter-bar`,
    `data-table`, charts (`area-line-chart`, `donut-chart`, `horizontal-bar-chart`).
    No modal/drawer, pagination, skeleton, or empty-state component exists yet — Phase 3
    adds these as shared components rather than ad hoc per page.
  - `app.routes.ts` — 18 routes use `placeholder(...)` → generic `PlaceholderPageComponent`.
    9 routes already load real feature components (Executive Overview, Sales Explorer,
    Product Analytics, Pipeline Runs, Context Lake, Catalog, Lineage, Knowledge Graph,
    Graph Explorer, Prompt Library, Prompt Playground).

- **Backend**: Spring Boot 4 (`com.nexora.platform`), context-path `/api`, packages:
  `analytics`, `contextlake`, `databricks`, `graph`, `observability`, `pipeline`, `prompts`.
  - `DatabricksSqlClient` — generic `executeQuery(sql) -> List<List<String>>` against the
    SQL Warehouse; this is the only read path into `workspace.gold.*`. Reused by
    `AnalyticsService`, `CatalogSyncService`.
  - `DatabricksJobsClient` — `runNow(jobId)`, `getRunStatus(runId)` against Databricks Jobs
    API. Used only by `PipelineRunService` today, for a single hardcoded job id
    (`databricks.job-id`).
  - `AnalyticsService`/`AnalyticsController` (`/api/analytics/*`) — sales summary, revenue
    trend (with z-score anomaly flag, threshold 1.3), revenue by category/top customers,
    anomalies list, orders, product performance. All query `workspace.gold.salesorderheader
    /salesorderdetail/customer/product/productcategory`. This is the real anomaly-detection
    engine the spec requires reusing for the dedicated Anomaly Detection page.
  - `PipelineRunService`/`PipelineController` (`/api/pipelines/*`) — trigger/list/refresh
    runs against one fixed Databricks job id, persisted to Postgres `pipeline_run` table.
  - `CatalogSyncService`/`CatalogController`, `LineageService`/`LineageController`,
    `DocumentIngestionService`/`DocumentController` (contextlake) — dataset catalog sync,
    lineage, MinIO/Qdrant document ingestion.
  - `GraphSyncService`/`GraphQueryService`/`GraphController` — Neo4j sync + traversal.
  - `PromptService`/`OllamaChatClient`/`PromptController` — prompt CRUD, versions, Ollama
    playground runs, ratings; Postgres `prompt`/`prompt_version`/`prompt_run`.
  - `DataQualityMetricsPoller` (observability) — polls `workspace.nexora.data_quality_report`
    into a Micrometer gauge only; no REST surface, no persisted history.
  - **No Spring Security dependency, no authentication/authorization, no user entity
    anywhere in the codebase.** Actuator exposes `health,info,prometheus` unauthenticated.
  - Postgres schema (`schema.sql`, `ddl-auto=validate`): `pipeline_run`, `catalog_dataset`,
    `document`, `prompt`, `prompt_version`, `prompt_run`. No tables yet for reports,
    data sources, data-quality runs, forecasts, alerts, settings, integrations, users.

- **Gold-layer schema available for BI**: `salesorderheader`, `salesorderdetail`,
  `customer`, `product`, `productcategory` (AdventureWorksLT sample — single-day order
  date, ~32 orders). This caps what Sales Dashboard/Customer Analytics/Forecasting can
  honestly show: no real time series longer than order-sequence order, no territory
  column observed yet (needs confirming against `customer`/`salesorderheader` columns
  before building territory filters — will fall back to an honest "not available" state
  if absent).

## 2. Reuse plan (no duplicate engines)

| New page | Reuses | New backend needed |
|---|---|---|
| Sales Dashboard | `AnalyticsService` queries | filter params (date/category/territory), CSV export, drill-down endpoint |
| Reports | — | new `report` persistence + generation service wrapping existing analytics queries |
| Customer Analytics | `AnalyticsService` customer query pattern | new customer-segmentation queries against `workspace.gold.customer/salesorderheader` |
| Forecasting | `AnalyticsService` order-value series | new simple statistical forecaster (moving-average/linear trend; no ML lib in pom.xml) + Postgres `forecast_run` table |
| Anomaly Detection | `AnalyticsService.anomalies()`/`revenueTrend()` directly (same source of truth as Executive Overview) | optional `anomaly_acknowledgement` persistence for ack/resolve |
| Data Sources | — | new `data_source` entity/CRUD, connection test per type |
| Data Pipelines | `PipelineRunService`/`DatabricksJobsClient`, extended to a registry instead of one fixed job id | `pipeline_definition` entity; reuse `pipeline_run` for history |
| Data Quality | `DataQualityMetricsPoller`'s query target (`workspace.nexora.data_quality_report`) via `DatabricksSqlClient`, made a real REST+persisted feature | `data_quality_run` entity |
| System Health | actuator `/health` + direct ping of Postgres/Neo4j/Qdrant/Ollama/Databricks | new `ObservabilityController` aggregating checks |
| Pipeline Monitoring | same `pipeline_run` table as Data Pipelines | — |
| Logs & Traces | — (no OTel collector/log backend wired in this stack) | integration-boundary endpoint returning "not configured" + structure for when it is |
| Alerts | — | new shared `alert` entity/service, fed by data-quality and anomaly findings |
| Metrics | Micrometer/Prometheus already exposed at `/api/actuator/prometheus` | thin REST wrapper parsing selected series for the UI |
| AI Insights | `OllamaChatClient`, `QdrantClient`/`DocumentSearchService`, `GraphQueryService`, `AnalyticsService` | new orchestration `AiInsightsService` composing the above with citations |
| Settings | — | new `app_setting` entity (single-row/key-value) |
| Integrations | same live checks as System Health, one per external system | `IntegrationsController` |
| Model Configuration | `OllamaChatClient`/`ollama.*` properties | Ollama `/api/tags` model discovery, `model_run` history table |
| User Management | **none — no auth exists** | minimal Spring Security (form/basic + BCrypt) + `app_user`/`role` tables; smallest viable RBAC, documented as a new addition, not a retrofit of something larger |

## 3. Constraints acknowledged

- AdventureWorksLT sample data limits real forecasting/trend depth — handled by honest
  "insufficient data" states per the spec, not fabricated series.
- No log/trace backend (Loki/Tempo/Jaeger) in `docker-compose.yml` — Logs & Traces ships
  the integration boundary and a clear "not configured" state, documented in
  `known-limitations.md`.
- No existing auth — User Management gets a minimal, real Spring Security setup; this is
  net-new, not a reuse of hidden existing logic.

Implementation proceeds in the spec's 5 groups, starting with Group 1.
