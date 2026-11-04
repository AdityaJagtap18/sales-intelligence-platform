# Databricks notebook source
# MAGIC %md
# MAGIC # Gold transform
# MAGIC
# MAGIC Carried over unchanged from the original `NB_Adventure_Works_Silver_to_Gold(_Delta)`
# MAGIC notebooks: rename every column from PascalCase to an underscore-segmented
# MAGIC form (insert `_` before an uppercase letter that immediately follows a
# MAGIC lowercase letter — e.g. `AddressLine1` -> `Address_Line1`; a run of
# MAGIC uppercase letters such as `ID` is left alone, matching the original
# MAGIC notebook's behavior exactly).

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


def rename_column(name: str) -> str:
    renamed = "".join(
        "_" + ch if ch.isupper() and name[i - 1].islower() else ch for i, ch in enumerate(name)
    )
    return renamed.lstrip("_")


# COMMAND ----------

for table in TABLES:
    df = spark.read.table(f"workspace.silver.{table}")
    for column in df.columns:
        df = df.withColumnRenamed(column, rename_column(column))

    df.write.format("delta").mode("overwrite").saveAsTable(f"workspace.gold.{table}")
    print(f"workspace.gold.{table}: {df.count()} rows, columns: {df.columns}")

# COMMAND ----------

dbutils.notebook.exit("gold_transform_complete")
