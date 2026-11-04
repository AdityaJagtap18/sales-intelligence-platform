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
| 6 | Context Lake (dataset registry, metadata catalog, document ingestion, semantic search, lineage — MinIO for local documents only, not analytical data) | ▶ next |
| 7 | Knowledge Graph (Neo4j schema, sync, Graph Explorer UI) | ⏳ |
| 8 | Prompt Library & AI (CRUD, versioning, playground, Ollama, evaluation) | ⏳ |
| 9 | Business Intelligence (dashboards against real Gold data via Databricks SQL Warehouse, forecasting, anomaly detection, AI insights) | ⏳ |
| 10 | Production validation (tests, security review, deployment docs) | ⏳ |
| Future | Azure redeployment | ⏳ (explicitly out of local MVP) |

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

## Immediate next step (Phase 6 kickoff prompt)

> Build the Context Lake: a dataset registry, metadata catalog, document ingestion, and semantic search over local documents, per the master spec. Register Databricks's Gold-layer tables (`workspace.gold.*`) as datasets in the catalog (queried via the existing `DatabricksSqlClient`/SQL Warehouse path from Phase 5 — don't add a new Databricks access path), with metadata (row counts, columns, last-updated) refreshed on a schedule similar to the Phase 5 data-quality poller. For documents: wire the existing local MinIO (`context-lake-documents` bucket, narrowed to this role since ADR-005) for storage, add an ingestion endpoint in Spring Boot that accepts uploads, extracts text, and pushes embeddings to the existing Qdrant container for semantic search — no new vector store. Add a basic lineage view connecting SalesLT source tables (Phase 4) → Bronze/Silver/Gold (Databricks) → catalog entries, since that chain already exists end-to-end and just needs to be made visible. Build real pages for `/data-platform/catalog`, `/data-platform/context-lake`, and `/data-platform/lineage` (currently placeholders), following the pattern established in Phase 5's Pipeline Runs page (Angular service → Spring Boot REST → backing store, never a direct frontend-to-datastore call). Exit criteria: uploading a document through the UI makes it searchable by semantic similarity; the catalog page lists all 10 Gold tables with real, refreshed metadata; the lineage view renders the source→Bronze→Silver→Gold→catalog chain for at least one table.

## Open assumptions (flag if wrong, otherwise proceeding)

1. AdventureWorksLT2022 is seeded locally by restoring the official `.bak` into a local SQL Server container (not the public T-SQL creation scripts) — more faithful to the original architecture and already in progress.
2. MinIO's analytical bronze/silver/gold layout from the original Phase 3 plan is retired per ADR-005; MinIO is kept only for Context Lake documents/local files.
3. The existing `.pbix` is preserved untouched; Angular is the primary BI surface going forward (see repository-assessment.md).
4. No Azure work starts before local Phase 10 is complete, per spec §11 Future Phase. The one cloud dependency in the whole stack is Databricks Free Edition, per ADR-005 — this is intentional, not a drift back toward Azure.
