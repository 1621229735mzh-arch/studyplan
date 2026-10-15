#!/usr/bin/env bash
# 仅将本地导出的单库 SQL 恢复到空库。绝不覆盖已有服务器表。
set -euo pipefail
backup="$(realpath "${1:?Usage: bash scripts/bundle-import.sh /absolute/path/study.sql}")"
[[ -s "$backup" ]] || { echo 'Backup missing or empty'; exit 1; }
cd "$(dirname "${BASH_SOURCE[0]}")/.."
dc=(docker compose -f compose.bundle.yaml)
[[ -z "$("${dc[@]}" ps -q --status running backend)" ]] || { echo 'Backend is running; aborting import'; exit 1; }
"${dc[@]}" up -d --wait --wait-timeout 360 mysql
count="$("${dc[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()"')"
[[ "$count" == 0 ]] || { echo 'Target database is not empty. No data was changed. Do not retry with a partial import.'; exit 1; }
"${dc[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"' < "$backup"
"${dc[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -e "SELECT version,success FROM flyway_schema_history ORDER BY installed_rank; SELECT COUNT(*) AS tasks FROM task; SELECT COUNT(*) AS study_records FROM study_record;"'
echo 'Import finished. Compare counts with local database BEFORE starting backend.'
