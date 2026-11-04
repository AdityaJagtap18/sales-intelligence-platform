# Implementation Roadmap (Phase-Gated)

Status legend: ✅ done · ▶ next · ⏳ not started

| Phase | Scope | Status |
|---|---|---|
| 1 | Project discovery & repository assessment | ✅ done (this set of docs) |
| 2 | Application foundation & design system (Angular shell, Spring Boot skeleton, NEXORA design tokens, nav, Executive Overview shell) | ✅ done — `frontend/angular-app`, `backend/spring-boot-api` |
| 3 | Local infrastructure (Docker Compose: Postgres, MinIO, Neo4j, Qdrant, Ollama, OTel/Prometheus/Grafana) | ✅ done — `docker-compose.yml`, `docs/architecture/local-deployment.md` |
| 4 | Core data engineering (AdventureWorksLT2022 local source, Bronze/Silver/Gold ported to MinIO+Delta, data quality checks) | ▶ next |
| 5 | Pipeline management & observability (registry, execution API, run history, logs/metrics/traces, alerts) | ⏳ |
| 6 | Context Lake (dataset registry, metadata catalog, document ingestion, semantic search, lineage) | ⏳ |
| 7 | Knowledge Graph (Neo4j schema, sync, Graph Explorer UI) | ⏳ |
| 8 | Prompt Library & AI (CRUD, versioning, playground, Ollama, evaluation) | ⏳ |
| 9 | Business Intelligence (dashboards against real Gold data, forecasting, anomaly detection, AI insights) | ⏳ |
| 10 | Production validation (tests, security review, deployment docs) | ⏳ |
| Future | Azure redeployment | ⏳ (explicitly out of local MVP) |

## Dependency matrix

| Phase | Depends on |
|---|---|
| 2 | 1 |
| 3 | 1 (can run in parallel with 2) |
| 4 | 3 |
| 5 | 3, 4 |
| 6 | 3 (document/vector pieces), 4 (dataset metadata needs real datasets to catalog) |
| 7 | 3, 6 (graph needs entities that 4/6 produce) |
| 8 | 3 (Ollama), 2 (playground UI); functionally independent of 4–7 but richer once 6/7 exist for grounding |
| 9 | 4 (needs real Gold data), 2 |
| 10 | all |

## Phase 2 — completed

Built `frontend/angular-app` (Angular 18 standalone, NEXORA design tokens in `src/styles.scss`, collapsible sidebar + topbar, full §6 nav tree with lazy placeholder pages for every unbuilt module, reusable KPI card / panel / badge / data table / filter bar / area-line / bar / donut chart components) and `backend/spring-boot-api` (Spring Boot 4, actuator health/info, dev CORS for `localhost:4200`, JPA/datasource autoconfig deliberately disabled until Phase 3 provides a real Postgres). Executive Overview renders on mock data with a visible "Mock data" badge. Verified in-browser against Image 1 — shell, KPI cards, anomaly chart, AI insight panel, donut/bar charts, and sidebar collapse all match. `ng build` and `mvnw compile` both pass clean.

## Phase 3 — completed

Stood up the full local stack via `docker-compose.yml`: Postgres, MinIO (substituted `bitnamilegacy/minio` for the now-gated `minio/minio` — see `docs/architecture/local-deployment.md`), Neo4j, Qdrant, Ollama, OTel Collector, Prometheus, and Grafana, all with persistent volumes and health checks. Re-enabled Spring Boot's datasource/JPA autoconfiguration against the Compose Postgres and exposed `/api/actuator/prometheus`. Verified live: all 9 containers healthy, Spring Boot's health check confirms a real DB connection, and Prometheus is actively scraping all 3 targets.

## Immediate next step (Phase 4 kickoff prompt)

> Build core data engineering: seed AdventureWorksLT2022 locally (public Microsoft sample scripts, run against a local SQL Server container or directly loaded into the Bronze layer — decide based on what's actually simplest, don't stand up a SQL Server service if the Bronze loader can read the sample data files directly), then port the existing Bronze→Silver→Gold notebook logic (date normalization, PascalCase→underscore renaming — see `docs/discovery/repository-assessment.md`) into a Python worker using PySpark + Delta Lake against MinIO (`s3a://` in place of the old `abfss://` mount), preserving the same `bronze/SalesLT/{table}/...` → `Silver/...` → `gold/...` key layout. Add real data quality checks in the Silver step that the original notebooks never had (nulls, duplicates, referential integrity, freshness — per master-spec Module C) since that's explicitly called out as a gap, not a rewrite of working logic. Exit criteria: running the worker against the seeded source produces real Gold-layer Delta tables in MinIO for all AdventureWorksLT2022 `SalesLT` tables, with a data-quality report (even a simple one) for at least the Silver step, and nothing in this phase touches Spring Boot's control-plane role per `docs/architecture/system-architecture.md`.

## Open assumptions (flag if wrong, otherwise proceeding)

1. AdventureWorksLT2022 will be seeded locally via the public Microsoft sample scripts (not restored from a `.bak`, which isn't in this repo) — acceptable since the spec only requires the same *data*, not the original restore path.
2. Delta Lake variant of the notebooks is the one carried forward; parquet-only variants are preserved as legacy reference, not actively maintained (see repository-assessment.md).
3. The existing `.pbix` is preserved untouched; Angular is the primary BI surface going forward (see repository-assessment.md).
4. No Azure work starts before local Phase 10 is complete, per spec §11 Future Phase.
