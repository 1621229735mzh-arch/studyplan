#!/usr/bin/env bash
# 部署一次发布：固定镜像版本 → 启动 → 健康检查 → 报告数据库迁移版本。
# 迁移由应用启动时的 Flyway 执行，这里只做校验，不做手工改表。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${DEPLOY_DIR}"

IMAGE_TAG="${1:-}"
if [[ -z "${IMAGE_TAG}" ]]; then
    echo "用法: $0 <image-tag>" >&2
    exit 2
fi

if [[ ! -f .env ]]; then
    echo "缺少 ${DEPLOY_DIR}/.env，请先从 .env.example 复制并填写。" >&2
    exit 2
fi

export IMAGE_TAG
echo "==> 部署版本 ${IMAGE_TAG}"

echo "==> 拉取镜像"
docker compose pull

echo "==> 启动服务"
docker compose up -d --no-build --remove-orphans

echo "==> 等待健康检查"
"${SCRIPT_DIR}/healthcheck.sh"

echo "==> 校验数据库迁移版本"
docker compose exec -T mysql \
    sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
           -N -e "SELECT CONCAT(version, \" - \", description, \" (\", success, \")\") \
                  FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;"'

echo "==> 部署完成：${IMAGE_TAG}"
