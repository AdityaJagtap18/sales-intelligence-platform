# ADR-004: Core technology stack

## Status
Accepted

## Context
The master spec (§5) prescribes a full stack. This ADR records the rationale, not just the list, so later phases don't re-debate settled choices.

## Decision

| Layer | Choice | Why |
|---|---|---|
| Frontend | Angular + TypeScript | Spec requirement; also a natural fit for a dense, navigation-heavy enterprise shell (strong routing/forms/DI story) matching Image 1's structure |
| Backend control plane | Java Spring Boot | Spec requirement; mature REST/security/JPA ecosystem for the metadata-heavy "control, not compute" role defined in system-architecture.md |
| ETL / AI workloads | Python | Spec requirement; required anyway for PySpark and the Ollama/embedding ecosystem |
| Analytical processing | Databricks Free Edition (PySpark + Delta Lake, Unity Catalog) | Superseded from a local-PySpark plan by ADR-005: preserves genuine Databricks usage — the platform the original notebooks already targeted — instead of flattening it into a generic local Spark job. The notebooks already target Delta in the `_Delta` variants, so the transform logic itself still needs no rewrite, only a new host. |
| App database | PostgreSQL | Relational metadata (users, pipeline registry, prompts, audit) with no need for Spark-scale storage |
| Object storage (local documents only) | MinIO | S3-compatible local store for Context Lake documents/application files. Per ADR-005 its originally-planned bronze/silver/gold analytical role moved to Databricks-managed storage/Unity Catalog — it no longer stands in for ADLS Gen2 in that sense. |
| Knowledge graph | Neo4j | See ADR-002 |
| Vector database | Qdrant | Lightweight, self-hosted, good local Docker story for the Context Lake's semantic search |
| Local LLM | Ollama | See ADR-003 |
| Observability | OpenTelemetry → Prometheus → Grafana, plus structured logs | Spec requirement; keeps engineering-level observability (Grafana) separate from the application-level Observability Center (Angular/Spring Boot), as the spec explicitly requires |
| Infra | Docker Compose | Sufficient for a local-first single-host MVP; Kubernetes would be premature complexity per engineering principle #3 |

## Consequences
- Seven backing services (Postgres, MinIO, Neo4j, Qdrant, Ollama, Prometheus, Grafana) plus three app processes (Angular, Spring Boot, Python worker) run via Docker Compose — real but not excessive for what's a legitimate multi-capability platform, not a simple CRUD app.
- Hardware/resource constraints (Ollama model size, Spark driver memory in a single local JVM) will be validated in Phase 3 before committing to default model/cluster sizing; if local hardware can't sustain all services simultaneously, the fallback is documented in Phase 3, not decided speculatively here.
