# Local Deployment

> **Updated per ADR-005.** MinIO's role below is now local documents only (Context Lake
> uploads) — analytical Bronze/Silver/Gold processing moved to Databricks Free Edition.
> A local SQL Server service was added as the *source* database for the ingestion path
> that feeds Databricks (restore → extract → upload), not as an app datastore.

## Bring the stack up

```bash
cp .env.example .env   # adjust ports if something on your host already uses them
docker compose up -d --build
docker compose ps      # everything should show "healthy" within ~30s (sqlserver takes longer on first boot)
```

## Services and default ports

| Service | Container | Host port (default) | Purpose |
|---|---|---|---|
| PostgreSQL | `postgres` | 5432 | App metadata (users, pipeline registry, prompts, audit) |
| SQL Server | `sqlserver` | 1433 | **Source** DB — AdventureWorksLT2022, restored from the official `.bak`; extracted from here and uploaded to Databricks, not queried directly by the app |
| SQL Server restore (one-shot) | `sqlserver-restore` | — | Runs `infrastructure/sqlserver/restore.sh`, restores the database, then exits; safe to re-run |
| MinIO (API / Console) | `minio` | 9000 / 9001 | Local documents / Context Lake files only |
| Neo4j (HTTP / Bolt) | `neo4j` | 7474 / 7687 | Knowledge graph |
| Qdrant (HTTP / gRPC) | `qdrant` | 6333 / 6334 | Vector search |
| Ollama | `ollama` | 11434 | Local LLM inference |
| OTel Collector (gRPC / HTTP) | `otel-collector` | 4317 / 4318 | Metrics/trace ingestion |
| Prometheus | `prometheus` | 9090 | Metrics storage |
| Grafana | `grafana` | 3000 | Engineering dashboards |
| Spring Boot API | `spring-boot-api` | 8080 | Control plane — also the only place Databricks credentials are read from (`DATABRICKS_HOST`/`DATABRICKS_TOKEN` in `.env`, never in Angular) |

**This machine's actual `.env` remaps four of these** because other local projects already held the default ports: Postgres → `5434`, Grafana → `3010`, Spring Boot → `8081`. SQL Server (`1433`) hasn't hit a conflict yet on this machine. `.env.example` documents the remapped values actually used; if a host doesn't have those conflicts, the original defaults are fine — just edit `.env`.

## Known substitution: MinIO image

The official `minio/minio` image was pulled from Docker Hub behind a login wall in 2025 (MinIO's new licensing). `docker-compose.yml` uses `bitnamilegacy/minio` instead — a frozen, unauthenticated, S3-API-compatible replacement. It's "legacy" (no longer updated) but sufficient for local document storage; this is recorded here so later phases don't waste time re-discovering why the compose file doesn't say `minio/minio`.

## SQL Server source data

`infrastructure/sqlserver/AdventureWorksLT2022.bak` is downloaded from Microsoft's official
`sql-server-samples` GitHub releases (not committed — it's large and reproducible; see
`.gitignore`). `restore.sh` waits for the server to accept connections, checks whether the
database already exists (idempotent), reads the backup's actual logical file names via
`RESTORE FILELISTONLY` (not hardcoded, since Microsoft doesn't guarantee naming across
sample releases), and restores it. Table extraction + upload to Databricks is Phase 4 work,
once Databricks connectivity is confirmed (see `docs/implementation/mvp-roadmap.md`).

## What's verified working (Phase 3 local-infra exit criteria)

- `docker compose up` brings up Postgres, MinIO, Neo4j, Qdrant, Ollama, OTel Collector,
  Prometheus, Grafana, and Spring Boot — all reporting healthy.
- Spring Boot connects to Postgres on startup — confirmed via `GET /api/actuator/health` (`"db":{"status":"UP"}`).
- Prometheus is actively scraping all 3 configured targets — all report `"health":"up"`.
- MinIO, Neo4j, Qdrant, and Ollama are each reachable from the host on their documented ports.
- Grafana is reachable and provisioned with the Prometheus datasource (no dashboards yet — Phase 5).

**Not yet verified**: the `sqlserver`/`sqlserver-restore` services (added after the above
verification pass) and Databricks connectivity — both pending the user's Databricks Free
Edition account setup and a re-run of `docker compose up`.

## What's deliberately left for later

- Grafana dashboards (Phase 5 — needs real pipeline metrics to chart).
- Databricks job/trace instrumentation through the OTel Collector (Phase 5).
- Postgres schema/entities (Phase 4+ — `spring.jpa.hibernate.ddl-auto=validate` is intentionally a no-op today since no `@Entity` classes exist yet).
