# 考研学习工作台

面向 2027 年底初试、2028 年入学的个人学习工作台。核心流程：

**整体目标 → 按日安排（周、月汇总）→ 今日执行 → 学习结果记录 → 进度反馈 → 手动调整**

需求与技术栈依据见 [PLAN.md](PLAN.md)；工程约束见 [AGENTS.md](AGENTS.md)；目录职责见 [docs/STRUCTURE.md](docs/STRUCTURE.md)。

## 当前状态

已实现并验证：工程基础、核心闭环、信息反馈、复习机制。智能升级（Spring AI）按计划留到后续阶段，当前未安装相关依赖。

- 后端：Spring Boot 4.1.1 模块化单体，8 个业务模块全部实现。
- 数据库：Flyway 迁移 V1–V15，随应用启动执行；V12 会一次性导入 2027 考研数学一轮计划；V15 支持计划工作区及外部方案防重。
- 登录：预设账号 + Spring Security + Spring Session JDBC（会话存 MySQL）+ CSRF 防护。
- 本轮验证（2026-10-08）：后端 92 项在隔离 MySQL 8.0.45 上通过并打包；前端 49 项测试、类型检查和生产构建通过。浏览器验证四视图、导入确认、日编辑/排序及离线本周；生产 MySQL 8.4.11 的隔离恢复、V14 → V15、会话及接口验收通过。未运行本地 Docker 专用会话迁移测试或重建 Docker 镜像，详见 [计划页验收](docs/testing/planning-workspace-20261008.md)。
- 历史验证（2026-10-01）：后端 72 项在隔离 MySQL 9.7.0 上通过；前端 38 项测试与构建通过。服务器 MySQL 8.4.11 隔离库验证 V13 → V14、登录与 JDBC 会话，今日清单已发布。见 [今日清单验证](docs/testing/today-checklist-20261001.md)和 [早期验收记录](docs/testing/acceptance.md)。
- 今日页：四科学习/复习清单、累计百分比与本次用时、后端时间合计；规则见 [今日清单](docs/business/today-checklist.md)。
- 计划页：默认整体时间轴、月历、周历和日安排，手动按日期规划；外部 JSON 方案校验、预览、确认追加。内置 AI 当前封锁。使用说明见 [导入规划书](docs/business/plan-import-guide.md)；已于 2026-10-08 发布，数据核对与线上验收见 [发布记录](docs/operations/planning-workspace-release-20261008.md)。
- 前端：Vue 3 + TypeScript + Vite + Element Plus + ECharts，含离线只读快照。
- 部署：Docker Compose + Nginx，CI 自动检查、生产发布手动触发。

## 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | 构建与运行 |
| Maven | 3.9.16 | 由 `backend/mvnw` 固定，无需预装 |
| Node.js | 22 | 仅前端构建需要 |
| pnpm | 9 | 前端唯一包管理器 |
| MySQL | 8.4 LTS | 开发也可用 8.0；集成测试默认用 Testcontainers 起 8.4 |
| Docker | 任意近期版本 | 集成测试与部署使用；不可用时见下 |

## 本地启动

### 1. 数据库

```bash
mysql -u root -p -e "CREATE DATABASE kaoyan_study CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u root -p -e "CREATE USER 'kaoyan'@'%' IDENTIFIED BY '你的口令'; GRANT ALL PRIVILEGES ON kaoyan_study.* TO 'kaoyan'@'%';"
```

表结构不需要手工创建：应用启动时由 Flyway 执行 `backend/src/main/resources/db/migration/` 下的迁移。

### 2. 后端

预设账号口令只能通过环境变量提供，仓库中不保存任何真实口令；口令仅用于首次建号，之后以 BCrypt 哈希保存在数据库里。

```bash
cd backend
export DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=kaoyan_study
export DB_USERNAME=kaoyan DB_PASSWORD=你的口令
export PRESET_ACCOUNT_USERNAME=kaoyan
export PRESET_ACCOUNT_PASSWORD=首次设置的口令
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Windows（PowerShell）把 `export` 换成 `$env:DB_PASSWORD = '...'`，Wrapper 命令使用
`.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev`（在 `backend` 目录执行）。

接口文档（仅 dev 开启）：<http://127.0.0.1:8080/swagger-ui.html>，健康检查：`/actuator/health`。

### 3. 前端

```bash
cd frontend
pnpm install
pnpm run dev
```

开发服务器默认代理 `/api` 到 `http://127.0.0.1:8080`。

独立预览可在启动前指定 `STUDY_API_PROXY`，例如 PowerShell：`$env:STUDY_API_PROXY='http://127.0.0.1:18089'`，再执行 `pnpm dev --host 127.0.0.1 --port 15173 --strictPort`。后端应指向一次性预览库；不要连接现有学习数据做破坏性验收。

## 构建与测试

```bash
# 后端：单元测试 + 集成测试 + 打包
cd backend && ./mvnw -B verify

# 前端：类型检查、单元测试、构建
cd frontend && pnpm run typecheck && pnpm run test && pnpm run build
```

Windows 验证命令（在 `backend` 目录）：`.\mvnw.cmd -v`、`.\mvnw.cmd -B verify`。

集成测试连接真实 MySQL，二选一：

1. **默认**：启动 `mysql:8.4` Testcontainers 容器（需要可用的 Docker）。
2. **无 Docker 时**：指向一个**一次性测试库**（迁移会在该库上执行）：

```bash
export TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/kaoyan_study_test?allowPublicKeyRetrieval=true&useSSL=false'
export TEST_MYSQL_USERNAME=kaoyan
export TEST_MYSQL_PASSWORD=你的口令
./mvnw -B test -Dtest=PlanLearningFlowIntegrationTest
```

> 注意：在 Windows 上通过 `mvn.cmd` 传 `-Dtest.mysql.url=...` 时，JDBC 参数里的 `&` 会被 cmd 解析吞掉，因此推荐用上面的环境变量。

## 部署

当前远程网站采用本地构建产物、上传到服务器的手动部署方式，服务器使用 `compose.bundle.yaml`；它与本仓库下述 `compose.yaml` 镜像发布流程不同。今日清单已于 2026-10-01 发布，升级验证、备份位置与回退限制见 [今日清单发布记录](docs/operations/today-checklist-release-20261001.md)。此前的前端更新见 [发布记录](docs/operations/frontend-update-20260930.md)。

```bash
cd deploy
cp .env.example .env   # 填写数据库口令与预设账号口令
./scripts/deploy.sh 0.1.0
```

Nginx 提供前端静态文件并转发 `/api`；数据库与管理端点都不对外发布。备份、恢复与回退见 [docs/operations/deploy.md](docs/operations/deploy.md)。

## 目录

- `backend/`：Spring Boot 应用（`src/main/java/com/kaoyan/study/<module>/`）。
- `frontend/`：Vue 3 应用（`src/features/<module>/`）。
- `deploy/`：Compose、Dockerfile、Nginx 与运维脚本。
- `docs/`：目录说明、开发、数据库、接口、测试与运维文档。
- `.github/workflows/`：CI（提交触发）与生产发布（手动触发）。

## 文档索引

| 文档 | 内容 |
| --- | --- |
| [docs/STRUCTURE.md](docs/STRUCTURE.md) | 目录职责与文件落点 |
| [docs/development/local-setup.md](docs/development/local-setup.md) | 本地环境、启动与常见问题 |
| [docs/database/schema.md](docs/database/schema.md) | 数据模型、索引与迁移说明 |
| [docs/api/README.md](docs/api/README.md) | 接口约定与端点清单 |
| [docs/testing/acceptance.md](docs/testing/acceptance.md) | 验收项与对应测试 |
| [docs/operations/deploy.md](docs/operations/deploy.md) | 部署、日志、备份、恢复与回退 |
| [docs/decisions/](docs/decisions/) | 已做出的工程决策及依据 |
