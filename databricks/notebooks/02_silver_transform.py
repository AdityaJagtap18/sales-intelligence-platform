# Databricks notebook source
# MAGIC %md
# MAGIC # Silver transform + data quality
# MAGIC
# MAGIC Two things happen here, both ported/extended from the original project:
# MAGIC
# MAGIC 1. **Date normalization** — carried over unchanged from the original
# MAGIC    `NB_Adventure_Works_Bronze_to_Silver(_Delta)` notebooks: any column whose
# MAGIC    name contains "date" is cast to a timestamp and reformatted as
# MAGIC    `yyyy-MM-dd`.
# MAGIC 2. **Real data quality checks** — the original notebooks had *none*. This
# MAGIC    is the explicitly named gap from the master spec (Module C) and from
# MAGIC    `docs/discovery/repository-assessment.md`: null checks on primary keys,
# MAGIC    full-row duplicate detection, a handful of real foreign-key
# MAGIC    referential-integrity checks, and freshness (non-empty tables). Results
# MAGIC    land in `workspace.nexora.data_quality_report`, not just printed and
# MAGIC    discarded, so Phase 5's observability work has something real to show.

# COMMAND ----------

from datetime import datetime, timezone

from pyspark.sql import DataFrame
from pyspark.sql.functions import col, date_format, from_utc_timestamp, lit
from pyspark.sql.types import TimestampType

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

# First column of each table is treated as its primary key for the null-check
# below. True for every SalesLT table except the two composite-key junction
# tables (CustomerAddress, ProductModelProductDescription), which are called
# out explicitly rather than silently mis-checked.
PRIMARY_KEY = {
    "Address": "AddressID",
    "Customer": "CustomerID",
    "Product": "ProductID",
    "ProductCategory": "ProductCategoryID",
    "ProductDescription": "ProductDescriptionID",
    "ProductModel": "ProductModelID",
    "SalesOrderDetail": "SalesOrderDetailID",
    "SalesOrderHeader": "SalesOrderID",
}

# A representative (not exhaustive) set of real foreign-key relationships to
# verify referential integrity against — enough to demonstrate the check
# works, not a full constraint re-implementation of the whole schema.
FOREIGN_KEYS = [
    ("SalesOrderDetail", "ProductID", "Product", "ProductID"),
    ("SalesOrderDetail", "SalesOrderID", "SalesOrderHeader", "SalesOrderID"),
    ("Product", "ProductCategoryID", "ProductCategory", "ProductCategoryID"),
    ("CustomerAddress", "CustomerID", "Customer", "CustomerID"),
]

# COMMAND ----------


def normalize_dates(df: DataFrame) -> DataFrame:
    for field in df.columns:
        if "date" in field.lower():
            df = df.withColumn(
                field,
                date_format(from_utc_timestamp(col(field).cast(TimestampType()), "UTC"), "yyyy-MM-dd"),
            )
    return df


# COMMAND ----------

quality_rows = []
checked_at = datetime.now(timezone.utc)


def record(table: str, check: str, status: str, details: str) -> None:
    quality_rows.append((table, check, status, details, checked_at))


silver_frames: dict[str, DataFrame] = {}

for table in TABLES:
    df = spark.read.table(f"workspace.bronze.{table}")
    df = normalize_dates(df)
    silver_frames[table] = df
    df.write.format("delta").mode("overwrite").saveAsTable(f"workspace.silver.{table}")

    row_count = df.count()
    record(table, "freshness_non_empty", "PASS" if row_count > 0 else "FAIL", f"{row_count} rows")

    pk = PRIMARY_KEY.get(table)
    if pk:
        null_pk_count = df.filter(col(pk).isNull()).count()
        record(
            table,
            "primary_key_not_null",
            "PASS" if null_pk_count == 0 else "FAIL",
            f"{null_pk_count} rows with null {pk}",
        )
    else:
        record(table, "primary_key_not_null", "SKIPPED", "composite key table, not checked by this rule")

    duplicate_count = row_count - df.dropDuplicates().count()
    record(
        table,
        "no_full_row_duplicates",
        "PASS" if duplicate_count == 0 else "FAIL",
        f"{duplicate_count} duplicate rows",
    )

# COMMAND ----------

for child_table, child_col, parent_table, parent_col in FOREIGN_KEYS:
    child_df = silver_frames[child_table]
    parent_df = silver_frames[parent_table]

    orphaned = (
        child_df.select(col(child_col).alias("fk"))
        .filter(col("fk").isNotNull())
        .join(parent_df.select(col(parent_col).alias("fk")), on="fk", how="left_anti")
        .count()
    )
    record(
        f"{child_table}->{parent_table}",
        "referential_integrity",
        "PASS" if orphaned == 0 else "FAIL",
        f"{orphaned} {child_table} rows reference a missing {parent_table}.{parent_col}",
    )

# COMMAND ----------

report_df = spark.createDataFrame(
    quality_rows, schema=["table_name", "check_name", "status", "details", "checked_at"]
).withColumn("run_id", lit(checked_at.isoformat()))

report_df.write.format("delta").mode("append").saveAsTable("workspace.nexora.data_quality_report")

report_df.show(truncate=False)

# COMMAND ----------

failures = [r for r in quality_rows if r[2] == "FAIL"]
if failures:
    # Intentionally does not fail the job: the quality report is recorded
    # either way (Phase 5 observability needs failures visible, not hidden),
    # and these are advisory checks on sample data, not hard pipeline gates.
    print(f"WARNING: {len(failures)} data quality check(s) failed — see workspace.nexora.data_quality_report")

dbutils.notebook.exit("silver_transform_complete")
