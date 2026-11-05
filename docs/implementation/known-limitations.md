# Phase 3 — Known Limitations

Genuine external-service and sample-data limitations, documented rather than hidden behind
fabricated data.

## Sample data (AdventureWorksLT via Databricks Free Edition)

- **Single Order_Date across the sample.** Revenue Growth (Sales Dashboard) and period-over-period
  trends cannot be computed and are shown as "Not available" rather than 0% or a fabricated figure.
- **Forecasting requires >= 3 distinct calendar periods** to fit a trend; the sample normally has 1,
  so `/api/analytics/forecasting/generate` correctly returns `INSUFFICIENT_DATA` with the real
  reason and the minimum requirement, per the spec's "never present arbitrary generated values as
  real forecasts" rule.
- **No SalesTerritory table.** "Territory" throughout Sales Dashboard, Customer Analytics and
  Reports is Address.State_Province (via Customer -> CustomerAddress -> Address) — the closest real
  geographic dimension AdventureWorksLT has, not a fabricated field.
- **"Customer lifetime value"** is lifetime revenue within this dataset, not a projected/discounted
  CLV model — the UI and docs label it this way rather than implying a forecasting model that
  doesn't exist.

## Infrastructure not yet wired (relevant to later groups)

- **No log/trace backend** (Loki/Tempo/Jaeger/similar) is in `docker-compose.yml` — Logs & Traces
  (Group 3) will need to ship an honest "not configured" state rather than fake traces.
- **No authentication/authorization exists anywhere in the codebase today.** User Management
  (Group 5) requires a net-new minimal Spring Security setup — not a retrofit of hidden existing
  logic, since none exists.

## Data platform (Group 2)

- **Only two source types have a real connection to test.** This app holds exactly one Databricks
  SQL Warehouse connection and one Postgres connection; `DataSourceService#testConnection` pings
  those actual connections for sources of those types. Registering a source with type CSV/JSON/
  Parquet reports `NOT_APPLICABLE`, not a fabricated "connected" status, since those are file
  references rather than live connections this app holds credentials for.
- **Connection config is masked, never returned raw.** `DataSourceDto` redacts any field whose key
  contains password/token/secret/key before it reaches the frontend, so there is no API response
  that can leak a stored credential even to an authenticated caller — relevant ahead of Group 5's
  user management, where callers won't all be the same trust level.
- **Only one real Databricks job exists in this environment** (`DATABRICKS_JOB_ID`). The pipeline
  registry auto-seeds exactly one `PipelineDefinition` from it at startup; the API supports
  registering further definitions, but there is no second job in this architecture for one to
  point at, so that path is untested against a real run.
- **Data Quality has no standalone check-execution step.** Checks run inside the existing
  Bronze-to-Silver Databricks notebook and land in `workspace.nexora.data_quality_report` (the same
  table `DataQualityMetricsPoller` already polls into Prometheus). "Execute Checks" re-triggers that
  whole pipeline rather than running checks in isolation — there is no narrower real action to wire
  it to.

## Observability (Group 3)

- **Logs & Traces is this one process's own data, not distributed tracing.** No Loki/Tempo/Jaeger
  exists in `docker-compose.yml`. `InMemoryLogAppender` mirrors this Spring Boot process's real
  Logback events into a 500-entry ring buffer, and `HttpExchangeRepository` (Spring Boot Actuator's
  own model) keeps the last 200 real HTTP request/response exchanges this API has handled. Both are
  genuine, just scoped to one process — there is no trace propagation across Angular, Spring Boot,
  Databricks and the other services, and the UI says so rather than implying full tracing exists.
- **Metrics has no historical store of its own.** `MetricsController` reads the live Micrometer
  `MeterRegistry` directly (the same one Prometheus already scrapes via
  `management.prometheus.metrics.export`), so the explorer shows only the current sample value per
  series — Prometheus itself, not this app, is where history would come from.
- **Alerts only cover the four real conditions this app can actually observe**: non-SUCCESS
  pipeline runs, failing data-quality checks, high-confidence anomalies (>= 70% confidence, reusing
  `AnalyticsService#anomalies()`), and DOWN system-health components. There is no generic
  user-defined alert-rule builder — adding a fifth source means adding a fifth real check, not a
  rule engine over unrelated data.
- **Alert acknowledgement has no real user identity to attribute to.** Since no authentication
  exists yet (Group 5 adds it), "Acknowledge" records `observability-ui` rather than inventing a
  named user.
- **System Health's Databricks check reports `NOT_CONFIGURED`, not `DOWN`,** when
  `DATABRICKS_HOST`/`DATABRICKS_SQL_WAREHOUSE_ID` are unset — an intentionally-skipped cloud
  dependency (ADR-005 is opt-in per environment) is a different state from a real outage.

## AI Insights (Group 4)

- **Grounding threshold is a fixed heuristic.** Chunks below a 0.3 cosine-similarity score from
  Qdrant are treated as not relevant; this is a reasonable cutoff for `nomic-embed-text`, not a
  tuned/validated value — the UI reports the best score it saw so this is inspectable, not hidden.
- **"Related entities" depend on the Knowledge Graph sync having run for that document.** Context
  Lake documents are only linked into Neo4j by `GraphSyncService`'s own scheduled sync
  (`graph.sync-interval-ms`); a newly-indexed document may show no related entities until that sync
  next runs — this returns an empty list, not an error, and is not presented as "this document
  mentions nothing."
- **Answer quality depends on the local Ollama model** (`ollama.chat-model`, default
  `llama3.2:1b` — a small model chosen to run without a GPU). It is prompted to answer only from the
  numbered sources and admit when they don't contain the answer, but there is no separate
  fact-checking pass verifying it actually did so.

## Administration (Group 5)

- **No Spring Security dependency is cached in this offline Maven build**, and this sandbox has no
  network access to fetch one (`./mvnw -o` is offline-only). User Management therefore provides
  real, persisted users, roles and credential verification — passwords are hashed with salted
  PBKDF2 (`javax.crypto`, 120,000 iterations, JDK built-in, never plaintext or a reversible
  encryption) — but **there is no HTTP-layer login, session, or filter chain enforcing
  authentication on any endpoint**, including `/api/users` itself. Retrofitting that across every
  existing Angular `HttpClient` call (which currently sends no credentials anywhere) is a
  substantially larger, riskier change than adding a user-administration CRUD page, so it is
  explicitly out of scope here rather than half-implemented. `POST /api/users/login` genuinely
  verifies credentials against the stored hash and reports success/failure; it is just not wired
  into anything that gates access yet.
- **A default `admin` account is seeded** at startup (`UserService#seedDefaultAdmin`) with a
  fixed dev-only password, following the same pattern as this project's existing default
  Postgres/MinIO/Neo4j dev credentials in `docker-compose.yml`/`.env.example` — not a production
  credential.
- **Settings is read-only.** Every value shown is bound at Spring Boot startup from environment
  variables; there is no runtime config store, so the page doesn't offer an edit control that
  wouldn't actually take effect without restarting the backend.
- **Integrations adds no new backend or tracking system.** It is a frontend composition of System
  Health's connectivity checks and the Data Sources registry — both already built in Groups 2–3 —
  specifically to avoid a third, redundant "is this thing connected" implementation.

## Forecasting implementation choice

The spec's forecasting section says to "prefer a Python service or worker if the existing
architecture supports it." `backend/python-worker` is a batch ingestion script invoked manually
during Phase 4 setup (`extract_to_databricks.py`, `deploy_notebooks.py`) — it is not a running
service Spring Boot could call at request time, and no HTTP server exists in it. Adding one would
be new infrastructure for a single feature when the forecasting method needed (least-squares linear
regression) has no dependency that Java lacks. Forecasting is implemented directly in
`ForecastingService` (Spring Boot) instead, consistent with "use an appropriate forecasting method
based on data availability."
