# Databricks Free Edition — Connectivity & Constraints Assessment (Phase 3)

Validated live against the user's actual Free Edition workspace via its REST API, using
`DATABRICKS_HOST`/`DATABRICKS_TOKEN` from `.env` (never displayed or logged). Every result
below is from a real API call made during this session, not assumed.

## Results

| Constraint (per ADR-005) | Result | Notes |
|---|---|---|
| Authentication (PAT) | ✅ Works | `GET /api/2.0/preview/scim/v2/Me` returns `200` with the authenticated user's identity. |
| Clusters API | ✅ Reachable | `GET /api/2.1/clusters/list` returns `200` with an empty list — Free Edition's workspace has no classic all-purpose clusters provisioned, consistent with Free Edition leaning on serverless compute rather than classic clusters. Not a blocker: nothing in the plan requires a classic cluster. |
| SQL Warehouse | ✅ Available | A **Serverless Starter Warehouse** already exists in the workspace (`PRO` type, `enable_serverless_compute: true`, `auto_resume: true`, state `STOPPED` when idle). This is exactly the SQL Warehouse connectivity Spring Boot will use for Gold-layer queries (Module G/H). |
| Unity Catalog | ✅ Available | A `workspace` managed catalog already exists. Created and verified a new schema `workspace.nexora` and a managed volume `workspace.nexora.raw_uploads` for this project — both succeeded (`200`). |
| Jobs API | ✅ Reachable | `GET /api/2.2/jobs/list` returns `200` with an empty list (no jobs defined yet — expected, Phase 4 work). |
| File upload mechanism | ✅ Confirmed end-to-end | Used the **Files API** (`PUT /api/2.0/fs/files/Volumes/...`) to upload a test file into the new Unity Catalog volume, listed it back (`GET /api/2.0/fs/directories/...`), and deleted it (`DELETE`) — all three succeeded (`204`/`200`/`204`). This is the local→Databricks upload path Phase 4 will use for extracted `SalesLT` tables. |
| Network restrictions | ✅ None encountered | Every call above succeeded from this machine with no VPN/allowlist needed. |
| Fair-use / resource limits | ⏳ Not yet stress-tested | Nothing in this assessment approached any rate limit. Real limits (serverless compute minutes, storage quotas) will only show up under actual Phase 4 workload — noted as a Phase 4 risk to watch, not a blocker now. |

## Decision: local → Databricks upload mechanism

**Unity Catalog Volumes + Files API**, not DBFS. Reasons:
- Already proven to work end-to-end above.
- Unity Catalog volumes are the modern, governed storage primitive (vs. legacy DBFS root,
  which Databricks itself is deprecating for new work) and integrate directly with the
  Bronze-layer notebook's table-by-table read loop — a volume path
  (`/Volumes/workspace/nexora/raw_uploads/...`) drops into the existing
  `spark.read.parquet(...)`-style code with minimal change from the original
  `/mnt/adventureworks-lt-2022/...` DBFS-mount path.
- Keeps credentials and the upload call entirely inside Spring Boot (per ADR-005's "never
  expose Databricks credentials to Angular" rule) — Angular never talks to Databricks
  directly.

## What's created in the workspace as a result of this assessment

- Schema `workspace.nexora` — the project's working schema going forward (Bronze/Silver/Gold
  tables land here in Phase 4, not in a throwaway/test schema).
- Managed volume `workspace.nexora.raw_uploads` — the landing zone for locally-extracted
  `SalesLT` table files before Databricks notebooks process them.
- The test file used to prove the upload path was deleted; both the schema and volume
  themselves are kept as Phase 4's actual working resources, not torn down.

## Exit criteria (per the Phase 3 kickoff prompt) — met

Every constraint in ADR-005's list has a documented, verified answer, and the local→Databricks
upload mechanism is decided (Unity Catalog Volumes + Files API). Phase 3 is now fully
complete — local infra (done earlier) and Databricks connectivity (done here).
