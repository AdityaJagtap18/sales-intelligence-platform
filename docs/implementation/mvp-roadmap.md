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
| 8 | Prompt Library & AI (CRUD, versioning, playground, Ollama, evaluation) | ▶ next |
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

## Immediate next step (Phase 8 kickoff prompt)

> Build the Prompt Library & AI module: CRUD + versioning for reusable prompts (Postgres-backed, same JPA pattern as `pipeline_run`/`catalog_dataset`/`document`), a Prompt Playground that executes a prompt against a local Ollama model (reuse the `OllamaProperties`/`ollamaRestClient` pattern from Context Lake — add a chat/generate call alongside the existing embeddings call, not a new client), and basic evaluation (store a prompt run's output + a simple pass/fail or rating alongside the version it used). Expose it via Spring Boot (`POST /prompts`, `GET /prompts`, `POST /prompts/{id}/versions`, `POST /prompts/{id}/run`) and build `/intelligence/prompt-library` and `/intelligence/prompt-playground` (currently placeholders) using the same Angular service → Spring Boot REST pattern as Phases 5–7. If time allows, let a playground run optionally ground itself in Context Lake search results or a Knowledge Graph entity (cite what it used), tying Phase 6–8 together — but a working CRUD + playground + versioning is the exit bar, not grounding. Exit criteria: a prompt can be created, versioned, and run against a real local Ollama model from the UI, with run history persisted; no Ollama credentials or raw model calls exposed to Angular.

## Open assumptions (flag if wrong, otherwise proceeding)

1. AdventureWorksLT2022 is seeded locally by restoring the official `.bak` into a local SQL Server container (not the public T-SQL creation scripts) — more faithful to the original architecture and already in progress.
2. MinIO's analytical bronze/silver/gold layout from the original Phase 3 plan is retired per ADR-005; MinIO is kept only for Context Lake documents/local files.
3. The existing `.pbix` is preserved untouched; Angular is the primary BI surface going forward (see repository-assessment.md).
4. No Azure work starts before local Phase 10 is complete, per spec §11 Future Phase. The one cloud dependency in the whole stack is Databricks Free Edition, per ADR-005 — this is intentional, not a drift back toward Azure.
