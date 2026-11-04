# Implementation Roadmap (Phase-Gated)

Status legend: ✅ done · ▶ next · ⏳ not started

| Phase | Scope | Status |
|---|---|---|
| 1 | Project discovery & repository assessment | ✅ done (this set of docs) |
| 2 | Application foundation & design system (Angular shell, Spring Boot skeleton, NEXORA design tokens, nav, Executive Overview shell) | ✅ done — `frontend/angular-app`, `backend/spring-boot-api` |
| 3 | Local infrastructure (Docker Compose: Postgres, MinIO, Neo4j, Qdrant, Ollama, OTel/Prometheus/Grafana) | ▶ next |
| 4 | Core data engineering (AdventureWorksLT2022 local source, Bronze/Silver/Gold ported to MinIO+Delta, data quality checks) | ⏳ |
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

## Immediate next step (Phase 3 kickoff prompt)

> Stand up local infrastructure via Docker Compose: PostgreSQL, MinIO, Neo4j, Qdrant, Ollama, and the observability trio (OpenTelemetry collector, Prometheus, Grafana). Deliverables: `docker-compose.yml` at the repo root plus per-service config under `infrastructure/`; persistent named volumes for every stateful service; health checks; a `.env.example` documenting every required variable (ports, credentials, bucket/database names); and a startup order that respects dependencies (e.g., app services wait on Postgres/MinIO health). Re-enable the Spring Boot datasource/JPA autoconfiguration now that a real Postgres exists, and point it at the Compose Postgres via env vars. Exit criteria: `docker compose up` brings up every service healthy, Spring Boot connects to Postgres on startup, and MinIO/Neo4j/Qdrant/Ollama are each reachable from the host on their documented ports — all without any Azure dependency, per ADR-001.

## Open assumptions (flag if wrong, otherwise proceeding)

1. AdventureWorksLT2022 will be seeded locally via the public Microsoft sample scripts (not restored from a `.bak`, which isn't in this repo) — acceptable since the spec only requires the same *data*, not the original restore path.
2. Delta Lake variant of the notebooks is the one carried forward; parquet-only variants are preserved as legacy reference, not actively maintained (see repository-assessment.md).
3. The existing `.pbix` is preserved untouched; Angular is the primary BI surface going forward (see repository-assessment.md).
4. No Azure work starts before local Phase 10 is complete, per spec §11 Future Phase.
