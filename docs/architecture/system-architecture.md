# System Architecture Baseline (Phase 1)

This is the baseline to be refined in Phase 3+. It fixes component ownership so later phases don't re-litigate it.

## Component map

```
┌─────────────┐      REST       ┌───────────────────┐
│  Angular    │ ───────────────▶│  Spring Boot API    │
│  (frontend) │ ◀─────────────── │  (control plane)    │
└─────────────┘                 └─────────┬───────────┘
                                           │ job dispatch (REST/queue table in Postgres)
                                           ▼
                                 ┌───────────────────┐
                                 │  Python Worker      │
                                 │  (PySpark + Delta)  │
                                 └─────┬──────┬────────┘
                     ┌─────────────────┘      └───────────────┐
                     ▼                                         ▼
           ┌─────────────────┐                       ┌─────────────────┐
           │ MinIO (bronze/    │                       │ PostgreSQL        │
           │ silver/gold,      │                       │ (app metadata,    │
           │ documents)        │                       │ pipeline registry,│
           └─────────────────┘                       │ prompts, serving   │
                                                        │ views)             │
           ┌─────────────────┐  ┌─────────────────┐  └─────────────────┘
           │ Neo4j (graph)     │  │ Qdrant (vectors)  │
           └─────────────────┘  └─────────────────┘
                                                        ┌─────────────────┐
                                                        │ Ollama (local LLM) │
                                                        └─────────────────┘

Observability (cross-cutting): OpenTelemetry SDKs in Spring Boot + Python worker →
Prometheus (metrics) + Grafana (eng dashboards); Angular has its own
app-level Observability Center reading aggregated data via Spring Boot, not Grafana directly.
```

## Ownership rules (fixed now to avoid drift later)

- **Spring Boot owns control, never bulk data.** It tracks pipeline *definitions* and *execution metadata* (status, duration, row counts, errors) in Postgres. It never pulls Gold-layer rows through itself for BI charts in bulk — Angular reads BI data from Postgres serving views/tables directly through a Spring Boot read endpoint, but Spring Boot does not run Spark.
- **Python worker owns all PySpark/Delta execution.** Triggered by Spring Boot (simple HTTP call or a `pipeline_runs` queue table it polls — decide in Phase 5, not now); reports status/metrics back to Spring Boot via callback/REST, not directly into Grafana.
- **MinIO is the only physical object store.** Bronze/Silver/Gold keep the existing key layout (`bronze/SalesLT/{table}/...`, `Silver/...`, `gold/...`) so the ported notebook logic needs no path rewriting beyond swapping `dbutils.fs` / `abfss://` for `s3a://` (Spark) or boto3 (document uploads).
- **Postgres is the only system of record for app metadata** (users, pipeline registry, execution history, dataset catalog metadata, prompt library + versions, audit log) — the Context Lake's "metadata catalog" is Postgres tables, not a separate database.
- **The Context Lake is a logical layer, not a new datastore.** It is Spring Boot APIs that join across Postgres (metadata), MinIO (files), Qdrant (semantic search) and Neo4j (lineage/relationships) — per spec §4's explicit instruction not to confuse it with the physical data lake.
- **Neo4j** holds only graph nodes/edges (business + technical entities) and provenance flags (source-fact vs inferred, per Image 3's solid/dotted edge convention). Sync direction is one-way for MVP: Postgres/MinIO facts → Neo4j, never the reverse.
- **Qdrant** holds only vectors + pointers (doc/dataset id) back to Postgres/MinIO — never the canonical text itself beyond what's needed for retrieval snippets.
- **Ollama** is called only through a provider interface in the Python worker (and/or Spring Boot for lightweight calls) so a future hosted-LLM swap doesn't touch business logic — per spec §5.2.

## What Phase 1 deliberately leaves open

- Job dispatch mechanism (sync HTTP vs. polling queue table) — decided in Phase 5 against real pipeline-execution needs, not speculatively now.
- Whether Spring Boot or the Python worker does embedding generation — decided in Phase 6 (Context Lake) based on what's actually simplest with Ollama's embedding models.
- Exact Postgres schema — drafted in Phase 3/4, not Phase 1.

## Azure compatibility seam (future phase only)

Kept thin on purpose: object storage access goes through a small storage-path abstraction (so `s3a://` → `abfss://` is a config change, consistent with the notebooks already using URI-style paths), and Ollama calls go through a provider interface. No Azure SDKs, Key Vault clients, or ADF-shaped abstractions are introduced now — per spec §5.2, over-building this seam before it's needed is explicitly discouraged.
