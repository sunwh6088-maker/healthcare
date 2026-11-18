#!/usr/bin/env bash
# 从 backup.sh 生成的 .sql.gz 文件恢复 MySQL。
# 用法：DB_PASSWORD=xxx ./healthcare-api/scripts/restore.sh backups/healthcare-20260101-020000.sql.gz
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "用法：$0 <backup.sql.gz>" >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

BACKUP_FILE="$1"
DB_NAME="${DB_NAME:-healthcare}"
DB_USER="${DB_USER:-healthcare}"
DB_PASSWORD="${DB_PASSWORD:-healthcare_dev}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "备份文件不存在：$BACKUP_FILE" >&2
  exit 1
fi

gunzip -c "$BACKUP_FILE" | docker compose exec -T mysql \
  mysql -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"

echo "恢复完成：$BACKUP_FILE"
