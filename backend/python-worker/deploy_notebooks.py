"""
Phase 4: upload the adapted Bronze/Silver/Gold notebooks in
databricks/notebooks/ to the Databricks workspace (Workspace Import API),
then create (or update) the Job that runs them in sequence.

Usage:
    python deploy_notebooks.py
"""
from __future__ import annotations

import base64
import json
import os
import sys
from pathlib import Path

import requests
from dotenv import load_dotenv

REPO_ROOT = Path(__file__).resolve().parents[2]
load_dotenv(REPO_ROOT / ".env")

NOTEBOOKS_DIR = REPO_ROOT / "databricks" / "notebooks"
WORKSPACE_DIR = "/Shared/nexora"

NOTEBOOKS = [
    "01_bronze_ingest.py",
    "02_silver_transform.py",
    "03_gold_transform.py",
]

JOB_NAME = "nexora-bronze-silver-gold"


def api(host: str, token: str, method: str, path: str, **kwargs):
    response = requests.request(
        method, f"{host}{path}", headers={"Authorization": f"Bearer {token}"}, timeout=60, **kwargs
    )
    response.raise_for_status()
    return response.json() if response.content else {}


def ensure_workspace_dir(host: str, token: str, path: str) -> None:
    api(host, token, "POST", "/api/2.0/workspace/mkdirs", json={"path": path})


def import_notebook(host: str, token: str, local_path: Path, workspace_path: str) -> None:
    content = base64.b64encode(local_path.read_bytes()).decode("ascii")
    api(
        host,
        token,
        "POST",
        "/api/2.0/workspace/import",
        json={
            "path": workspace_path,
            "format": "SOURCE",
            "language": "PYTHON",
            "content": content,
            "overwrite": True,
        },
    )
    print(f"Uploaded {local_path.name} -> {workspace_path}")


def find_existing_job(host: str, token: str, name: str) -> int | None:
    jobs = api(host, token, "GET", "/api/2.2/jobs/list", params={"name": name}).get("jobs", [])
    return jobs[0]["job_id"] if jobs else None


def upsert_job(host: str, token: str) -> int:
    tasks = []
    previous_task_key = None
    for notebook in NOTEBOOKS:
        task_key = Path(notebook).stem
        task = {
            "task_key": task_key,
            "notebook_task": {"notebook_path": f"{WORKSPACE_DIR}/{Path(notebook).stem}"},
        }
        if previous_task_key:
            task["depends_on"] = [{"task_key": previous_task_key}]
        tasks.append(task)
        previous_task_key = task_key

    job_spec = {
        "name": JOB_NAME,
        "tasks": tasks,
        "max_concurrent_runs": 1,
    }

    existing_id = find_existing_job(host, token, JOB_NAME)
    if existing_id:
        api(host, token, "POST", "/api/2.2/jobs/reset", json={"job_id": existing_id, "new_settings": job_spec})
        print(f"Updated existing job {existing_id} ({JOB_NAME})")
        return existing_id

    result = api(host, token, "POST", "/api/2.2/jobs/create", json=job_spec)
    job_id = result["job_id"]
    print(f"Created job {job_id} ({JOB_NAME})")
    return job_id


def main() -> int:
    host = os.environ["DATABRICKS_HOST"].rstrip("/")
    token = os.environ["DATABRICKS_TOKEN"]

    ensure_workspace_dir(host, token, WORKSPACE_DIR)
    for notebook in NOTEBOOKS:
        import_notebook(host, token, NOTEBOOKS_DIR / notebook, f"{WORKSPACE_DIR}/{Path(notebook).stem}")

    job_id = upsert_job(host, token)
    print(json.dumps({"job_id": job_id, "job_name": JOB_NAME}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
