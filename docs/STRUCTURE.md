# 目录与模块落点

本文件说明现有骨架及后续文件放置位置，不定义数据库表结构或 API 合同。需求依据为根目录 `PLAN.md`。

## 顶层结构

```text
408doing/
├── PLAN.md
├── AGENTS.md
├── README.md
├── .github/workflows/             CI 与手动发布
├── frontend/
│   ├── public/                    无需打包处理的公共静态资源
│   ├── src/
│   │   ├── api/                   HTTP 客户端、会话及 CSRF 相关通用处理
│   │   ├── assets/                参与打包的静态资源
│   │   ├── components/            跨功能通用组件
│   │   ├── composables/           跨功能组合式函数
│   │   ├── features/              各业务功能，见下表
│   │   ├── layouts/               页面布局
│   │   ├── router/                路由及登录守卫
│   │   ├── stores/                全局会话与共享应用状态
│   │   ├── styles/                全局样式与主题
│   │   ├── types/                 跨功能通用类型
│   │   ├── offline/               IndexedDB 快照读写、同步与清理
│   │   └── pwa/                   应用外壳缓存与 Service Worker 相关逻辑
│   └── tests/
│       ├── unit/                  Vitest 单元与组件测试
│       ├── e2e/                   Playwright 关键流程测试
│       └── fixtures/              无敏感信息的测试数据
├── backend/
│   └── src/
│       ├── main/
│       │   ├── java/com/kaoyan/study/
│       │   │   ├── config/        框架装配与安全配置
│       │   │   ├── common/
│       │   │   │   ├── api/       通用响应与分页等接口基础类型
│       │   │   │   └── exception/ 通用异常与异常处理
│       │   │   └── <module>/     下表所列业务模块
│       │   └── resources/
│       │       ├── db/migration/  Flyway 迁移，所有模块共用版本序列
│       │       └── mapper/        按模块分目录的 MyBatis XML
│       └── test/
│           ├── java/com/kaoyan/study/
│           │   ├── <module>/     对应业务模块测试
│           │   ├── security/     登录、权限与 CSRF 测试
│           │   └── support/      Testcontainers 与公共测试支持
│           └── resources/fixtures/ 测试数据
├── deploy/
│   ├── docker/                   前后端 Dockerfile
│   ├── nginx/                    静态站点、HTTPS 与 /api 转发配置
│   └── scripts/                  部署、健康检查、备份与恢复脚本
└── docs/
    ├── STRUCTURE.md
    ├── api/                      OpenAPI 与接口约定
    ├── business/                 业务规则细化与用例
    ├── database/                 数据模型、索引与迁移说明
    ├── development/              开发环境与本地启动
    ├── testing/                  验收场景与测试说明
    ├── operations/               部署、日志、升级、备份、恢复与回退
    ├── decisions/                已做出的工程决策及依据
    └── ai/                       智能阶段的工具边界与评估场景（预留）
```

`<module>` 是说明用占位名称，实际目录使用下表中的英文名。

## 业务模块归属

| 目录名 | 职责 | 前端 | 后端 |
| --- | --- | --- | --- |
| `account` | 预设账号、登录、会话及退出 | 已建 | 已建 |
| `settings` | 考试时间、科目、计量单位、学习预算与复习额度配置 | 已建 | 已建 |
| `plan` | 阶段目标、任务、周计划、每日安排及手动调整 | 已建 | 已建 |
| `learning` | 实际学习记录、补记、修改及删除 | 已建 | 已建 |
| `progress` | 进度、计划与实际对比及趋势查询 | 已建 | 已建 |
| `review` | 复习项、复习记录、掌握反馈、建议与确认安排 | 已建 | 已建 |
| `memo` | 备忘录、搜索、到期提醒及转任务 | 已建 | 已建 |
| `target` | 候选院校、专业、链接及备注 | 已建 | 已建 |
| `today` | 今日安排与记录入口的页面组合 | 已建 | 使用计划、学习与复习业务能力 |
| `ai` | 智能计划建议与解释 | 仅预留目录 | 仅预留目录 |

阶段目标归 `plan`，候选院校目标归 `target`；科目等配置归 `settings`。今日页面不另存一套任务。复习额度配置由 `settings` 保存，额度校验由 `review` 业务执行；计划调整仍需校验相关预算。

## 模块内部结构

后端 `account/settings/plan/learning/review/memo/target` 均已建立：

```text
<module>/
├── controller/  HTTP 入口与参数接收
├── dto/         明确的请求、响应及业务传输类型
├── service/     用例编排、业务规则与事务边界
├── mapper/      MyBatis 数据访问接口
└── entity/      持久化数据类型，不直接用于接口响应
```

`progress` 已建立 `controller/dto/service/mapper`，只读统计结果放 DTO，不预建一份独立完成量实体。

SQL XML 放在 `backend/src/main/resources/mapper/<module>/`，与对应 Mapper 接口的 namespace 一致；初始化时配置加载路径。数据库迁移集中在 `db/migration/`，不能给每个模块各自从 `V1` 开始编号。

`plan/advisor/` 预留业务层的建议接口及其输入输出类型，未来由 `ai/` 实现。当前不预建模型 SDK 配置或工具执行代码。

前端除预留的 `ai` 外，每个功能均有 `api/`、`components/`、`views/`：分别放业务接口调用、功能内组件与页面。功能专用的类型或状态按需放回该功能目录，共享目录只放真正跨功能的内容。

后端测试已按八个当前业务模块建立同名包。AI 的测试与真实模型评估在智能阶段建立并隔离执行。

## 实际创建的工程文件

工程基础与业务模块已实现，下表文件均已创建并经过真实构建与测试：

| 位置 | 文件 |
| --- | --- |
| `backend/` | `pom.xml`、`mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties`（Maven 3.9.16） |
| `backend/src/main/java/com/kaoyan/study/` | `StudyApplication.java`（`@MapperScan` + `@ConfigurationPropertiesScan`） |
| `backend/src/main/resources/` | `application.yml`、`application-dev.yml`、`application-prod.yml` |
| `backend/src/main/resources/db/migration/` | `V1`–`V12`（账号、会话、设置、计划、学习、复习、备忘录、目标、只记时长记录与数学一轮计划导入） |
| `backend/src/main/java/com/kaoyan/study/config/` | 安全（Spring Security 7 + JDBC 会话 + CSRF）、OpenAPI、预设账号配置 |
| `frontend/` | `package.json`、`pnpm-lock.yaml`、`index.html`、Vite/TypeScript/Vitest 配置 |
| `frontend/src/` | `main.ts`、`App.vue` 及各功能页面 |
| `deploy/` | `compose.yaml`、`.env.example`、`docker/*.Dockerfile`、`nginx/default.conf`、`scripts/*.sh` |
| `.github/workflows/` | `ci.yml`（提交触发）、`release.yml`（手动触发的生产发布） |
| `docs/decisions/` | 已记录 4 项工程决策（版本锁定、Boot 4 模块拆分、复习参数可配置、复习安排落点） |

`progress` 只使用只读关联查询，不预建独立完成量实体。`plan/advisor/` 仍为智能阶段的预留位置。

Docker 构建上下文取仓库根目录（`deploy/compose.yaml` 中 `context: ..`），
手动发布工作流也使用仓库根目录（`context: .`）。
因此 Dockerfile 内路径以 `backend/`、`frontend/`、`deploy/` 开头，仓库根的 `.dockerignore`
排除 `.git`、`node_modules`、`target`、备份与环境文件。

写学习记录和修改任务通过 `TaskService.requireExistingForUpdate` 共用任务行锁。
复习模块通过 `StudySettingsService.lockForReviewScheduling` 取得额度锁，不直接写设置 Mapper。
计划外复习通过学习业务服务核验 `sourceRecordId`；前端从已有学习记录选择来源。
备忘录转任务先锁定备忘录，已有转换结果直接返回；任务仍由计划业务服务创建。

`.gitkeep` 占位文件在对应目录已有真实文件后即失去作用，可按需删除，不影响构建。
