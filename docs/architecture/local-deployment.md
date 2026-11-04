# Local Deployment (Phase 3)

## Bring the stack up

```bash
cp .env.example .env   # adjust ports if something on your host already uses them
docker compose up -d --build
docker compose ps      # everything should show "healthy" within ~30s
```

## Services and default ports

| Service | Container | Host port (default) | Purpose |
|---|---|---|---|
| PostgreSQL | `postgres` | 5432 | App metadata (users, pipeline registry, prompts, audit) |
| MinIO (API / Console) | `minio` | 9000 / 9001 | Object storage — `bronze/SalesLT/...`, `Silver/...`, `gold/...` |
| Neo4j (HTTP / Bolt) | `neo4j` | 7474 / 7687 | Knowledge graph |
| Qdrant (HTTP / gRPC) | `qdrant` | 6333 / 6334 | Vector search |
| Ollama | `ollama` | 11434 | Local LLM inference |
| OTel Collector (gRPC / HTTP) | `otel-collector` | 4317 / 4318 | Metrics/trace ingestion |
| Prometheus | `prometheus` | 9090 | Metrics storage |
| Grafana | `grafana` | 3000 | Engineering dashboards |
| Spring Boot API | `spring-boot-api` | 8080 | Control plane |

**This machine's actual `.env` remaps three of these** because other local projects already held the default ports: Postgres → `5434`, Grafana → `3010`, Spring Boot → `8081`. `.env.example` documents the remapped values actually used; if a host doesn't have those conflicts, the original defaults (5432/3000/8080) are fine — just edit `.env`.

## Known substitution: MinIO image

The official `minio/minio` image was pulled from Docker Hub behind a login wall in 2025 (MinIO's new licensing). `docker-compose.yml` uses `bitnamilegacy/minio` instead — a frozen, unauthenticated, S3-API-compatible replacement. It's "legacy" (no longer updated) but sufficient for a local MVP; this is recorded here so Phase 4+ doesn't waste time re-discovering why the compose file doesn't say `minio/minio`. `MINIO_DEFAULT_BUCKETS` (a Bitnami-image feature) creates the `adventureworks-lt-2022` bucket automatically on first boot, so no separate `mc mb` init container is needed.

## What's verified working (Phase 3 exit criteria)

- `docker compose up` brings up all 9 services, each reporting healthy.
- Spring Boot connects to Postgres on startup — confirmed via `GET /api/actuator/health` (`"db":{"status":"UP"}`).
- Prometheus is actively scraping all 3 configured targets (`otel-collector`, `prometheus` itself, `spring-boot-api` via `/api/actuator/prometheus`) — all report `"health":"up"`.
- MinIO, Neo4j, Qdrant, and Ollama are each reachable from the host on their documented ports, with no Azure dependency anywhere in the stack (ADR-001).
- Grafana is reachable and provisioned with the Prometheus datasource (no dashboards yet — those are built in Phase 5 against real pipeline/data-quality metrics, not invented now).

## What Phase 3 deliberately leaves for later

- Grafana dashboards (Phase 5 — needs real pipeline metrics to chart).
- Wiring Spring Boot/Python traces through the OTel Collector (Phase 5 — collector and Prometheus scrape path exist now; trace instrumentation is pipeline-management work).
- Postgres schema/entities (Phase 4+ — `spring.jpa.hibernate.ddl-auto=validate` is intentionally a no-op today since no `@Entity` classes exist yet).
