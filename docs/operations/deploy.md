# 部署、日志、备份与回退

## 结构

```text
浏览器 ──HTTPS──> Nginx（前端静态文件 + /api 转发）──> Spring Boot ──> MySQL
```

- 只有 Nginx 发布端口；`mysql` 与 `backend` 只在 Compose 内部网络可达。
- `/actuator/**` 在 Nginx 上直接返回 404，管理端点不对外暴露。
- 数据库结构变更由应用启动时的 Flyway 执行，`deploy.sh` 只做校验。

## 首次部署

```bash
cd deploy
cp .env.example .env
# 填写 DB_PASSWORD、MYSQL_ROOT_PASSWORD、PRESET_ACCOUNT_PASSWORD 与镜像地址
./scripts/deploy.sh 0.1.0
```

`deploy.sh` 会固定 `IMAGE_TAG` 拉取镜像、启动服务、执行健康检查，并打印最近三条迁移记录。

## 日常发布

生产发布由 `.github/workflows/release.yml` **手动触发**（`workflow_dispatch`），
流程为：构建并推送镜像 → 上传部署文件 → 在服务器执行 `deploy.sh <tag>`。

发布前检查：

1. 确认目标提交的 CI 通过（后端须在真实 MySQL 上运行全部测试，当前共 64 个；前端检查与构建通过）。
2. 新增迁移文件已随本次发布一起带上（`backend/src/main/resources/db/migration/`）。
3. 备份已完成（见下节），且备份文件可读。

## 日志

```bash
docker compose logs -f backend          # 应用日志
docker compose logs -f frontend         # Nginx 访问日志
tail -f deploy/logs/study-backend.log   # prod profile 写入的文件日志
```

应用日志记录了请求路径、业务错误码与异常堆栈；不记录口令、会话 ID 等敏感内容。

## 备份

```bash
cd deploy
BACKUP_DIR=/srv/backups ./scripts/backup.sh
```

- 使用 `mysqldump --single-transaction`，导出后 `gzip -t` 校验，并检查 mysqldump 结束标记。
- 默认保留 30 天（`RETENTION_DAYS`），目录必须位于**独立于 MySQL 数据卷**的位置。
- 建议由 cron 每日执行，并定期做一次恢复演练。

## 恢复

```bash
cd deploy
./scripts/restore.sh /srv/backups/kaoyan_study-20260922-030000.sql.gz
```

脚本会先停后端再导入，然后启动后端并做健康检查。恢复后请确认
`flyway_schema_history` 的最高版本与备份时一致。

## 回退

1. **只回退应用版本**：用上一个 `IMAGE_TAG` 重新执行 `./scripts/deploy.sh <上一个 tag>`。
   Flyway 不会回退已应用的迁移；只要新版本没有引入破坏性结构变更，旧版本可以继续运行。
2. **涉及数据迁移的回退**：先恢复备份（`restore.sh`），再启动旧版本镜像。
   数据库结构变更一律单独评估回退方式，不能假设“代码回退即完成回退”。
3. 回退后执行 `./scripts/healthcheck.sh` 并检查关键功能（登录、今日、记录提交）。

## 健康检查

```bash
cd deploy
./scripts/healthcheck.sh
```

依次检查前端外壳、后端 `/actuator/health` 与数据库连通性；失败时打印相关容器日志尾部。
