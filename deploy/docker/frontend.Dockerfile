# 前端镜像：构建静态文件后由 Nginx 提供，并转发 /api 到后端容器。
# 构建上下文是仓库根目录（见 deploy/compose.yaml）。

FROM node:22-alpine AS build
WORKDIR /build

RUN corepack enable && corepack prepare pnpm@9.15.0 --activate

# 先安装依赖，利用层缓存；lockfile 必须提交，保证可重复构建
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY frontend/ ./
# 导入窗口打包仓库中的规划书；当前 /build 对应仓库的 frontend/。
COPY docs/business/plan-import* /docs/business/
RUN pnpm run build


FROM nginx:1.27-alpine AS runtime

COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1/healthz >/dev/null || exit 1
