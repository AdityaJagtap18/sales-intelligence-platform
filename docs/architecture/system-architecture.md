# System Architecture Baseline

> **Superseded in part by ADR-005.** The original Phase 1 baseline put a local PySpark
> worker + MinIO in the data-engineering role below. Per ADR-005, that role now belongs to
> **Databricks Free Edition** — the hybrid architecture's one cloud component. This doc is
> updated to match; see ADR-005 for the full rationale.

## Component map (current)

```
┌─────────────┐      REST       ┌───────────────────┐        authenticated API       ┌──────────────────────┐
│  Angular    │ ───────────────▶│  Spring Boot API    │ ──────────────────────────────▶│  Databricks Free       │
│  (frontend) │ ◀─────────────── │  (control plane)    │◀──────────────────────────────│  Edition (cloud)       │
└─────────────┘                 └─────────┬───────────┘   Jobs API / SQL Warehouse    │  Bronze/Silver/Gold,   │
                                           │                                            │  Delta Lake, Unity     │
                                           │                                            │  Catalog, Jobs/        │
                                           ▼                                            │  Lakeflow              │
                                 ┌───────────────────┐                                  └──────────────────────┘
                                 │  PostgreSQL          │
                                 │  (app metadata,      │
                                 │  pipeline registry,   │
                                 │  execution history,   │
                                 │  prompts, audit)      │
                                 └───────────────────┘

           ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
           │ MinIO (local       │  │ Neo4j (graph)     │  │ Qdrant (vectors)  │
           │ documents only,    │  └─────────────────┘  └─────────────────┘
           │ NOT analytical     │
           │ bronze/silver/gold)│                        ┌─────────────────┐
           └─────────────────┘                        │ Ollama (local LLM) │
                                                        └─────────────────┘

Observability (local side): OpenTelemetry SDK in Spring Boot → Prometheus → Grafana.
Observability (Databricks side): Jobs API / SQL Warehouse query history, surfaced through
Spring Boot, clearly labeled as Databricks-sourced — never conflated with local metrics.
Angular has its own app-level Observability Center reading aggregated data via Spring
Boot, not Grafana or Databricks directly.
```

## Ownership rules (fixed now to avoid drift later)

- **Spring Boot owns control, never bulk data processing.** It tracks pipeline *definitions* and *execution metadata* (status, duration, row counts, errors) in Postgres, and persists Databricks job-run history locally so historical runs are visible even if Databricks is unreachable. It triggers and monitors Databricks jobs through the Jobs API and queries Gold-layer data through SQL Warehouse connectivity — it never runs Spark itself.
- **Databricks Free Edition owns all Bronze/Silver/Gold processing.** Delta tables live in Databricks-managed storage under Unity Catalog, not in MinIO. Orchestration uses Databricks Jobs/Lakeflow where Free Edition supports it.
- **MinIO is local-only, document-scoped.** It holds Context Lake uploads and other application-managed files — never analytical bronze/silver/gold data. (This narrows MinIO's role from the original Phase 3 plan; see ADR-005. No code currently depends on the old layout, so nothing needs to be migrated.)
- **Postgres is the only system of record for app metadata** (users, pipeline registry, Databricks execution history mirror, dataset catalog metadata, prompt library + versions, audit log) — the Context Lake's "metadata catalog" is Postgres tables, not a separate database.
- **The Context Lake is a logical layer, not a new datastore.** It is Spring Boot APIs that join across Postgres (metadata), MinIO (local documents), Qdrant (semantic search), Neo4j (lineage/relationships), and references to Databricks-held datasets (not their bulk data) — per spec §4's explicit instruction not to confuse it with a physical data lake.
- **Neo4j** holds only graph nodes/edges (business + technical entities) and provenance flags (source-fact vs inferred, per Image 3's solid/dotted edge convention). Sync direction is one-way for MVP: Postgres/Databricks facts → Neo4j, never the reverse.
- **Qdrant** holds only vectors + pointers (doc/dataset id) back to Postgres/MinIO — never the canonical text itself beyond what's needed for retrieval snippets.
- **Ollama** is called only through a provider interface (Spring Boot, and/or a future local worker for heavier batch work) so a future hosted-LLM swap doesn't touch business logic — per spec §5.2.
- **Databricks credentials (host + PAT) live only in Spring Boot's environment.** Never in Angular, never hardcoded, never requested or pasted into chat — see ADR-005.

## What's deliberately left open

- Exact Databricks Jobs/Lakeflow orchestration shape — decided in Phase 5 once Phase 3's
  connectivity validation shows what Free Edition actually supports.
- Whether embedding generation happens in Spring Boot or a dedicated local worker — decided
  in Phase 6 (Context Lake) based on what's simplest with Ollama's embedding models.
- Exact Postgres schema — drafted in Phase 3/4, not earlier.

## No Azure (ADR-006)

An earlier version of this doc described a thin "Azure compatibility seam" kept open for a
future Azure Databricks swap. Per ADR-006, that future phase is dropped, not deferred —
Databricks Free Edition plus the local Docker Compose stack (ADR-005) is the permanent
architecture. The Spring Boot abstraction around Databricks (Jobs API / SQL Warehouse
client) and Ollama's provider interface still exist because they're good internal
boundaries on their own merits, not because they're being kept open for an Azure migration.
No Azure SDKs, Key Vault clients, or ADF-shaped abstractions should be added.
