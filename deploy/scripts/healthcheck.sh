#!/usr/bin/env bash
# 健康检查：前端外壳、后端健康端点与数据库连通性。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${DEPLOY_DIR}"

HTTP_PORT="$(grep -E '^HTTP_PORT=' .env 2>/dev/null | cut -d= -f2 || true)"
HTTP_PORT="${HTTP_PORT:-80}"
ATTEMPTS="${HEALTH_ATTEMPTS:-30}"
SLEEP_SECONDS="${HEALTH_INTERVAL:-5}"

echo "==> 等待前端可用（最多 $((ATTEMPTS * SLEEP_SECONDS)) 秒）"
for ((i = 1; i <= ATTEMPTS; i++)); do
    # 只到前端容器这一层，容器内路径即 Nginx 的 /healthz
    if docker compose exec -T frontend wget -qO- http://127.0.0.1/healthz >/dev/null 2>&1; then
        echo "    前端就绪"
        break
    fi
    if [[ "${i}" -eq "${ATTEMPTS}" ]]; then
        echo "前端在预期时间内没有就绪" >&2
        docker compose logs --tail=80 frontend >&2 || true
        exit 1
    fi
    sleep "${SLEEP_SECONDS}"
done

echo "==> 检查后端健康端点"
if ! docker compose exec -T backend curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null; then
    echo "后端健康检查失败" >&2
    docker compose logs --tail=120 backend >&2 || true
    exit 1
fi
echo "    后端就绪"

echo "==> 检查数据库"
if ! docker compose exec -T mysql sh -c 'mysqladmin ping -h 127.0.0.1 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD"' >/dev/null; then
    echo "数据库健康检查失败" >&2
    exit 1
fi
echo "    数据库就绪"

echo "==> 健康检查通过（对外端口 ${HTTP_PORT}）"
