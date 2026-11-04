# Deployment Guide (Phase 10)

A from-clean-clone walkthrough for standing up the full local stack — written so a new
engineer can follow it without any tribal knowledge of the earlier phases. For the
service/port reference table and the MinIO image substitution note, see
[`docs/architecture/local-deployment.md`](../architecture/local-deployment.md); this doc
covers the steps around it (Databricks setup, source data, pipeline deploy, model pulls,
running the test suites) that `local-deployment.md` predates.

## 1. Prerequisites

- Docker Desktop (or an equivalent Docker Engine + Compose v2).
- Java 17+ and the project's bundled Maven wrapper (`./mvnw`, no separate Maven install needed).
- Node.js 18+ and npm, for the Angular CLI (`npx ng`, no global install needed).
- Python 3.10+ for the local extraction/deploy scripts in `backend/python-worker`.
- A free [Databricks Free Edition](https://www.databricks.com/learn/free-edition) workspace —
  the one cloud-hosted component in this stack (ADR-005). Everything else runs locally.

## 2. Clone and configure

```bash
git clone <this repository>
cd Azure-End-to-End-Sales-Data-Analytics-Pipeline
cp .env.example .env
```

Edit `.env`:
- Pick ports that don't collide with anything already running on your host — the defaults
  in `.env.example` are what this repo's own dev machine landed on after a few conflicts
  (see `local-deployment.md` for which ones moved and why). If nothing else uses the
  standard ports, the `.env.example` defaults are fine as-is.
- Leave `DATABRICKS_HOST`/`DATABRICKS_TOKEN`/`DATABRICKS_JOB_ID`/`DATABRICKS_SQL_WAREHOUSE_ID`
  blank for now — step 4 fills them in once the workspace exists.
- Never commit `.env` — it's gitignored, and `.env.example` should never carry a real value
  in any of the credential fields.

## 3. Bring up the local Docker stack

```bash
docker compose up -d --build
docker compose ps
```

Everything should report `healthy` within about 30 seconds, except `sqlserver`, which takes
longer on first boot while it initializes its data directory. This starts: Postgres (app
metadata), SQL Server (the local **source** database SalesLT data is extracted from —
restored in step 5), MinIO (Context Lake document storage), Neo4j (knowledge graph), Qdrant
(vector search), Ollama (local LLM inference), the OTel Collector + Prometheus + Grafana
(observability), and the Spring Boot API itself.

## 4. Set up Databricks Free Edition

1. Create a workspace at [databricks.com/learn/free-edition](https://www.databricks.com/learn/free-edition).
2. Generate a personal access token: **User Settings → Developer → Access tokens**. Put the
   workspace URL and token into `.env` as `DATABRICKS_HOST`/`DATABRICKS_TOKEN`.
3. In Unity Catalog, create the `workspace.nexora` schema and a `raw_uploads` volume inside
   it — this is where step 6 uploads extracted Parquet files.
4. Note the id of an existing or newly created SQL Warehouse (Databricks Free Edition ships
   a Serverless Starter Warehouse) and put it in `.env` as `DATABRICKS_SQL_WAREHOUSE_ID`.

Full validation steps and the exact API calls this project checks against are in
[`docs/discovery/databricks-free-edition-assessment.md`](../discovery/databricks-free-edition-assessment.md).

## 5. Restore the source database

`infrastructure/sqlserver/AdventureWorksLT2022.bak` is **not** committed (it's large and
reproducible). Download the official backup from Microsoft's `sql-server-samples` GitHub
releases into `infrastructure/sqlserver/`, then let the one-shot restore service run:

```bash
docker compose up sqlserver-restore
```

`restore.sh` waits for SQL Server to accept connections, checks whether the database already
exists (safe to re-run), reads the backup's real logical file names via
`RESTORE FILELISTONLY` rather than hardcoding them, and restores all 10 `SalesLT` tables.

## 6. Extract, upload, and run the Bronze/Silver/Gold pipeline

```bash
cd backend/python-worker
python -m venv .venv && source .venv/bin/activate   # or .venv\Scripts\activate on Windows
pip install -r requirements.txt
python extract_to_databricks.py   # pulls all 10 SalesLT tables, uploads Parquet to the raw_uploads volume
python deploy_notebooks.py        # uploads the 3 notebooks and creates the nexora-bronze-silver-gold job
```

`deploy_notebooks.py` prints the created job's id — put it in `.env` as `DATABRICKS_JOB_ID`.
From here, triggering a pipeline run happens through the app itself (the Angular Pipeline
Runs page, or `POST /api/pipelines/run`), not by re-running this script — it's a one-time
deploy step, not part of the regular workflow.

## 7. Pull the local Ollama models

```bash
docker compose exec ollama ollama pull nomic-embed-text   # Context Lake embeddings (Phase 6)
docker compose exec ollama ollama pull llama3.2:1b        # Prompt Playground chat model (Phase 8)
```

## 8. Restart Spring Boot with the completed `.env`

```bash
docker compose up -d --build spring-boot-api
```

Confirm it's healthy: `curl http://localhost:<SPRING_API_PORT>/api/actuator/health` should
report `"status":"UP"` with the `db` component also `UP`.

## 9. Run the Angular app

```bash
cd frontend/angular-app
npm install
npx ng serve
```

Open `http://localhost:4200` — it should redirect to the Executive Overview and render real
`workspace.gold.*` numbers (not mock data) within a few seconds.

## 10. Run the test suites

```bash
cd backend/spring-boot-api
./mvnw test
```

The backend suite needs the Postgres container from step 3 reachable on the host — its
`spring.datasource.url` fallback (`application.properties`) defaults to
`localhost:${POSTGRES_PORT}`, which only resolves correctly if `.env`'s `POSTGRES_PORT`
matches what you actually mapped in step 2 (if you changed it from the `.env.example`
default, the fallback here assumes the default — pass `-Dspring.datasource.url=...` to
override, or just make sure nothing else on your host is already bound to the port you
picked). `AnalyticsServiceTest`/`PromptServiceTest`/`PipelineRunServiceTest`/
`CatalogSyncServiceTest` and friends run with the Databricks/Ollama/Neo4j clients mocked, so
they don't need Databricks credentials configured to pass — only `SpringBootApiApplicationTests`
(a full context load) needs the real Postgres connection.

```bash
cd frontend/angular-app
npx ng test --watch=false --browsers=ChromeHeadless
```

Needs a Chrome/Chromium binary on `PATH` (or `CHROME_BIN` set) for Karma's headless runner.

## Troubleshooting

- **Postgres auth failure on `./mvnw test` or `spring-boot:run`, but the app works fine
  inside Docker**: something else on your host is listening on the port
  `application.properties`'s datasource fallback assumes. The containerized app isn't
  affected (it connects via the `postgres` service's Docker-internal hostname, set
  explicitly by `docker-compose.yml`'s `SPRING_DATASOURCE_URL`), but a host-side run or test
  will try the fallback URL and may hit a different local Postgres instance with different
  credentials. Either free the port, or export `SPRING_DATASOURCE_URL` pointing at your
  actual mapped `POSTGRES_PORT` before running.
- **MinIO pull fails / `minio/minio` not found**: this project uses `bitnamilegacy/minio`
  instead of the official image — see the note in `local-deployment.md` for why.
- **`sqlserver-restore` exits immediately with nothing restored**: confirm
  `infrastructure/sqlserver/AdventureWorksLT2022.bak` actually exists at that path — it's
  gitignored and must be downloaded manually (step 5).
