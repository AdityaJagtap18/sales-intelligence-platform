# Phase 10 Security Review

A review of the stack built across Phases 1-9, focused on the areas the Phase 10 kickoff
called out: credential isolation, CORS, SQL injection surface, and secrets management.
Scope is the local MVP as it stands today (`v0.9.0`); Azure redeployment is explicitly out
of scope until this phase is done (per spec §11 / `mvp-roadmap.md`).

## 1. Credential isolation (ADR-005: Spring Boot is the sole control plane)

**Checked**: every `*Properties` record that holds a Databricks/MinIO/Qdrant/Ollama/Neo4j
host, token, username, or password (`DatabricksProperties`, `MinioProperties`,
`OllamaProperties`, `GraphProperties`, etc.) — confirmed none is referenced from any
`*Controller` class. Confirmed no `*Dto` record anywhere in the codebase has a field named
`token`/`password`/`secret`/`apiKey` (grepped case-insensitively across all DTOs).

**Checked live**: `GET /api/actuator/health` against the running container returns only
`{"db":{"details":{"database":"PostgreSQL","validationQuery":"isValid()"},"status":"UP"},...}`
— database name and a validation-query label, no username/password. `GET /api/actuator/env`
returns `404` — it's not in `management.endpoints.web.exposure.include` (only
`health,info,prometheus` are exposed), so there's no endpoint that could ever dump the
environment variables holding real credentials.

**Result**: no Databricks/MinIO/Qdrant/Ollama/Neo4j/Postgres credential has a path to reach
Angular or any HTTP response. This matches the architecture's stated invariant and nothing
found contradicts it.

## 2. CORS

`WebConfig.addCorsMappings` scopes `/**` to exactly `http://localhost:4200` and
`http://localhost:4201` (the two `ng serve` ports used in dev), explicit HTTP methods, and
does **not** set `allowCredentials(true)`. No wildcard origin, no production domain, nothing
broader than local dev. No other `CorsRegistry`/`@CrossOrigin` exists elsewhere in the
codebase.

## 3. SQL injection surface

Every Gold-layer query in `AnalyticsService`, `GraphSyncService`, `CatalogSyncService`,
`LineageService`, and `DataQualityMetricsPoller` is a `static final String` constant with no
runtime interpolation — the large majority of `DatabricksSqlClient.executeQuery()` call
sites pass one of these constants directly.

Two call sites build SQL dynamically and were checked individually:

- **`CatalogSyncService.fetchRowCounts`**: builds a `UNION ALL` query by interpolating table
  names — but those names come from `workspace.information_schema.columns` (Unity Catalog's
  own metadata, filtered to `table_schema = 'gold'`), not from any HTTP request. There is no
  path from user input into this string.
- **`LineageService.lineageFor(String table)`**: takes a path variable directly from
  `GET /lineage/{table}` and interpolates it into a `UNION ALL` query — but before that
  happens, it's matched case-insensitively against the fixed `SALESLT_TABLES` allowlist and
  the request throws `NoSuchElementException` (→ 404) for anything that doesn't match
  exactly. An attacker-supplied value can't reach the SQL string unless it's already one of
  the 10 known table names, at which point it's equivalent to the matched allowlist entry,
  not attacker-controlled content.

**Result**: no SQL injection path from Angular/HTTP input into a Databricks SQL statement.

## 4. Secrets management

- `.env` is listed in `.gitignore` and `git log --all --diff-filter=A --name-only` confirms
  it has never been committed in this repository's history.
- `.env.example` (tracked) and `docker-compose.yml`'s inline fallback defaults
  (`${POSTGRES_PASSWORD:-nexora_dev_password}` and similar) only ever contain placeholder
  local-dev values, consistently named to signal they're not meant for anything but a local
  Compose stack. `DATABRICKS_TOKEN` has **no** fallback default in either file — it must come
  from a real `.env`, so there's no risk of a stale/shared placeholder token ever being used
  by accident.
- No secret is logged: none of the REST clients (`DatabricksSqlClient`, `DatabricksJobsClient`,
  `OllamaChatClient`, `OllamaEmbeddingClient`, `QdrantClient`) log their request bodies or
  headers.

## 5. Found and fixed during this review

- **MinIO object-key construction from an unsanitized filename**
  (`DocumentIngestionService.ingest`): the uploaded file's original filename was concatenated
  directly into the MinIO object key (`UUID.randomUUID() + "-" + file.getOriginalFilename()`).
  A filename containing `/` or `\` couldn't escape the bucket (S3/MinIO object keys don't
  support directory traversal outside their own bucket), but it could produce confusing
  nested-looking keys or an unexpectedly deep key hierarchy. Fixed by stripping path
  separators from the filename before use (`sanitizeFilename`), so the key is always a flat
  name within the bucket.
- **Stale default Postgres port**: `application.properties`'s `spring.datasource.url`
  fallback hardcoded port `5432`, while `.env.example`'s actual `POSTGRES_PORT` is `5434`
  (remapped to avoid a conflict with another local Postgres instance on the reference dev
  machine). This isn't a security issue on its own, but it meant a host-side `mvnw test` or
  `mvnw spring-boot:run` would silently connect to whatever else was listening on `5432`
  rather than the intended Compose Postgres — worth fixing here since the same class of
  drift (a config default silently diverging from the real `.env`) is exactly what caused a
  real incident back in Phase 6 (the MinIO bucket name). Fixed by correcting the fallback to
  `5434` to match `.env.example`; the containerized app is unaffected either way since
  `docker-compose.yml` sets `SPRING_DATASOURCE_URL` explicitly for that service.

## 6. Out of scope for this pass

- Authentication/authorization: the app has no user accounts or login flow yet (not built in
  any phase so far) — there is nothing to review here until that exists; every endpoint is
  currently open to anything that can reach `localhost:8080`/`:8081`, which is an accepted
  state for a local single-user MVP, not a gap this review is closing.
- Dependency CVE scanning (`mvn dependency-check`, `npm audit`) — not run as part of this
  pass; worth adding as a follow-up if this moves toward the Azure redeployment phase.
- Rate limiting / abuse protection on any endpoint — not applicable to a local, single-user
  dev stack.
