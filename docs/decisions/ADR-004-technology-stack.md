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
| Analytical processing | PySpark + Delta Lake | Directly continues the existing notebooks' approach (they already target Delta in the `_Delta` variants) — least rewrite, most continuity with the original project |
| App database | PostgreSQL | Relational metadata (users, pipeline registry, prompts, audit) with no need for Spark-scale storage |
| Object storage | MinIO | S3-compatible local stand-in for ADLS Gen2; same bucket/key layout as the original bronze/silver/gold folders |
| Knowledge graph | Neo4j | See ADR-002 |
| Vector database | Qdrant | Lightweight, self-hosted, good local Docker story for the Context Lake's semantic search |
| Local LLM | Ollama | See ADR-003 |
| Observability | OpenTelemetry → Prometheus → Grafana, plus structured logs | Spec requirement; keeps engineering-level observability (Grafana) separate from the application-level Observability Center (Angular/Spring Boot), as the spec explicitly requires |
| Infra | Docker Compose | Sufficient for a local-first single-host MVP; Kubernetes would be premature complexity per engineering principle #3 |

## Consequences
- Seven backing services (Postgres, MinIO, Neo4j, Qdrant, Ollama, Prometheus, Grafana) plus three app processes (Angular, Spring Boot, Python worker) run via Docker Compose — real but not excessive for what's a legitimate multi-capability platform, not a simple CRUD app.
- Hardware/resource constraints (Ollama model size, Spark driver memory in a single local JVM) will be validated in Phase 3 before committing to default model/cluster sizing; if local hardware can't sustain all services simultaneously, the fallback is documented in Phase 3, not decided speculatively here.
