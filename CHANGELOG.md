# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## Versioning scheme

This project version tracks the phase-gated roadmap in
[docs/implementation/mvp-roadmap.md](docs/implementation/mvp-roadmap.md), not independent
feature releases:

- `0.<phase>.0` — minor bump on completion of each roadmap phase (Phase 1 → `0.1.0`, Phase 2 → `0.2.0`, …).
- `0.x.<patch>` — patch bump for fixes/adjustments made within an already-completed phase.
- `1.0.0` — reserved for completion of Phase 10 (local MVP fully validated).
- `2.0.0+` — reserved for the future Azure deployment phase.

The current version is tracked in [VERSION](VERSION).

## [Unreleased]

---

## [0.3.0] — Phase 3: Local Infrastructure

### Added
- `docker-compose.yml`: PostgreSQL, MinIO, Neo4j, Qdrant, Ollama, OpenTelemetry Collector,
  Prometheus, and Grafana, all with persistent named volumes and health checks.
- `.env.example` documenting every configurable port/credential/bucket name.
- `backend/spring-boot-api/Dockerfile` (multi-stage Maven build) so the API builds and runs
  inside the same Compose stack as its dependencies.
- Re-enabled the Spring Boot datasource/JPA autoconfiguration (disabled in Phase 2 since no
  Postgres existed) now pointed at the Compose Postgres via env vars; added
  `micrometer-registry-prometheus` and exposed `/api/actuator/prometheus` for scraping.
- Prometheus config scraping the OTel Collector, itself, and the Spring Boot API; Grafana
  provisioned with the Prometheus datasource (no dashboards yet — Phase 5).
- `docs/architecture/local-deployment.md` documenting ports, a known MinIO image
  substitution, and verified Phase 3 exit criteria.

### Changed
- **MinIO image substitution**: `minio/minio` was pulled from Docker Hub behind a login wall
  in 2025 (MinIO's licensing change). Switched to `bitnamilegacy/minio`, a frozen but free,
  unauthenticated, S3-API-compatible replacement, with `MINIO_DEFAULT_BUCKETS` creating the
  default bucket on first boot.

### Verified
- All 9 containers report healthy; Spring Boot's `/health` confirms a live Postgres
  connection; Prometheus reports all 3 scrape targets `health: up`; MinIO, Neo4j, Qdrant,
  Ollama, and Grafana are each independently reachable from the host.

---

## [0.2.0] — Phase 2: Application Foundation & Design System

### Added
- Angular 18 standalone application shell (`frontend/angular-app`): collapsible sidebar,
  topbar, and the full §6 navigation tree with lazy-loaded placeholder pages for every
  module not yet built.
- NEXORA global design tokens (`src/styles.scss`) derived from the primary reference image,
  per `docs/design/reference-analysis.md`.
- Reusable UI components: KPI card, panel, badge, data table, filter bar, area/line chart,
  horizontal bar chart, donut chart, and a shared SVG icon set.
- Executive Overview screen on labeled mock data (revenue trend with anomaly marker, AI
  insight card, revenue-by-channel, users-by-segment, recent anomalies table).
- Spring Boot 4 API skeleton (`backend/spring-boot-api`): actuator health/info endpoints,
  dev CORS for `localhost:4200`.

### Fixed
- Angular's default HTML sanitizer was silently stripping SVG child elements bound via
  `[innerHTML]` in the icon component, leaving every icon blank. Fixed by routing the
  (fixed, developer-authored) icon markup through `DomSanitizer.bypassSecurityTrustHtml`.

---

## [0.1.0] — Phase 1: Project Discovery & Repository Assessment

### Added
- Repository assessment of the existing Databricks notebooks, README, and Power BI report —
  documented the original Azure architecture and the exact Bronze→Silver→Gold transform logic
  to carry forward (`docs/discovery/repository-assessment.md`).
- Reference analysis of all five supplied UI images against the NEXORA primary visual
  authority, mapped to target modules (`docs/design/reference-analysis.md`).
- Architecture baseline, component ownership rules, and dependency matrix
  (`docs/architecture/system-architecture.md`, `docs/implementation/mvp-roadmap.md`).
- Four founding ADRs: local-first deployment, Neo4j for the knowledge graph, Ollama for
  local LLM inference, and the core technology stack rationale (`docs/decisions/`).
- Git repository initialized.
