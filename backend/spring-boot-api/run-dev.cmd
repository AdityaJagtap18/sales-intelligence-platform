@echo off
rem Loads the Databricks env vars from the repo-root .env (docker-compose reads
rem these via env_file, but `mvnw spring-boot:run` run directly does not) and
rem then starts the Spring Boot dev server. Only Databricks vars are pulled in
rem here -- the Postgres/MinIO/Qdrant/Neo4j/Ollama defaults in
rem application.properties already target localhost for this exact use case.
setlocal enabledelayedexpansion

if exist "..\..\.env" (
  for /f "usebackq tokens=1,* delims==" %%A in ("..\..\.env") do (
    if "%%A"=="DATABRICKS_HOST" set "DATABRICKS_HOST=%%B"
    if "%%A"=="DATABRICKS_TOKEN" set "DATABRICKS_TOKEN=%%B"
    if "%%A"=="DATABRICKS_JOB_ID" set "DATABRICKS_JOB_ID=%%B"
    if "%%A"=="DATABRICKS_SQL_WAREHOUSE_ID" set "DATABRICKS_SQL_WAREHOUSE_ID=%%B"
  )
)

call .\mvnw.cmd spring-boot:run
