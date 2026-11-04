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
| 3 | Local infrastructure + Databricks connectivity (Docker Compose: Postgres, Neo4j, Qdrant, Ollama, OTel/Prometheus/Grafana; validate Databricks Free Edition access) | ⏸ local infra done; Databricks connectivity blocked on account setup (see below) |
| 4 | Databricks notebook adaptation and pipeline implementation (Bronze/Silver/Gold on Databricks, local source extraction/upload) | ⏳ blocked on Phase 3 Databricks connectivity |
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

## Phase 3 — local infra done, Databricks connectivity blocked

Stood up the full local stack via `docker-compose.yml`: Postgres, MinIO, Neo4j, Qdrant, Ollama, OTel Collector, Prometheus, and Grafana, all with persistent volumes and health checks. Re-enabled Spring Boot's datasource/JPA autoconfiguration against the Compose Postgres and exposed `/api/actuator/prometheus`. Verified live: all 9 containers healthy, Spring Boot's health check confirms a real DB connection, Prometheus scraping all 3 targets.

**Per ADR-005, MinIO's role is now narrowed** to local documents/application-managed files only (Context Lake uploads) — its original bronze/silver/gold key layout is retired; that responsibility moves to Databricks-managed storage and Unity Catalog. No code currently depends on the old MinIO data-lake layout, so nothing needs to be unwound.

**Databricks connectivity is blocked on a user action this assistant cannot take**: creating the Databricks Free Edition account/workspace. See "Blocked: next action" below.

## Blocked: next action (needs the user)

1. Sign up for [Databricks Free Edition](https://www.databricks.com/product/pricing) (or confirm an existing account) and create/confirm a workspace.
2. In the workspace, generate a **personal access token** (User Settings → Developer → Access tokens).
3. Add two values to the project's `.env` (never pasted into chat, per this assistant's security rules):
   ```
   DATABRICKS_HOST=https://<your-workspace>.cloud.databricks.com
   DATABRICKS_TOKEN=<your personal access token>
   ```
4. Confirm back here once that's done — Phase 3's Databricks-connectivity validation (auth, Jobs API, file upload, SQL Warehouse, fair-use/resource limits) and Phase 4 (notebook adaptation) pick up immediately from there.

## Immediate next step once unblocked (Phase 3 Databricks-connectivity validation)

> With `DATABRICKS_HOST`/`DATABRICKS_TOKEN` available, validate what Free Edition actually supports before building anything on top of it: authenticate via the Databricks REST API (e.g. `/api/2.0/clusters/list` or `/api/2.1/unity-catalog/catalogs`), confirm whether a SQL Warehouse is available and queryable, confirm the Jobs API is reachable, and confirm a file/table upload path (DBFS, Unity Catalog volumes, or `databricks-sql-connector`/`databricks-connect`). Record findings — what's available, what's rate-limited, what's missing — in `docs/architecture/ai-architecture.md` or a new `docs/discovery/databricks-free-edition-assessment.md`, since the master spec explicitly requires validating these constraints before relying on them (no assuming paid-tier features). Exit criteria: a documented, verified answer for each constraint in ADR-005's "Free Edition constraints" list, and a decision on the local→Databricks upload mechanism Phase 4 will use.

## In progress, not blocked: local source data

Independent of the Databricks blocker, `infrastructure/sqlserver/AdventureWorksLT2022.bak` is
being downloaded from Microsoft's official sample releases (same source this assistant would
use either way) so the SQL Server container + restore + table extraction step is ready the
moment Databricks connectivity is confirmed.

## Open assumptions (flag if wrong, otherwise proceeding)

1. AdventureWorksLT2022 is seeded locally by restoring the official `.bak` into a local SQL Server container (not the public T-SQL creation scripts) — more faithful to the original architecture and already in progress.
2. MinIO's analytical bronze/silver/gold layout from the original Phase 3 plan is retired per ADR-005; MinIO is kept only for Context Lake documents/local files.
3. The existing `.pbix` is preserved untouched; Angular is the primary BI surface going forward (see repository-assessment.md).
4. No Azure work starts before local Phase 10 is complete, per spec §11 Future Phase. The one cloud dependency in the whole stack is Databricks Free Edition, per ADR-005 — this is intentional, not a drift back toward Azure.
