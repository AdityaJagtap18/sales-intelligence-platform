# Implementation Roadmap (Phase-Gated)

Status legend: ✅ done · ▶ next · ⏳ not started

| Phase | Scope | Status |
|---|---|---|
| 1 | Project discovery & repository assessment | ✅ done (this set of docs) |
| 2 | Application foundation & design system (Angular shell, Spring Boot skeleton, NEXORA design tokens, nav, Executive Overview shell) | ▶ next |
| 3 | Local infrastructure (Docker Compose: Postgres, MinIO, Neo4j, Qdrant, Ollama, OTel/Prometheus/Grafana) | ⏳ |
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

## Immediate next step (Phase 2 kickoff prompt)

> Build the Angular application shell and Spring Boot API skeleton. Deliverables: global design tokens derived from Image 1 (NEXORA) per `docs/design/reference-analysis.md`; sidebar + top nav per §6 of the master spec; reusable KPI card, table, chart-container, filter, and badge components; the full navigation tree (even for modules not yet implemented, as disabled/placeholder routes); and a first working Executive Overview screen using **mock data clearly labeled as mock** (no live pipeline exists until Phase 4). Spring Boot: project skeleton, health endpoint, CORS/dev config, no business endpoints yet beyond a `/health` check. Exit criteria: `ng serve` renders a shell visually consistent with Image 1, navigation covers every module in §6, and `docs/architecture/system-architecture.md`'s Postgres/MinIO/etc. ownership rules are not yet violated by anything built (nothing should call a datastore directly from Angular).

## Open assumptions (flag if wrong, otherwise proceeding)

1. AdventureWorksLT2022 will be seeded locally via the public Microsoft sample scripts (not restored from a `.bak`, which isn't in this repo) — acceptable since the spec only requires the same *data*, not the original restore path.
2. Delta Lake variant of the notebooks is the one carried forward; parquet-only variants are preserved as legacy reference, not actively maintained (see repository-assessment.md).
3. The existing `.pbix` is preserved untouched; Angular is the primary BI surface going forward (see repository-assessment.md).
4. No Azure work starts before local Phase 10 is complete, per spec §11 Future Phase.
