# ADR-006: Drop the Azure redeployment phase — Databricks Free Edition remains the only cloud component

## Status
Accepted (supersedes the "Future: Azure redeployment" row in `mvp-roadmap.md`)

## Context
The original master spec's §11 held a "Future" phase for redeploying the platform to
Azure, kept explicitly out of scope until the local MVP (Phases 1-10) was complete. Phase
10 finished at `v1.0.0`, reaching that gate. The user has now directed that this project
not move to Azure at all — Databricks Free Edition should remain the only cloud-hosted
component, with everything else staying on the local Docker Compose stack established in
ADR-001 and carried through every phase since.

This isn't a new direction so much as making permanent what ADR-001 and ADR-005 already
established in practice: ADR-001 chose local-first specifically to avoid required Azure/
paid-cloud spend, and ADR-005 kept that intent "in spirit" by scoping Databricks Free
Edition as the one exception. The Azure row in the roadmap was a leftover from the
original master spec rather than a plan anyone had acted on.

## Decision
- The "Future: Azure redeployment" phase is dropped, not deferred. It will not be scoped,
  planned, or built as part of this project going forward.
- The hybrid architecture from ADR-005 — Databricks Free Edition for Bronze/Silver/Gold
  data engineering and SQL Warehouse queries; Angular, Spring Boot, Postgres, Neo4j,
  Qdrant, Ollama, and the OTel/Prometheus/Grafana stack all on local Docker Compose — is
  the platform's permanent target architecture, not a stepping stone to something else.
- Future work on this project is scoped as enhancements to the local+Databricks stack
  (new features, hardening, additional BI/AI capability) rather than infrastructure
  migration.

## Consequences
- `docs/implementation/mvp-roadmap.md`'s "Future" row is updated to reflect this — removed
  as a planned phase rather than left showing "⏳ (explicitly out of local MVP)", which
  implied it was still coming.
- No Azure-specific code, IaC, or deployment tooling should be added to this repository.
  If Azure is ever revisited, that would be a new decision superseding this one, not a
  resumption of the original spec's §11 plan.
- This does not affect anything already built: Databricks Free Edition remains exactly as
  scoped in ADR-005, and nothing about the local stack changes.
