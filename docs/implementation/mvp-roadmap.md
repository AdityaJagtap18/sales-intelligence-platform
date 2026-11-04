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
| 4 | Databricks notebook adaptation and pipeline implementation (Bronze/Silver/Gold on Databricks, local source extraction/upload) | ▶ next |
| 5 | Databricks orchestration integration (Jobs/Lakeflow) and local observability | ⏳ |
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

## Immediate next step (Phase 4 kickoff prompt)

> Build the Databricks notebook adaptation and pipeline: write a local extraction step (Python, reading from the `sqlserver` container via `pyodbc`/`sqlalchemy`) that pulls each `SalesLT` table to Parquet and uploads it to `/Volumes/workspace/nexora/raw_uploads/{table}/` via the Databricks Files API (the mechanism validated in Phase 3 — see `docs/discovery/databricks-free-edition-assessment.md`). Then adapt the existing Bronze→Silver→Gold notebook logic (date normalization, PascalCase→underscore renaming — see `docs/discovery/repository-assessment.md`) to run as Databricks notebooks against that volume, writing Delta tables into `workspace.nexora` under Unity Catalog (no MinIO, no local PySpark — per ADR-005). Add the real data quality checks in the Silver step that the original notebooks never had (nulls, duplicates, referential integrity, freshness — per master-spec Module C), since that's an explicitly named gap, not a rewrite of working logic. Orchestrate as a Databricks Job (even a simple sequential one) rather than running notebooks ad hoc, since Phase 5 builds job-execution monitoring on top of whatever job shape Phase 4 creates. Exit criteria: running the pipeline produces real Gold-layer Delta tables in `workspace.nexora` for all 10 `SalesLT` tables, queryable from the SQL Warehouse, with at least a basic data-quality report for the Silver step — and nothing in this phase touches Spring Boot's control-plane role or exposes Databricks credentials to Angular, per `docs/architecture/system-architecture.md`.

## Open assumptions (flag if wrong, otherwise proceeding)

1. AdventureWorksLT2022 is seeded locally by restoring the official `.bak` into a local SQL Server container (not the public T-SQL creation scripts) — more faithful to the original architecture and already in progress.
2. MinIO's analytical bronze/silver/gold layout from the original Phase 3 plan is retired per ADR-005; MinIO is kept only for Context Lake documents/local files.
3. The existing `.pbix` is preserved untouched; Angular is the primary BI surface going forward (see repository-assessment.md).
4. No Azure work starts before local Phase 10 is complete, per spec §11 Future Phase. The one cloud dependency in the whole stack is Databricks Free Edition, per ADR-005 — this is intentional, not a drift back toward Azure.
