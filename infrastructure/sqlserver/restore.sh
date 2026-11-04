#!/usr/bin/env bash
# Restores AdventureWorksLT2022 from the official Microsoft .bak into the
# sqlserver service. Runs once, inside a short-lived init container that
# shares the sqlserver container's data volume (so RESTORE's MOVE targets
# land where the server process can actually see them) and a read-only
# mount of the backup file.
#
# Logical file names inside the .bak aren't assumed — RESTORE FILELISTONLY
# is queried first and parsed, since Microsoft doesn't guarantee a fixed
# naming scheme across sample releases and guessing wrong fails RESTORE.
set -euo pipefail

SQLCMD="/opt/mssql-tools/bin/sqlcmd -C -S sqlserver -U sa -P ${SA_PASSWORD}"
BAK_PATH="/var/opt/mssql/backup/AdventureWorksLT2022.bak"
DB_NAME="AdventureWorksLT2022"

echo "Waiting for SQL Server to accept connections..."
until $SQLCMD -Q "SELECT 1" > /dev/null 2>&1; do
  sleep 2
done

echo "Checking if ${DB_NAME} already exists..."
EXISTS=$($SQLCMD -h -1 -Q "SET NOCOUNT ON; SELECT COUNT(*) FROM sys.databases WHERE name = '${DB_NAME}'" | tr -d '[:space:]')
if [ "$EXISTS" = "1" ]; then
  echo "${DB_NAME} already restored, nothing to do."
  exit 0
fi

echo "Reading logical file list from backup..."
FILELIST=$($SQLCMD -h -1 -s "," -W -Q "SET NOCOUNT ON; RESTORE FILELISTONLY FROM DISK = N'${BAK_PATH}'")

DATA_LOGICAL=$(echo "$FILELIST" | awk -F, '$2 ~ /\.mdf$|D$/ {print $1; exit}')
LOG_LOGICAL=$(echo "$FILELIST" | awk -F, '$2 ~ /\.ldf$|L$/ {print $1; exit}')

# Fallback: first row is data, second is log — true for every known
# AdventureWorksLT release, used only if the heuristic above found nothing.
if [ -z "$DATA_LOGICAL" ] || [ -z "$LOG_LOGICAL" ]; then
  DATA_LOGICAL=$(echo "$FILELIST" | sed -n '1p' | awk -F, '{print $1}')
  LOG_LOGICAL=$(echo "$FILELIST" | sed -n '2p' | awk -F, '{print $1}')
fi

echo "Data logical file: ${DATA_LOGICAL}"
echo "Log logical file:  ${LOG_LOGICAL}"

$SQLCMD -Q "
RESTORE DATABASE [${DB_NAME}]
FROM DISK = N'${BAK_PATH}'
WITH
  MOVE N'${DATA_LOGICAL}' TO N'/var/opt/mssql/data/${DB_NAME}.mdf',
  MOVE N'${LOG_LOGICAL}' TO N'/var/opt/mssql/data/${DB_NAME}_log.ldf',
  REPLACE;
"

echo "Restore complete."
