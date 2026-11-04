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
| 5 | Databricks orchestration integration (Jobs/Lakeflow) and local observability | ▶ next |
| 6 | Context Lake (dataset registry, metadata catalog, document ingestion, semantic search, lineage — MinIO for local documents only, not analytical data) | ⏳ |
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

## Immediate next step (Phase 5 kickoff prompt)

> Build Databricks orchestration integration and local observability. On the Spring Boot side (the control plane, per ADR-005 — Angular must never see Databricks credentials): add a service that calls the Databricks Jobs API (`/api/2.2/jobs/run-now`, `/api/2.2/jobs/runs/get`) to trigger and poll the `nexora-bronze-silver-gold` job created in Phase 4, expose it via a small internal REST endpoint (e.g. `POST /api/pipelines/run`, `GET /api/pipelines/runs/{id}`) that Angular can call without ever touching `DATABRICKS_HOST`/`DATABRICKS_TOKEN` directly, and persist run history (job id, run id, state, timestamps, triggered-by) in Postgres. On the observability side: wire Spring Boot's existing `/api/actuator/prometheus` endpoint into the Compose Prometheus (scrape config) and build a Grafana dashboard covering both infra health (service up/down, JVM/HTTP metrics already exposed by Actuator) and pipeline health (job run history and the Phase 4 data-quality report — `workspace.nexora.data_quality_report`, queried via the SQL Warehouse through the same Databricks service). Exit criteria: triggering a pipeline run from Angular (through Spring Boot, not directly) produces a real Databricks job run that completes and whose status is visible in the UI; Grafana shows live infra metrics and at least one panel surfacing the latest data-quality report results; still no Databricks credentials reach the browser at any point.

## Open assumptions (flag if wrong, otherwise proceeding)

1. AdventureWorksLT2022 is seeded locally by restoring the official `.bak` into a local SQL Server container (not the public T-SQL creation scripts) — more faithful to the original architecture and already in progress.
2. MinIO's analytical bronze/silver/gold layout from the original Phase 3 plan is retired per ADR-005; MinIO is kept only for Context Lake documents/local files.
3. The existing `.pbix` is preserved untouched; Angular is the primary BI surface going forward (see repository-assessment.md).
4. No Azure work starts before local Phase 10 is complete, per spec §11 Future Phase. The one cloud dependency in the whole stack is Databricks Free Edition, per ADR-005 — this is intentional, not a drift back toward Azure.
