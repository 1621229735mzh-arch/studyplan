#!/usr/bin/env bash
# 导出备份，不自动删除历史备份。仍需将备份复制到另一台设备/独立存储。
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
destination="${1:?Usage: bash scripts/bundle-backup.sh /absolute/backup/directory}"
[[ "$destination" == /* ]] || { echo 'Use an absolute backup directory'; exit 1; }
umask 077
mkdir -p "$destination"
target="$destination/study-$(date +%Y%m%d-%H%M%S).sql.gz"
[[ ! -e "$target" && ! -e "$target.partial" ]] || exit 1
docker compose -f compose.bundle.yaml exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --no-tablespaces --set-gtid-purged=OFF --routines --triggers --default-character-set=utf8mb4 "$MYSQL_DATABASE"' \
  | gzip > "$target.partial"
gzip -t "$target.partial"
mv "$target.partial" "$target"
echo "Backup: $target"
sha256sum "$target"
