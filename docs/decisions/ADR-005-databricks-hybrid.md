# ADR-005: Hybrid architecture — Databricks Free Edition as the sole cloud component

## Status
Accepted (supersedes the local-PySpark data engineering plan implied by ADR-001/ADR-004 for the processing layer only)

## Context
ADR-001 established local-first deployment for the whole platform, and the Phase 4 kickoff
prompt (see `docs/implementation/mvp-roadmap.md` history) planned a local PySpark + Delta
Lake worker against MinIO as the Bronze→Silver→Gold engine. The user has since directed a
hybrid architecture instead: Databricks Free Edition is the one cloud-hosted component,
and it specifically takes over the role MinIO+local-PySpark was going to play for
analytical processing. Everything else — Angular, Spring Boot, Postgres, Neo4j, Qdrant,
Ollama, OpenTelemetry/Prometheus/Grafana, Docker Compose — stays local, unchanged from
Phase 3.

The explicit reasoning (from the user's direction): preserve genuine use of Databricks —
the platform this project's source notebooks were originally written for — rather than
flattening that into a generic local Spark job. Free Edition makes that possible without
a paid subscription, which keeps ADR-001's "no required Azure/paid-cloud spend" intent
intact in spirit even though one managed service is now in the loop.

## Decision
- **Databricks Free Edition** is the data engineering execution plane: Bronze, Silver,
  and Gold processing, Delta Lake tables, Unity Catalog, notebook/job orchestration, and
  SQL Warehouse for analytical queries.
- **MinIO** is demoted to local documents and application-managed files only (Context
  Lake document uploads, etc.) — it must not duplicate Databricks' analytical storage.
  This directly reverses the Phase 3 compose setup's original intent for MinIO's bronze/
  silver/gold key layout; that layout is retired in favor of Databricks-managed storage
  and Unity Catalog.
- **Spring Boot remains the control plane.** It talks to Databricks only through
  supported authenticated APIs (Jobs API / SQL Warehouse connectivity), never exposes
  Databricks credentials to Angular, and keeps its own local record of execution history
  so the app can show past runs even when Databricks is unreachable.
- **Data ingestion path**: AdventureWorksLT2022 is restored to a local SQL Server
  container (already the plan — see the downloaded `.bak` under
  `infrastructure/sqlserver/`), tables are extracted locally, uploaded to Databricks, and
  processed there by the adapted Bronze→Silver→Gold notebooks. Direct Databricks-to-local-
  SQL-Server connectivity is not assumed and is evaluated separately, not built as a
  default path.
- **Free Edition constraints are validated before being relied on** — auth/API access,
  file upload mechanics, SQL Warehouse connectivity, job/notebook execution limits,
  storage, network restrictions, and fair-use limits. Nothing in this phase assumes paid
  Databricks features.
- **Observability is kept separate and honestly scoped per environment**: local container/
  app health through the existing OTel/Prometheus/Grafana stack; Databricks-side job
  status, duration, failures, and data-processed counts through whatever Databricks APIs
  Free Edition actually exposes — never fabricated infrastructure metrics Databricks
  doesn't expose at this tier.

## Consequences
- The Phase 4 kickoff prompt (local PySpark worker against MinIO) is superseded before
  any of that code was written — nothing to unwind.
- A real external dependency now exists: if Databricks Free Edition is unreachable (rate
  limits, outage, account issue), pipeline *execution* is blocked, but the app must still
  show historical run metadata from its own local Postgres record, per the master spec's
  "store execution metadata locally" requirement above.
- Setting up the Databricks Free Edition workspace itself (account creation, workspace
  provisioning, generating a personal access token) is an action only the user can take —
  this assistant does not create third-party accounts. See the open item tracked in
  `docs/implementation/mvp-roadmap.md` Phase 3/4.
- Credentials (Databricks host URL + PAT) are read from environment variables on the
  Spring Boot side only, following the same pattern as the Postgres credentials in `.env`
  — never entered into Angular, never hardcoded, never requested or typed in chat.
