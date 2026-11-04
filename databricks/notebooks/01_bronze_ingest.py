# Databricks notebook source
# MAGIC %md
# MAGIC # Bronze ingestion
# MAGIC
# MAGIC Reads the Parquet files `extract_to_databricks.py` uploaded to
# MAGIC `/Volumes/workspace/nexora/raw_uploads/{table}/{table}.parquet` and writes
# MAGIC each one as a managed Delta table under `workspace.bronze`, unmodified.
# MAGIC
# MAGIC This mirrors the original pipeline's bronze layer: ADF's copy activity
# MAGIC also landed raw SalesLT data with no transformation applied — the
# MAGIC transform logic starts in Silver, not here.

# COMMAND ----------

TABLES = [
    "Address",
    "Customer",
    "CustomerAddress",
    "Product",
    "ProductCategory",
    "ProductDescription",
    "ProductModel",
    "ProductModelProductDescription",
    "SalesOrderDetail",
    "SalesOrderHeader",
]

RAW_VOLUME = "/Volumes/workspace/nexora/raw_uploads"

# COMMAND ----------

results = []
for table in TABLES:
    input_path = f"{RAW_VOLUME}/{table}/{table}.parquet"
    df = spark.read.parquet(input_path)
    row_count = df.count()

    df.write.format("delta").mode("overwrite").saveAsTable(f"workspace.bronze.{table}")
    results.append((table, row_count))
    print(f"workspace.bronze.{table}: {row_count} rows")

# COMMAND ----------

# MAGIC %md
# MAGIC ## Result summary
# MAGIC Printed above per table. The orchestrating job checks this notebook's exit
# MAGIC status; a non-zero row count for every table is this step's informal
# MAGIC success signal, with real validation deferred to the Silver step's data
# MAGIC quality checks.

# COMMAND ----------

assert all(count > 0 for _, count in results), "One or more bronze tables ingested zero rows"
dbutils.notebook.exit("bronze_ingest_complete")
