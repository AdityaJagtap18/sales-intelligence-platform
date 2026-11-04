# Implementation Roadmap (Phase-Gated)

Status legend: ✅ done · ▶ next · ⏳ not started · ⏸ blocked on user action

**Architecture note:** as of ADR-005, this is a hybrid architecture — Databricks Free
Edition is the one cloud-hosted component (data engineering execution: Bronze/Silver/Gold,
Delta Lake, Unity Catalog, Jobs/Lakeflow, SQL Warehouse). Everything else stays local. This
changed the scope of Phases 3–5 from the original all-local plan; see ADR-005 for why.

| Phase | Scope | Status |
|---|---|---|
| 1 | Project discovery & repository assessment | ✅ done |
| 2 | Application foundation & design system (Angular shell, Spring Boot skeleton, NEXORA design tokens, nav, Executive Overview shell) | ✅ done — `frontend/angular-app`, `backend/spring-boot-api` |
| 3 | Local infrastructure + Databricks connectivity (Docker Compose: Postgres, Neo4j, Qdrant, Ollama, OTel/Prometheus/Grafana; validate Databricks Free Edition access) | ✅ done — see `docs/discovery/databricks-free-edition-assessment.md` |
| 4 | Databricks notebook adaptation and pipeline implementation (Bronze/Silver/Gold on Databricks, local source extraction/upload) | ✅ done — `backend/python-worker`, `databricks/notebooks` |
| 5 | Databricks orchestration integration (Jobs/Lakeflow) and local observability | ✅ done — `backend/spring-boot-api` (pipeline package), `frontend/angular-app` (Pipeline Runs page), Grafana dashboard |
| 6 | Context Lake (dataset registry, metadata catalog, document ingestion, semantic search, lineage — MinIO for local documents only, not analytical data) | ✅ done — `backend/spring-boot-api` (contextlake package), `frontend/angular-app` (Catalog/Context Lake/Lineage pages) |
| 7 | Knowledge Graph (Neo4j schema, sync, Graph Explorer UI) | ✅ done — `backend/spring-boot-api` (graph package), `frontend/angular-app` (Knowledge Graph/Graph Explorer pages) |
| 8 | Prompt Library & AI (CRUD, versioning, playground, Ollama, evaluation) | ✅ done — `backend/spring-boot-api` (prompts package), `frontend/angular-app` (Prompt Library/Prompt Playground pages) |
| 9 | Business Intelligence (dashboards against real Gold data via Databricks SQL Warehouse, forecasting, anomaly detection, AI insights) | ✅ done — `backend/spring-boot-api` (analytics package), `frontend/angular-app` (Executive Overview/Sales Explorer/Product Analytics pages) |
| 10 | Production validation (tests, security review, deployment docs) | ✅ done — `backend/spring-boot-api/src/test`, `frontend/angular-app/src/app/**/*.spec.ts`, `docs/implementation/phase-10-security-review.md`, `docs/implementation/deployment-guide.md` |
| ~~Future~~ | ~~Azure redeployment~~ — **dropped**, see ADR-006. The local + Databricks Free Edition hybrid (ADR-005) is the permanent architecture, not a stepping stone. | ❌ not planned |

## Dependency matrix

| Phase | Depends on |
|---|---|
| 2 | 1 |
| 3 | 1 (local-infra part can run in parallel with 2; Databricks part needs the account) |
| 4 | 3 (specifically: a working Databricks Free Edition connection) |
| 5 | 3, 4 |
| 6 | 3 (document/vector pieces), 4 (dataset metadata needs real Databricks datasets to catalog) |
| 7 | 3, 6 (graph needs entities that 4/6 produce) |
| 8 | 3 (Ollama), 2 (playground UI); functionally independent of 4–7 but richer once 6/7 exist for grounding |
| 9 | 4 (needs real Gold data via Databricks SQL Warehouse), 2 |
| 10 | all |

## Phase 2 — completed

Built `frontend/angular-app` (Angular 18 standalone, NEXORA design tokens in `src/styles.scss`, collapsible sidebar + topbar, full §6 nav tree with lazy placeholder pages for every unbuilt module, reusable KPI card / panel / badge / data table / filter bar / area-line / bar / donut chart components) and `backend/spring-boot-api` (Spring Boot 4, actuator health/info, dev CORS for `localhost:4200`, JPA/datasource autoconfig deliberately disabled until Phase 3 provides a real Postgres). Executive Overview renders on mock data with a visible "Mock data" badge. Verified in-browser against Image 1 — shell, KPI cards, anomaly chart, AI insight panel, donut/bar charts, and sidebar collapse all match. `ng build` and `mvnw compile` both pass clean.

## Phase 3 — completed

**Local infrastructure**: stood up the full local stack via `docker-compose.yml`: Postgres, MinIO, Neo4j, Qdrant, Ollama, OTel Collector, Prometheus, Grafana, and a local SQL Server **source** database, all with persistent volumes and health checks. Re-enabled Spring Boot's datasource/JPA autoconfiguration against the Compose Postgres and exposed `/api/actuator/prometheus`. Restored the official `AdventureWorksLT2022.bak` into the SQL Server container via an idempotent script (`infrastructure/sqlserver/restore.sh`) that reads the backup's real logical file names rather than hardcoding them — verified all 10 `SalesLT` tables + 3 views are present and queryable. (Hit one real snag: the SQL Server image's default non-root user couldn't write to a Docker Desktop named volume on first boot — fixed with `user: root` on that service, documented in `docs/architecture/local-deployment.md`.)

**Databricks connectivity**: once the user set up the Free Edition workspace and added `DATABRICKS_HOST`/`DATABRICKS_TOKEN` to `.env`, validated every constraint in ADR-005's list against the live workspace via real API calls — auth, Clusters API, SQL Warehouse (an existing Serverless Starter Warehouse), Unity Catalog (created `workspace.nexora` schema + `raw_uploads` volume), Jobs API, and a full upload/list/delete round-trip through the Files API. All confirmed working; see `docs/discovery/databricks-free-edition-assessment.md` for the complete results table. Decided the local→Databricks upload mechanism: Unity Catalog Volumes + Files API (not DBFS).

**Per ADR-005, MinIO's role is now narrowed** to local documents/application-managed files only (Context Lake uploads) — its original bronze/silver/gold key layout is retired; that responsibility moves to Databricks-managed storage and Unity Catalog. No code currently depends on the old MinIO data-lake layout, so nothing needed to be unwound.

## Phase 4 — completed

**Local extraction**: `backend/python-worker/extract_to_databricks.py` connects to the local SQL Server container with `python-tds` (pure-Python TDS client — chosen over `pyodbc`/`sqlalchemy` from the original kickoff prompt specifically to avoid installing native ODBC drivers on the extraction host), pulls all 10 `SalesLT` tables, and uploads each as Parquet to `/Volumes/workspace/nexora/raw_uploads/{table}/{table}.parquet` via the Databricks Files API validated in Phase 3. Hit and fixed one real snag: SQL Server `uniqueidentifier` (`rowguid`) columns come back as `uuid.UUID` objects, which pyarrow writes as a Parquet `UUID` logical type that Spark's reader rejects (`PARQUET_TYPE_ILLEGAL`) — fixed by stringifying any UUID-valued column before writing Parquet.

**Databricks notebooks** (`databricks/notebooks/`): `01_bronze_ingest.py` reads the uploaded Parquet into `workspace.bronze.{table}` unmodified (10/10 tables, asserts non-zero rows). `02_silver_transform.py` carries over the original date-normalization logic and adds the real data-quality checks the original pipeline never had — primary-key null checks (8/10 tables; the 2 composite-key junction tables are explicitly marked `SKIPPED` rather than silently mis-checked), full-row duplicate detection, 4 representative foreign-key referential-integrity checks via `left_anti` joins, and freshness — all persisted to `workspace.nexora.data_quality_report` (advisory only: a quality failure logs a warning but does not fail the job). `03_gold_transform.py` carries over the original PascalCase→underscore column renaming unchanged, writing `workspace.gold.{table}`.

**Orchestration**: `backend/python-worker/deploy_notebooks.py` uploads the 3 notebooks to `/Shared/nexora/` via the Workspace API (hit and fixed a 404: the Import API does not auto-create nested parent directories, so a `workspace/mkdirs` call on the parent dir was added before `import`) and creates a 3-task sequential job (`nexora-bronze-silver-gold`, tasks chained via `depends_on`). Verified via a real triggered run: `RUNNING` → `TERMINATED`/`SUCCESS`. Confirmed via the SQL Warehouse: all 10 `workspace.gold.*` tables exist with row counts matching the source extraction exactly, column renaming applied correctly (e.g. `AddressID` → `Address_ID`), and `workspace.nexora.data_quality_report` populated with 32 `PASS` / 2 `SKIPPED` / 0 `FAIL`. Nothing in this phase touched Spring Boot's control-plane role or exposed Databricks credentials to Angular — all Databricks calls were made from local Python scripts using `.env`-sourced credentials, never from the frontend.

## Phase 5 — completed

**Spring Boot control plane** (`backend/spring-boot-api/src/main/java/com/nexora/platform/databricks` and `.../pipeline`): `DatabricksJobsClient` wraps the Jobs API (`run-now`, `runs/get`); `DatabricksSqlClient` wraps the SQL Statement Execution API (polling PENDING/RUNNING to completion, used for reading Gold/report data back without DBFS or a JDBC driver). `PipelineRunService` + `PipelineRun` (JPA entity, `schema.sql`-defined `pipeline_run` table) persist run history in Postgres. `PipelineController` exposes `POST /api/pipelines/run`, `GET /api/pipelines/runs`, `GET /api/pipelines/runs/{id}` — Angular only ever calls these; `DATABRICKS_HOST`/`DATABRICKS_TOKEN` live in Spring Boot's environment only (`docker-compose.yml`), never forwarded to a response.

**Observability**: the Prometheus scrape config for `spring-boot-api` already existed from Phase 3; added `DataQualityMetricsPoller` (`@Scheduled`, default 60s), which queries `workspace.nexora.data_quality_report` for the latest `run_id` via `DatabricksSqlClient` and republishes each check as a `nexora_data_quality_check` Micrometer gauge (1=PASS, 0.5=SKIPPED, 0=FAIL) — avoiding a second Grafana datasource. Added a provisioned dashboard (`infrastructure/observability/grafana/provisioning/dashboards/json/nexora-platform-health.json`): infra panels (API up/down, HTTP request rate, JVM heap) plus pipeline panels (overall data-quality pass rate gauge, per-check table, pipeline-run-triggered rate).

**Angular**: added `PipelineApiService` (`core/services/`) and a real `PipelineRunsComponent` at `/data-platform/pipeline-runs` (replacing its Phase 2 placeholder) — a "Run Pipeline" button that calls `POST /pipelines/run`, a run-history table with status badges, and 5s polling that calls `GET /pipelines/runs/{id}` for any non-terminal run (so Spring Boot re-checks Databricks) before re-listing. Added `provideHttpClient()` and a minimal `environment.ts` (`apiBaseUrl`).

**Verified end-to-end, twice**: triggered a run from the Angular UI itself (not just curl) — Spring Boot called `run-now`, persisted `PENDING`, the UI showed the new row, and polling carried it through `RUNNING` → `TERMINATED`/`SUCCESS` live in the browser. Confirmed in Prometheus that `nexora_data_quality_check` (34 series) and the `/pipelines/run` request counter are scraped, and confirmed via the Grafana API that the dashboard is provisioned with corrected metric queries (the first draft had a stray `/api` prefix on one `uri` label — fixed after comparing against the real Micrometer tag). No Databricks credential ever appeared in an Angular network request or response; CORS widened only to also allow port 4201 (the `ng serve` fallback port used for browser verification in this sandbox).

## Phase 6 — completed

**Dataset catalog** (`backend/spring-boot-api/.../contextlake`): `CatalogSyncService` queries `workspace.information_schema.columns` (one call for every `gold.*` table's columns) plus a dynamically built `UNION ALL` row-count query — two Databricks round trips total, not 10 — and upserts into a new `catalog_dataset` Postgres table (columns stored as JSON). Runs on a schedule (default 5 min) and on demand via `POST /catalog/refresh`; `GET /catalog/datasets` serves the Angular Catalog page.

**Document ingestion**: `DocumentIngestionService` uploads the raw file to MinIO (`MinioDocumentStore`, the `io.minio` client), extracts text (`TextExtractor` — plain UTF-8 for `.txt`/`.md`, Apache PDFBox for `.pdf`), fixed-size chunks it (`TextChunker`, 1000 chars), embeds each chunk via Ollama's `nomic-embed-text` (pulled into the existing `ollama` container — 768-dim vectors) through `OllamaEmbeddingClient`, and upserts into a new Qdrant collection (`QdrantClient`, a plain REST wrapper — avoided the official gRPC client for this scope) via `/documents`. `DocumentSearchService` embeds a query the same way and searches Qdrant for semantic matches; `DocumentController` exposes `POST /documents`, `GET /documents`, `GET /documents/search`.

**Lineage**: `LineageService` renders the already-existing Source (SQL Server) → Bronze → Silver → Gold → Catalog chain per table, with live row counts for the three Unity Catalog layers (one `UNION ALL` query) and the Catalog stage read from `catalog_dataset`. `GET /lineage/tables` and `GET /lineage/{table}`.

**Angular**: three real pages replacing their Phase 2 placeholders — `/data-platform/catalog` (dataset table with a Refresh button), `/data-platform/context-lake` (upload button, document table, semantic search box), `/data-platform/lineage` (table picker + a 5-stage visual chain). Same pattern as Phase 5: a Angular service per domain, Spring Boot REST in between, no direct frontend-to-datastore call — MinIO/Qdrant/Ollama are never reachable from the browser.

**Fixed along the way**: `.env` still had the pre-ADR-005 MinIO bucket name (`adventureworks-lt-2022`) because it had been created before `.env.example` was updated in Phase 3 — same class of drift as the missing SQL Server vars back then. Recreated MinIO with the correct `context-lake-documents` bucket and deleted the stale empty one. Also hit a DI surprise: injecting a Spring-managed `ObjectMapper` bean failed (`spring-boot-starter-webmvc` alone doesn't register one the way the old `spring-boot-starter-web` did) — worked around by instantiating `ObjectMapper` directly in the two classes that needed it rather than chasing the auto-configuration gap.

**Verified end-to-end in the browser**: uploaded a real `.txt` file through the Context Lake page, searched for it by meaning (not keyword) and got a 65%-match hit on the right chunk; refreshed the catalog and saw all 10 Gold tables with real row counts and renamed columns; picked a table in the Lineage page and saw all 5 stages render with matching row counts (450 for Address, end to end).

## Phase 7 — completed

**Graph sync** (`backend/spring-boot-api/.../graph`): `GraphSyncService` reads Customer/Address/Product/ProductCategory/SalesOrder/SalesOrderDetail/CustomerAddress straight off `workspace.gold.*` via the existing `DatabricksSqlClient` (7 round trips, one per table — no new Databricks access path), plus `catalog_dataset` and `document` straight off their existing Postgres repositories. It `MERGE`s them into Neo4j via the plain Bolt driver (`org.neo4j.driver`, not Spring Data Neo4j — same explicit-Cypher style as `QdrantClient`) and builds the real relationships: `(:Customer)-[:PLACED]->(:SalesOrder)-[:CONTAINS]->(:Product)-[:BELONGS_TO]->(:ProductCategory)`, `(:Customer)-[:HAS_ADDRESS]->(:Address)`, `(:ProductCategory)-[:SUBCATEGORY_OF]->(:ProductCategory)`, and `(:Product|:ProductCategory)-[:SOURCED_FROM]->(:Dataset)` for catalog provenance. `(:Document)-[:MENTIONS]->(:Product|:ProductCategory)` comes from a plain case-insensitive substring match of each Qdrant chunk's text (`QdrantClient.scrollAll()`, new) against known product/category names — no new NLP dependency. Runs on a schedule (default 10 min) and on demand via `POST /graph/sync`.

**Graph Explorer API**: `GraphQueryService` serves `GET /graph/stats` (node/relationship counts), `GET /graph/entities/{type}` (picker lists), and `GET /graph/entity/{type}/{id}` — a bounded-depth Cypher traversal built from a fixed type→label/id-property map, never raw Cypher from Angular. `SOURCED_FROM` is deliberately excluded from this traversal: every node of a type shares one `Dataset` node, so without excluding it a 2-hop traversal fans out to the entire dataset instead of a meaningful entity path (caught during verification, see below).

**Angular**: `/intelligence/knowledge-graph` (node/relationship totals, a per-label breakdown, Sync Graph button) and `/intelligence/graph-explorer` (entity-type + entity pickers, an SVG layered-layout diagram computed client-side via BFS from the selected entity, clicking any node re-centers the explorer on it) replace their Phase 2 placeholders — same Angular service → Spring Boot REST pattern as Phases 5–6.

**Fixed along the way**: the first sync doubled every node (1694 customers instead of 847). Cause: `MERGE` without a backing uniqueness constraint does a full node scan, and the scheduled sync firing 20s after startup raced a manual `POST /graph/sync`, so both passed the "does this exist" check before either committed. Fixed by adding a `UNIQUE` constraint per label/id-property (`GraphSyncService.ensureConstraints()`, `@PostConstruct`) and wiping the duplicated graph before re-syncing.

**Verified end-to-end**: `POST /graph/sync` → counts matching the known Gold row counts exactly (847 customers, 450 addresses, 295 products, 41 categories, 32 orders, 542 order lines, 1 document); confirmed live in the browser — selecting a Sales Order renders its customer, address, and products with real `PLACED`/`HAS_ADDRESS`/`CONTAINS`/`BELONGS_TO` edges, and clicking the customer node re-centers the explorer on that customer's own orders/address, a real interactive multi-hop path over live data.

## Phase 8 — completed

**Prompt CRUD + versioning** (`backend/spring-boot-api/.../prompts`): `Prompt`/`PromptVersion`/`PromptRun` (JPA entities, `schema.sql`-defined tables — same pattern as `pipeline_run`/`catalog_dataset`/`document`). A prompt's template can carry a `{{input}}` placeholder that the Playground's input text fills at run time (falls back to appending the input when a template has no placeholder). `PromptController`: `GET /prompts`, `POST /prompts`, `GET /prompts/{id}`, `POST /prompts/{id}/versions`, `GET /prompts/{id}/runs`, `POST /prompts/{id}/run`, `PUT /prompts/runs/{runId}/rating`.

**Prompt Playground**: `OllamaChatClient` reuses Context Lake's existing `ollamaRestClient` bean and `OllamaProperties` (Phase 6) — a second call (`/api/generate`, non-streaming) against the same client, not a new one. Pulled a small local chat model (`llama3.2:1b`) into the existing `ollama` container alongside `nomic-embed-text`. Each run persists the rendered prompt, output, model, latency, and a `PASS`/`FAIL`/`UNRATED` rating that can be set after the fact — the "basic evaluation" the kickoff asked for.

**Angular**: `/intelligence/prompt-library` (create a prompt, browse/add versions) and `/intelligence/prompt-playground` (prompt + version pickers, run, rate, run history — preselects a prompt via `?promptId=` from the Library's "Open in Playground" link) replace their Phase 2 placeholders. Same Angular service → Spring Boot REST pattern as every prior phase; no Ollama credentials or raw model calls reach Angular.

**Verified end-to-end**: created a prompt via curl, ran it against the real local model, added a second version, rated a run — all exactly reproduced live in the browser afterward, including the preselect-from-Library link and the run-history rating update.

## Phase 9 — completed

**Analytics aggregation** (`backend/spring-boot-api/.../analytics`): `AnalyticsService` queries `workspace.gold.*` directly via the existing `DatabricksSqlClient` — no new JDBC driver or Databricks access path. `salesSummary()` joins a single aggregate query (total revenue/orders/customers/products) with a `REVENUE_BY_CATEGORY_SQL` revenue-by-category breakdown (queried once, not twice) to surface the top category and its share of revenue. `revenueByCategory()` converts each category's revenue into a percentage share (the shared `HorizontalBarChartComponent` expects pre-normalized percentages); `revenueByTopCustomers()` returns raw dollar amounts (the shared `DonutChartComponent` normalizes internally). `orders()` and `productPerformance()` are straight row mappers over Gold joins.

**Anomaly detection**: discovered via a real Gold-layer query that every one of AdventureWorksLT's 32 sample orders shares a single `Order_Date` (2008-06-01) — a day-over-day revenue trend would be exactly one point with zero variance, so there's nothing to call an anomaly against. Pivoted the trend/anomaly basis to per-order total value instead (`ORDER_VALUES_SQL`, ordered by `Sales_Order_ID`, which is assigned in the order orders were placed) — real variance from ~$43 to ~$120,000 across 32 orders. `revenueTrend()` and `anomalies()` both compute a z-score (`(value - mean) / stdDev`) over these 32 values and flag `|z| > 1.3` — the "simple statistical method, no new ML dependency" the kickoff asked for. `anomalies()` additionally buckets impact (High `|z|>2.0`, Medium `|z|>1.6`, else Low) and a confidence score (`min(99, round(|z|*35))`).

**Angular**: Executive Overview (`features/overview/executive-overview.component.*`) fully de-mocked — its "Mock data" badge is gone, replaced with "Live — workspace.gold.* via Databricks"; all five KPI cards, the Order Value Trend chart, the Revenue by Category bar chart, the Top 5 Customers donut, and the Order Value Anomalies table now source from `AnalyticsApiService`. The old fabricated "AI Insight" panel (with invented factors/weights) was replaced with a "Top Anomaly" panel whose copy is derived entirely from the real top anomaly row. Two more Phase 9 pages replace their Phase 2 placeholders: `/analytics/sales-explorer` (KPI row + searchable order table) and `/analytics/products` (top-8 revenue-share bar chart + full 20-row product table) — exceeding the ≥2-page exit bar (3 pages total). All reuse the existing `AreaLineChartComponent`/`HorizontalBarChartComponent`/`DonutChartComponent`/`DataTableComponent` rather than adding a charting library.

**Fixed along the way**: `salesSummary()` initially queried the revenue-by-category SQL twice (once via `revenueByCategory()` for percentages, again via a dedicated helper for the top category's raw dollar figure) — fixed by querying it once and reading the already-`ORDER BY revenue DESC`-sorted first row. Also hit a layout bug during live-browser verification: mapping all 32 order-number labels onto the trend chart's x-axis with none marked `showLabel: false` made each unbreakable `~45px` label force the whole `.x-labels` flex row (and the page) to overflow horizontally, pushing the Top Anomaly panel off-screen — fixed by thinning the labels shown (first, last, and every Nth point) in `executive-overview.component.ts`'s `revenueTrend()` mapping, the same sparse-label approach the old mock data used.

**Verified end-to-end**: curl against every new endpoint matched a manual Gold-layer spot check (`sales-summary`: $956,303.59 revenue, 32 orders, $29,884.49 avg order, 847 customers, 295 products, top category "Touring Bikes" at 23.07%; `anomalies`: 6 real outlier orders from High to Low impact; `revenue-by-category`: 26 categories summing to ~100%). Confirmed live in the browser on all three pages — Executive Overview's KPIs/trend/anomaly panel/bar chart/donut/table, Sales Explorer's searchable order list, and Product Analytics' top-8 chart and full table all match the curl output exactly, with no mock data and no layout overflow.

## Phase 10 — completed

**Tests**: added real unit coverage for the aggregation/statistical logic that had grown
across Phases 5-9 with no tests at all — `AnalyticsServiceTest` (z-score/mean/stdDev math,
including the zero-stdDev and confidence-cap edge cases), `PromptServiceTest` (template
`{{input}}` substitution vs. append-fallback, failed-generation handling), `PipelineRunServiceTest`
(terminal-state skip logic), `CatalogSyncServiceTest` (create-vs-update upsert behavior,
empty-catalog no-op). Added `@WebMvcTest`/`MockMvc` integration tests for `AnalyticsController`,
`PromptController` (including bean-validation 400s), and `PipelineController` (404 mapping).
On the Angular side, added specs for `AnalyticsApiService` (HTTP contract via
`HttpClientTestingModule`) and the pure-logic getters in `ExecutiveOverviewComponent`
(including a regression test locking in the label-thinning fix from Phase 9),
`ProductAnalyticsComponent`, and `SalesExplorerComponent`. `mvnw test` (30 tests) and
`ng test --browsers=ChromeHeadless` (14 tests) both run clean.

**Fixed along the way**: `SpringBootApiApplicationTests` (the Phase 2 smoke test) was
silently broken for anyone running it on the host rather than inside Docker —
`application.properties`'s Postgres fallback port (`5432`) didn't match `.env.example`'s
actual mapped port (`5434`), so it connected to whatever else was listening on 5432 instead
of the Compose Postgres. Fixed the fallback; documented the failure mode in the deployment
guide's troubleshooting section in case another host hits the same thing.

**Security review** (`docs/implementation/phase-10-security-review.md`): checked credential
isolation (confirmed via live `/actuator/health` and `/actuator/env` calls, not just code
reading), CORS scope, every dynamic-SQL call site for an injection path from user input
(found none — the one path-variable-driven case, `LineageService.lineageFor`, validates
against a fixed table allowlist before the value ever reaches a query string), and secrets
management (`.env` never committed, no real credential has a fallback default). Found and
fixed one real issue: `DocumentIngestionService` built MinIO object keys directly from
unsanitized user-supplied filenames — fixed by stripping path separators.

**Deployment docs** (`docs/implementation/deployment-guide.md`): a clean-clone walkthrough —
Docker Compose stack, Databricks Free Edition setup, the SQL Server restore step, the
extract/upload/deploy-notebooks pipeline bootstrap, the Ollama model pulls, running both
test suites, and a troubleshooting section for the Postgres-port issue found above.

## Immediate next step

Phase 10 was the last phase of the local MVP (spec §11), and the user has since decided
the project will **not** move to Azure (ADR-006) — the local + Databricks Free Edition
hybrid from ADR-005 is the permanent architecture. There is no further phase queued; future
work is scoped as enhancements to the existing stack as the user requests them.

## Open assumptions (flag if wrong, otherwise proceeding)

1. AdventureWorksLT2022 is seeded locally by restoring the official `.bak` into a local SQL Server container (not the public T-SQL creation scripts) — more faithful to the original architecture and already in progress.
2. MinIO's analytical bronze/silver/gold layout from the original Phase 3 plan is retired per ADR-005; MinIO is kept only for Context Lake documents/local files.
3. The existing `.pbix` is preserved untouched; Angular is the primary BI surface going forward (see repository-assessment.md).
4. ~~No Azure work starts before local Phase 10 is complete, per spec §11 Future Phase.~~ Superseded by ADR-006: Azure redeployment is dropped entirely, not just deferred. The one cloud dependency in the whole stack is Databricks Free Edition, per ADR-005, and that remains permanent.
