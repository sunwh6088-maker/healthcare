#!/usr/bin/env bash
# MySQL 逻辑备份：gzip 压缩并保留最近 N 天。
# 用法：DB_PASSWORD=xxx ./healthcare-api/scripts/backup.sh [备份目录]
# 建议用 cron 每日执行，并把备份目录同步到另一台机器或对象存储。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

BACKUP_DIR="${1:-${BACKUP_DIR:-./backups}}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"
DB_NAME="${DB_NAME:-healthcare}"
DB_USER="${DB_USER:-healthcare}"
DB_PASSWORD="${DB_PASSWORD:-healthcare_dev}"

mkdir -p "$BACKUP_DIR"
TARGET="$BACKUP_DIR/healthcare-$STAMP.sql.gz"

docker compose exec -T mysql \
  mysqldump -u"$DB_USER" -p"$DB_PASSWORD" \
  --single-transaction --routines --triggers --no-tablespaces \
  "$DB_NAME" | gzip > "$TARGET"

find "$BACKUP_DIR" -name "healthcare-*.sql.gz" -mtime +"$RETENTION_DAYS" -delete

echo "备份完成：$TARGET"
