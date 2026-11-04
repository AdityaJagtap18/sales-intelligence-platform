# Repository Assessment

## What exists today

```
Azure-End-to-End-Sales-Data-Analytics-Pipeline/
├── README.md
├── Databricks Notebooks/
│   ├── Storage Mount/NB_Adventure_works_Storagemount.ipynb
│   ├── Bronze to Silver/
│   │   ├── NB_Adventure_Works_Bronze_to_Silver.ipynb        (parquet variant)
│   │   └── NB_Adventure_Works_Bronze_to_Silver_Delta.ipynb  (delta variant)
│   └── Silver to Gold/
│       ├── NB_Adventure_Works_Silver_to_Gold.ipynb          (parquet variant)
│       └── NB_Adventure_Works_Silver_to_Gold_Delta.ipynb    (delta variant)
└── Power BI Report/AdventureWorksLT2022.pbix
```

Not a git repository yet (no `.git`). No source database, no Azure resources, no `.bak` file for AdventureWorksLT2022 are present — everything described in the README as "Azure services" is gone except these artifacts.

## Original architecture (from README + notebooks)

SQL Server (AdventureWorksLT2022, schema `SalesLT`) → Azure Data Factory (Copy pipeline, on-prem IR) → ADLS Gen2 `bronze/SalesLT/{table}/{table}.parquet` → Databricks (mount via passthrough token) → Bronze→Silver→Gold transforms → Synapse serverless/dedicated SQL (stored proc builds a view per gold table) → Power BI (DirectQuery).

## Notebook-by-notebook logic (this is the reusable business logic)

**Storage Mount notebook** — Azure-specific only. Mounts `abfss://adventureworks-lt-2022@...dfs.core.windows.net/` using Databricks credential passthrough. **Not portable** — local implementation needs an equivalent "object storage access" concept, satisfied by MinIO + boto3/s3fs, not a literal mount.

**Bronze → Silver** (both variants, parquet and delta, logic is identical except the write format):
1. List tables under `bronze/SalesLT/*`.
2. For each table, read `bronze/SalesLT/{table}/{table}.parquet`.
3. Any column with "date"/"Date" in its name: cast to timestamp, convert UTC → formatted string `yyyy-MM-dd`.
4. Write to `Silver/SalesLT/{table}` (parquet or delta).
No dedup, no null-handling, no schema validation — this is the gap the spec's "Silver layer" quality rules need to fill in the local rebuild.

**Silver → Gold** (both variants):
1. List tables under `Silver/SalesLT/*`.
2. For each table, read it.
3. Rename every column from PascalCase to a convention that inserts `_` before an uppercase letter that follows a lowercase letter (e.g. `AddressLine1` → `Address_Line1`, `CustomerID` stays `Customer_ID`... actually `ID` has no lowercase-before-uppercase transition inside it, so acronyms are left alone — verified against the regex-equivalent logic in the cell).
4. Write to `gold/SalesLT/{table}`.
No business aggregation yet in the gold layer as shipped — "gold" here just means schema-normalized copies of Silver, not the summary tables (sales summary, customer performance, etc.) the master spec calls for. Those aggregates are new work, not a port.

**Tables touched** (inferred from AdventureWorksLT2022 standard schema + the two tables explicitly read back in the notebooks — `SalesOrderDetail`, `Product`, `Address`): `Address`, `Customer`, `CustomerAddress`, `Product`, `ProductCategory`, `ProductDescription`, `ProductModel`, `ProductModelProductDescription`, `SalesOrderDetail`, `SalesOrderHeader`.

## Power BI report

`AdventureWorksLT2022.pbix` is binary; per the README it connects via DirectQuery to the Synapse SQL views. Cannot be inspected without Power BI Desktop. **Decision needed later** (Phase 9): either (a) keep it as a legacy artifact and point it at the new Postgres/Delta serving layer via an ODBC/Postgres connector, or (b) treat Angular as the sole BI surface and leave the .pbix untouched as a historical reference. Recommendation: (b) — repointing DirectQuery to a new local source is low value versus building the Angular BI module the spec already requires.

## Missing / to be rebuilt locally

| Original component | Local replacement |
|---|---|
| SQL Server (AdventureWorksLT2022) | Same DB, run locally via Docker (`mcr.microsoft.com/mssql/server`) seeded from the public AdventureWorksLT install script — this becomes the "Data Sources" ingestion target, not the app DB |
| Azure Data Factory (copy pipeline, trigger) | Python worker job (local pipeline execution engine), Spring Boot controls scheduling |
| ADLS Gen2 (bronze/silver/gold folders) | MinIO buckets, same `bronze/SalesLT/...` key layout |
| Azure Databricks (PySpark + mount) | Local PySpark (or pandas, see ADR-004) running the same transform logic against MinIO via s3a, no mount step needed |
| Azure Key Vault | Local `.env` / Docker secrets for MVP |
| Azure Synapse (stored proc → SQL views) | PostgreSQL materialized views or plain tables populated by the Python worker after Gold is written |
| Power BI (DirectQuery) | Angular BI module (primary); .pbix preserved as-is |

## Reusable logic to carry forward as-is

- Date-column normalization rule (Bronze→Silver).
- PascalCase → underscore-segmented column rename rule (Silver→Gold).
- Per-table iteration pattern (list tables in a layer, process each independently) — maps directly onto the "Pipeline Stages" / stage-level execution concept in the master spec.

## Scope decision for local MVP

Rebuild Bronze→Silver→Gold using the **Delta** variants (not plain parquet) as the base, since the master spec already commits to PySpark + Delta Lake, and keep the parquet-variant notebooks only as preserved historical reference under a `legacy/` folder — do not maintain two parallel transform code paths going forward.
