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

## [0.7.0] — Phase 7 completed: Knowledge Graph (Neo4j sync, Graph Explorer)

### Added
- `com.nexora.platform.graph`: `GraphSyncService` populates Neo4j from
  `workspace.gold.*` (via the existing `DatabricksSqlClient` — no new
  Databricks access path) and from the `catalog_dataset`/`document` Postgres
  tables. Builds `(:Customer)-[:PLACED]->(:SalesOrder)-[:CONTAINS]->(:Product)
  -[:BELONGS_TO]->(:ProductCategory)`, `(:Customer)-[:HAS_ADDRESS]->(:Address)`,
  `(:ProductCategory)-[:SUBCATEGORY_OF]->(:ProductCategory)`,
  `(:Product|:ProductCategory)-[:SOURCED_FROM]->(:Dataset)`, and
  `(:Document)-[:MENTIONS]->(:Product|:ProductCategory)` from a plain
  substring match of Qdrant chunk text against known entity names (no new
  NLP dependency). Runs on a schedule and on demand via `POST /graph/sync`.
- `GraphQueryService`/`GraphController`: `GET /graph/stats`,
  `GET /graph/entities/{type}`, `GET /graph/entity/{type}/{id}` — a bounded
  Cypher traversal built from a fixed type map, never raw Cypher from Angular.
- `QdrantClient.scrollAll()`: single-page scroll over every chunk's payload,
  reused for the document-mentions match.
- Angular: `/intelligence/knowledge-graph` (node/relationship totals, a
  per-label breakdown, Sync Graph button) and `/intelligence/graph-explorer`
  (entity pickers, an SVG layered diagram laid out client-side via BFS,
  click-to-recenter) — replacing their Phase 2 placeholders.
- `neo4j-java-driver` dependency (plain Bolt driver, not Spring Data Neo4j —
  same explicit-Cypher style as `QdrantClient`).

### Fixed
- The first sync doubled every node (e.g. 1694 customers instead of 847):
  `MERGE` without a backing uniqueness constraint does a full node scan, so
  the scheduled sync firing 20s after startup raced a manual
  `POST /graph/sync` and both passed the "does this exist" check before
  either committed. Fixed with a `UNIQUE` constraint per label/id-property
  (`GraphSyncService.ensureConstraints()`), plus wiping the duplicated graph
  once before re-syncing.
- The Graph Explorer's traversal initially included `SOURCED_FROM`, which
  made any 2-hop traversal fan out to every node of that type (they all
  share one `Dataset` node) instead of a meaningful entity path. Excluded it
  from the explorer's traversal query; it stays visible on the Catalog/
  Lineage pages instead.

### Verified
- `POST /graph/sync` produced node counts matching the known Gold row counts
  exactly (847 customers, 450 addresses, 295 products, 41 categories, 32
  orders, 542 order lines, 1 document).
- Verified live in the browser: selecting a Sales Order renders its
  customer, address, and products with real `PLACED`/`HAS_ADDRESS`/
  `CONTAINS`/`BELONGS_TO` edges; clicking the customer node re-centers the
  explorer on that customer's own orders and address — an interactive
  multi-hop path over live data, not sample/seed data.

---

## [0.6.0] — Phase 6 completed: Context Lake (catalog, documents, lineage)

### Added
- `com.nexora.platform.contextlake`: `CatalogSyncService` syncs `catalog_dataset`
  (Postgres) from `workspace.gold.*` via Unity Catalog's `information_schema`
  plus a single dynamic row-count query; `CatalogController` exposes
  `GET /catalog/datasets` and `POST /catalog/refresh`.
- Document ingestion: `MinioDocumentStore` (raw file storage), `TextExtractor`
  (`.txt`/`.md`/PDF via Apache PDFBox), `TextChunker`, `OllamaEmbeddingClient`
  (`nomic-embed-text`, 768-dim), and `QdrantClient` (a plain REST wrapper, no
  gRPC client dependency) wired together by `DocumentIngestionService` and
  `DocumentSearchService`. `DocumentController`: `POST /documents`,
  `GET /documents`, `GET /documents/search`.
- `LineageService`/`LineageController`: renders the Source -> Bronze -> Silver
  -> Gold -> Catalog chain per table with live Databricks row counts.
- Angular: real pages for `/data-platform/catalog`, `/data-platform/context-lake`
  (upload + semantic search), and `/data-platform/lineage` (table picker + a
  visual 5-stage chain) — replacing their Phase 2 placeholders.
- Pulled `nomic-embed-text` into the existing local Ollama container.

### Fixed
- `.env`'s `MINIO_BUCKET` still held the pre-ADR-005 name
  (`adventureworks-lt-2022`) from before `.env.example` was updated in Phase
  3. Recreated MinIO with the correct `context-lake-documents` bucket and
  removed the stale empty one.
- Injecting a Spring-managed `ObjectMapper` failed —
  `spring-boot-starter-webmvc` alone doesn't register one the way
  `spring-boot-starter-web` used to. Worked around by constructing
  `ObjectMapper` directly where needed instead of chasing the auto-config gap.

### Verified
- Uploaded a real document through the Angular UI, searched for it by
  meaning (not keyword) and got the correct chunk back; refreshed the
  catalog and saw all 10 Gold tables with real metadata; picked a table in
  the Lineage page and saw all 5 stages render with matching row counts —
  all tested live in the browser, not just via curl.

---

## [0.5.0] — Phase 5 completed: Databricks orchestration + local observability

### Added
- Spring Boot Databricks control plane (`com.nexora.platform.databricks`):
  `DatabricksJobsClient` (run-now / runs-get) and `DatabricksSqlClient` (SQL
  Statement Execution API, used to read Gold/report data without DBFS or a
  JDBC driver). Credentials come from env vars and are never returned by any
  endpoint.
- `com.nexora.platform.pipeline`: `PipelineRun` entity + Postgres-backed
  history (`schema.sql`), and `PipelineController` (`POST /pipelines/run`,
  `GET /pipelines/runs`, `GET /pipelines/runs/{id}`) — the only way Angular
  touches the Databricks pipeline.
- `DataQualityMetricsPoller`: republishes the latest
  `workspace.nexora.data_quality_report` run as `nexora_data_quality_check`
  Prometheus gauges (1=PASS, 0.5=SKIPPED, 0=FAIL), so Grafana needs no second
  datasource for pipeline data quality.
- Grafana dashboard (`nexora-platform-health.json`): infra panels (API
  up/down, HTTP request rate, JVM heap) and pipeline panels (data quality
  pass rate, per-check table, pipeline-run-triggered rate).
- Angular: `PipelineApiService` and a real Pipeline Runs page
  (`/data-platform/pipeline-runs`, replacing its Phase 2 placeholder) — a
  "Run Pipeline" button and a polling run-history table with status badges.

### Verified
- Triggered a run from the Angular UI itself end-to-end: Spring Boot called
  Databricks `run-now`, persisted the run, and the UI's own polling (not a
  manual API call) showed it progress from `PENDING` to `TERMINATED` /
  `SUCCESS` live in the browser — done twice, for two separate runs.
- Confirmed in Prometheus that the data-quality gauges and the
  `/pipelines/run` request counter are scraped, and that the Grafana
  dashboard is provisioned with metric queries matching the real Micrometer
  tags (fixed one query that had a stray `/api` prefix on its `uri` label).

---

## [0.4.0] — Phase 4 completed: Databricks pipeline (Bronze/Silver/Gold)

### Added
- `backend/python-worker/extract_to_databricks.py`: local extraction of all 10 `SalesLT`
  tables from the SQL Server source container (via `python-tds`, chosen to avoid native
  ODBC driver installation) to Parquet, uploaded to the Unity Catalog volume
  `workspace.nexora.raw_uploads` via the Databricks Files API.
- `databricks/notebooks/01_bronze_ingest.py`, `02_silver_transform.py`,
  `03_gold_transform.py`: adapted Bronze→Silver→Gold notebooks running entirely on
  Databricks against Unity Catalog, per ADR-005. Silver adds the real data quality checks
  the original pipeline never had — primary-key null checks, full-row duplicate detection,
  foreign-key referential-integrity checks, and freshness — persisted to
  `workspace.nexora.data_quality_report` rather than just printed.
- `backend/python-worker/deploy_notebooks.py`: uploads the notebooks to the Databricks
  workspace and creates/updates a 3-task sequential job (`nexora-bronze-silver-gold`).
- Verified end-to-end: a triggered job run completes successfully and all 10
  `workspace.gold.*` Delta tables are queryable from the SQL Warehouse with correct row
  counts and column renaming; the data quality report shows 32 PASS / 2 SKIPPED / 0 FAIL.

### Fixed
- Workspace Import API returned 404 on first use — it does not auto-create nested parent
  directories; `deploy_notebooks.py` now calls `workspace/mkdirs` on the parent directory
  before `workspace/import`.
- SQL Server `uniqueidentifier` columns (e.g. `rowguid`) came back from `python-tds` as
  `uuid.UUID` objects; pyarrow wrote these as a Parquet `UUID` logical type that Spark's
  reader rejects (`PARQUET_TYPE_ILLEGAL`). Fixed by stringifying UUID-valued columns before
  writing Parquet.

---

## [0.3.1] — Phase 3 completed: Databricks connectivity + architecture pivot

### Changed
- **Architecture pivot (ADR-005)**: adopted a hybrid architecture directed by the user —
  Databricks Free Edition becomes the sole cloud-hosted component and takes over all
  Bronze/Silver/Gold processing, Delta Lake, and Unity Catalog. This supersedes the local
  PySpark + MinIO worker plan from the original Phase 4 kickoff prompt before any of that
  code was written. MinIO's role narrows to local documents/Context Lake files only.
  `docs/architecture/system-architecture.md`, ADR-001, and ADR-004 updated to match.
  Roadmap Phases 3–5 rescoped per the user's explicit mapping.

### Added
- Local SQL Server source service (`sqlserver` + one-shot `sqlserver-restore`) in
  `docker-compose.yml` — the *source* database for the local-extract → Databricks-upload
  ingestion path (ADR-005), not an app datastore.
- `infrastructure/sqlserver/restore.sh`: idempotent restore of the official
  `AdventureWorksLT2022.bak` (downloaded from Microsoft's `sql-server-samples` GitHub
  releases), reading the backup's actual logical file names via `RESTORE FILELISTONLY`
  rather than hardcoding them.
- `.gitattributes` forcing LF line endings on `*.sh`/`mvnw` so scripts don't break inside
  Linux containers regardless of a contributor's `git autocrlf` setting.
- `docs/discovery/databricks-free-edition-assessment.md`: live validation of every
  Databricks Free Edition constraint named in ADR-005 — auth, Clusters API, SQL Warehouse,
  Unity Catalog, Jobs API, and a full file upload/list/delete round-trip via the Files API.
  All confirmed working against the user's real workspace. Decided the local→Databricks
  upload mechanism (Unity Catalog Volumes + Files API, not DBFS).

### Fixed
- SQL Server's default non-root container user couldn't write to a Docker-Desktop-created
  named volume on first boot (`Access is denied` on `master.mdf`). Fixed with `user: root`
  on the `sqlserver` service.

### Verified
- All 10 `SalesLT` tables + 3 views present and queryable after restore.
- Databricks workspace: schema `workspace.nexora` and managed volume
  `workspace.nexora.raw_uploads` created; a real file was uploaded, listed, and deleted
  through the Files API.
- Databricks credentials (`DATABRICKS_HOST`/`DATABRICKS_TOKEN`) were read from `.env` and
  checked for presence/length only — never displayed, logged, or committed.

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
