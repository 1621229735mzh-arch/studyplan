#!/usr/bin/env bash
# 备份数据库到独立于运行数据卷的位置，并校验备份可读。
# 备份目录通过 BACKUP_DIR 指定（默认 deploy/backups），不要与 mysql_data 卷放在同一磁盘。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${DEPLOY_DIR}"

BACKUP_DIR="${BACKUP_DIR:-${DEPLOY_DIR}/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
STAMP="$(date +%Y%m%d-%H%M%S)"
TARGET="${BACKUP_DIR}/kaoyan_study-${STAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

# 口令通过容器内环境变量传入，不出现在宿主机命令行历史里
echo "==> 导出数据库到 ${TARGET}"
docker compose exec -T mysql \
    sh -c 'exec mysqldump --single-transaction --routines --triggers \
           -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
    | gzip -9 > "${TARGET}"

if [[ ! -s "${TARGET}" ]]; then
    echo "备份文件为空，判定为失败" >&2
    rm -f "${TARGET}"
    exit 1
fi

echo "==> 校验备份可读"
gzip -t "${TARGET}"
gzip -dc "${TARGET}" | tail -n 5 | grep -q 'Dump completed' \
    || echo "警告：未找到 mysqldump 结束标记，请人工确认备份完整性" >&2

echo "==> 清理超过 ${RETENTION_DAYS} 天的旧备份"
find "${BACKUP_DIR}" -name 'kaoyan_study-*.sql.gz' -type f -mtime "+${RETENTION_DAYS}" -print -delete

echo "==> 备份完成：${TARGET} ($(du -h "${TARGET}" | cut -f1))"
