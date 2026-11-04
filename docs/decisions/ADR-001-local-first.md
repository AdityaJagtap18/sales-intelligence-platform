# ADR-001: Local-first deployment, Azure deferred

## Status
Accepted, qualified by ADR-005 — Databricks Free Edition is now the one intentional
exception to "entirely local" (chosen specifically to preserve genuine Databricks usage,
not as drift back toward a paid cloud dependency). Every other decision below still holds.

## Context
The original Azure subscription and all deployed resources (ADF, ADLS Gen2, Databricks workspace, Synapse, Key Vault) no longer exist. Rebuilding on Azure first would require provisioning a paid subscription before any functional progress is visible, and would block development on cloud resource availability.

## Decision
Build the entire platform to run locally via Docker Compose first (PostgreSQL, MinIO, Neo4j, Qdrant, Ollama, OTel/Prometheus/Grafana, Spring Boot, Angular, Python worker). Azure redeployment is a distinct future phase, not part of the MVP.

## Consequences
- Every local service must be swappable via config/env vars (object storage endpoint, LLM provider, etc.) so the eventual Azure port doesn't require rewriting business logic — but no Azure-specific abstraction is built until that phase actually starts.
- The team gets a fully working, demonstrable product without any cloud spend or access.
- Some Azure-specific original behavior (ADLS mount via credential passthrough, ADF triggers, Synapse stored procs) will be re-implemented with local equivalents rather than literally ported (see repository-assessment.md).
