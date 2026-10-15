# 考研学习工作台

面向 2027 年底初试、2028 年入学的个人学习工作台。核心流程：

**整体目标 → 按日安排（周、月汇总）→ 今日执行 → 学习结果记录 → 进度反馈 → 手动调整**

需求与技术栈依据见 [PLAN.md](PLAN.md)；工程约束见 [AGENTS.md](AGENTS.md)；目录职责见 [docs/STRUCTURE.md](docs/STRUCTURE.md)。

## 当前状态（更新于 2026-10-15）

张宇数学计划的当前摘要见 [当前确认版](docs/business/zhangyu-math-plan.md)：按两本书独立接续，保留详细学习内容，不安排复习和订正。仓库文件更新与线上计划导入分别记录，当前部署保持暂停。

已实现并验证：工程基础、核心闭环、信息反馈、复习机制。智能升级（Spring AI）按计划留到后续阶段，当前未安装相关依赖。

- 后端：Spring Boot 4.1.1 模块化单体，8 个业务模块全部实现。
- 本地清理复核（2026-10-15）：前端 84 项测试、类型检查及 PWA 构建，后端 13 项单元测试、干净编译及打包通过；本轮未运行数据库集成测试和 Docker 构建。见 [清理记录](docs/development/cleanup-20261009.md)。
- 数据库：Flyway 迁移 V1–V20，随应用启动执行；V12 导入数学一轮计划，V15 支持计划工作区，V16–V19 支持课程目录、接续练习、时间型安排与每日确认。
- 最新开发：V20 与导入 v5 支持按书独立接续，既有验证为后端 136 项、前端 87 项、类型检查和构建通过；验证环境与限制见 [验证记录](docs/testing/continued-study-20261015.md)。本次进展同步仅更新文档，未重跑业务测试。
- 线上记录：V19 英语每日确认与两份计划已发布导入，随后完成 38 任务／118 天／236 日安排的简化高数替换，保留学习记录及其他计划；见 [英语发布](docs/operations/english-plan-release-20261015.md)、[高数替换](docs/operations/math-simple-replacement-20261015.md)。V20 两书接续尚未发布，完整方案尚未转换导入，继续 [暂停交接](docs/development/continued-study-deploy-handoff-20261015.md)；本次未重新核验生产状态。
- 登录：预设账号 + Spring Security + Spring Session JDBC（会话存 MySQL）+ CSRF 防护。
- 历史验证（2026-10-08）：后端 92 项在隔离 MySQL 8.0.45 上通过并打包；前端 49 项测试、类型检查和生产构建通过。浏览器验证四视图、导入确认、日编辑/排序及离线本周；生产 MySQL 8.4.11 的隔离恢复、V14 → V15、会话及接口验收通过。未运行本地 Docker 专用会话迁移测试或重建 Docker 镜像，详见 [计划页验收](docs/testing/planning-workspace-20261008.md)。
- 历史验证（2026-10-01）：后端 72 项在隔离 MySQL 9.7.0 上通过；前端 38 项测试与构建通过。服务器 MySQL 8.4.11 隔离库验证 V13 → V14、登录与 JDBC 会话，今日清单已发布。见 [今日清单验证](docs/testing/today-checklist-20261001.md)和 [早期验收记录](docs/testing/acceptance.md)。
- 今日页：四科学习/复习清单、累计百分比与本次用时、后端时间合计；规则见 [今日清单](docs/business/today-checklist.md)。
- 计划页：默认整体时间轴、月历、周历和日安排，手动按日期规划；外部 JSON 方案校验、预览、确认追加。内置 AI 当前封锁。使用说明见 [导入规划书](docs/business/plan-import-guide.md)；已于 2026-10-08 发布，数据核对与线上验收见 [发布记录](docs/operations/planning-workspace-release-20261008.md)。
- 首次部署与会话修复的历史记录见 [首次部署全过程](docs/operations/deployment-journal-2026-09.md)。
- 前端：Vue 3 + TypeScript + Vite + Element Plus，含离线只读快照；当前课程完成比例使用 SVG，不安装无使用入口的图表库。
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

2 GB 服务器首次手动部署、HTTPS、已有本地数据迁移，使用
[手动部署说明](docs/operations/manual-deploy.md) 中的 `compose.bundle.yaml`。
在 Windows 本地构建：`./deploy/scripts/build-bundle.ps1 -Offline`，再上传生成的版本包。
已有学习数据须先备份并恢复验证，不要直接启动一个空数据库当作迁移完成。

本次从服务器选型、域名与 DNS、数据迁移，到排错、上线、定时维护的实际步骤及作用，
见 [首次部署全过程记录](docs/operations/deployment-journal-2026-09.md)。

以下为原 GHCR 镜像发布流程，需另行配置 HTTPS 入口，不与手动部署配置混用：

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

记录采用月 → 周 → 日展开，进度采用四个方向 → 分科 → 课程／资料 → 完整目录。目录导入与手动维护先预览确认，关联学习记录自动累计；今日和计划保持现状。见 [工作区说明](docs/business/records-progress-workspace.md)、[目录导入说明](docs/business/progress-catalog-guide.md) 与 [第二轮验收](docs/testing/records-progress-catalog-20261013.md)。本地启动、测试和构建命令保持不变，启动时由 Flyway 执行新增 V16 目录迁移。

| 文档 | 内容 |
| --- | --- |
| [PROJECT_MEMORY.md](PROJECT_MEMORY.md) | 用户偏好、长期交接与 Windows 接续提醒 |
| [DECISIONS.md](DECISIONS.md) | 已确认的设计决策、否决与暂缓边界 |
| [IDEAS.md](IDEAS.md) | 后续创意与待办候选，未代表开发授权 |
| [docs/STRUCTURE.md](docs/STRUCTURE.md) | 目录职责与文件落点 |
| [docs/development/local-setup.md](docs/development/local-setup.md) | 本地环境、启动与常见问题 |
| [docs/development/cleanup-20261009.md](docs/development/cleanup-20261009.md) | 本地冗余清理范围与验证结果 |
| [docs/database/schema.md](docs/database/schema.md) | 数据模型、索引与迁移说明 |
| [docs/api/README.md](docs/api/README.md) | 接口约定与端点清单 |
| [docs/testing/acceptance.md](docs/testing/acceptance.md) | 验收项与对应测试 |
| [docs/operations/deploy.md](docs/operations/deploy.md) | 部署、日志、备份、恢复与回退 |
| [docs/operations/deployment-journal-2026-09.md](docs/operations/deployment-journal-2026-09.md) | 本次首次部署的 30 个步骤、每步作用、排错与最终状态 |
| [problem.md](problem.md) | 本次部署失误、排查结论与避免方法 |
| [docs/decisions/](docs/decisions/) | 已做出的工程决策及依据 |

有序每日练习使用计划导入 v2：每日一个名额，完成后推进，尝试只记用时。启动会自动执行 V17，须先备份；旧 v1 导入兼容。详见 [接续练习说明](docs/business/sequential-practice.md)。

2026-10-14 已发布 V17，并通过网页导入数据结构每日一题计划。服务器升级、数据保留和最终线上核对见 [接续练习发布记录](docs/operations/sequence-release-20261014.md)。

时间型计划使用导入 v3：基本用时与弹性上限分别填写，未知题量留空，实际完成量据实记录；复盘只计用时。升级前备份，启动会执行 V18。字段及操作见 [导入规划书](docs/business/plan-import-guide.md)。

每日勾选清单使用导入 v4 的 CHECK_ONLY：无需数量或时长，今日可确认／取消，不增加新学量。启动执行新增 V19；已有 V1–V18 不改写，旧 v1–v3 导入兼容。启动、测试与构建命令保持不变，字段见 docs/business/plan-import-guide.md。

按书学习接续使用导入 v5：两本书独立收纳内容，旧内容与新内容分别记录累计百分比；未推进则延期，部分完成后才带入下一份。新增 V20 保存规划元数据与原学习记录的百分比增量，普通任务和每日一题保持原规则。启动、测试、构建命令不变。计划导入窗口可下载张宇首周格式样例；旧方案不会自动替换。规则与格式见 [导入规划书](docs/business/plan-import-guide.md#按书学习接续版本-5)，本轮验证边界见 [开发验证记录](docs/testing/continued-study-20261015.md)。

2026-10-15 学习接续验证：隔离 MySQL9.7 上后端136项及打包、前端87项、类型检查和生产构建通过；未执行Linux Docker会话专测或MySQL8.4验收，未发布。详见上述验证记录。
