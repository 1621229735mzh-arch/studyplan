#!/usr/bin/env bash
# 从备份恢复数据库。
# 注意：恢复会覆盖当前数据，请先停后端服务，并确认迁移版本与备份一致。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${DEPLOY_DIR}"

BACKUP_FILE="${1:-}"
if [[ -z "${BACKUP_FILE}" || ! -f "${BACKUP_FILE}" ]]; then
    echo "用法: $0 <备份文件.sql.gz>" >&2
    echo "可用备份：" >&2
    ls -1 "${DEPLOY_DIR}/backups" 2>/dev/null >&2 || true
    exit 2
fi

echo "==> 校验备份文件"
gzip -t "${BACKUP_FILE}"

echo "==> 停止后端（避免写入过程中恢复）"
docker compose stop backend

echo "==> 恢复数据"
gzip -dc "${BACKUP_FILE}" | docker compose exec -T mysql \
    sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'

echo "==> 启动后端"
docker compose start backend

echo "==> 健康检查"
"${SCRIPT_DIR}/healthcheck.sh"

echo "==> 恢复完成；请检查应用日志确认 Flyway 迁移版本与备份一致"
