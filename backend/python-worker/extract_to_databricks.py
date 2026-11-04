"""
Phase 4 ingestion step 1: extract AdventureWorksLT2022 SalesLT tables from the
local SQL Server source (see docker-compose.yml `sqlserver` service, restored
by infrastructure/sqlserver/restore.sh) and upload each as Parquet into the
Databricks Unity Catalog volume `workspace.nexora.raw_uploads`.

This is the local side of ADR-005's ingestion path: restore locally -> extract
-> upload to Databricks -> process there. Everything past the upload step
(Bronze/Silver/Gold) runs as Databricks notebooks, not here.

Usage:
    python extract_to_databricks.py
"""
from __future__ import annotations

import io
import os
import sys
import uuid
from pathlib import Path

import pandas as pd
import pytds
import requests
from dotenv import load_dotenv

REPO_ROOT = Path(__file__).resolve().parents[2]
load_dotenv(REPO_ROOT / ".env")

SALESLT_TABLES = [
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

VOLUME_PATH = "/Volumes/workspace/nexora/raw_uploads"


def sql_server_connection():
    return pytds.connect(
        server=os.environ.get("SQLSERVER_HOST", "localhost"),
        port=int(os.environ.get("SQLSERVER_PORT", "1433")),
        database="AdventureWorksLT2022",
        user="sa",
        password=os.environ["SQLSERVER_SA_PASSWORD"],
    )


def extract_table(conn, table: str) -> pd.DataFrame:
    query = f"SELECT * FROM SalesLT.[{table}]"
    with conn.cursor() as cursor:
        cursor.execute(query)
        columns = [col[0] for col in cursor.description]
        df = pd.DataFrame(cursor.fetchall(), columns=columns)

    # SQL Server `uniqueidentifier` columns come back as uuid.UUID objects;
    # pyarrow writes those as a Parquet UUID logical type that Spark's reader
    # rejects (PARQUET_TYPE_ILLEGAL), so stringify them before writing out.
    for column in df.columns:
        if df[column].map(lambda v: isinstance(v, uuid.UUID)).any():
            df[column] = df[column].map(lambda v: str(v) if isinstance(v, uuid.UUID) else v)

    return df


def upload_parquet(table: str, df: pd.DataFrame, host: str, token: str) -> None:
    buffer = io.BytesIO()
    df.to_parquet(buffer, index=False)
    buffer.seek(0)

    url = f"{host}/api/2.0/fs/files{VOLUME_PATH}/{table}/{table}.parquet"
    response = requests.put(
        url,
        headers={"Authorization": f"Bearer {token}"},
        data=buffer.getvalue(),
        params={"overwrite": "true"},
        timeout=120,
    )
    response.raise_for_status()


def main() -> int:
    databricks_host = os.environ["DATABRICKS_HOST"].rstrip("/")
    databricks_token = os.environ["DATABRICKS_TOKEN"]

    conn = sql_server_connection()
    try:
        for table in SALESLT_TABLES:
            df = extract_table(conn, table)
            upload_parquet(table, df, databricks_host, databricks_token)
            print(f"{table}: {len(df)} rows uploaded to {VOLUME_PATH}/{table}/{table}.parquet")
    finally:
        conn.close()

    print("Extraction complete.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
