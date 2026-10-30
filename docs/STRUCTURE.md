# 目录与模块落点

本文件说明现有骨架及后续文件放置位置，不定义数据库表结构或 API 合同。需求依据为根目录 `PLAN.md`。

## 顶层结构

```text
408doing/
├── PLAN.md
├── AGENTS.md
├── README.md
├── PROJECT_MEMORY.md             用户偏好、项目交接与 Windows 接续提醒
├── DECISIONS.md                  本次对话确认的设计决策及暂缓边界
├── IDEAS.md                      尚未批准实现的后续创意
├── problem.md                    部署问题与避免方法，部署和线上排障前阅读
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
│       │   │   │   ├── api/       通用错误响应等接口基础类型
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
| `plan` | 阶段目标、任务、按日安排、周月汇总及外部方案确认导入 | 已建 | 已建 |
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

`progress` 已建立 `controller/dto/service/mapper`，派生完成量放 DTO，不预建一份独立完成量实体。课程目录与归属元数据由本模块服务管理，不修改计划或原始学习记录。

SQL XML 放在 `backend/src/main/resources/mapper/<module>/`，与对应 Mapper 接口的 namespace 一致；初始化时配置加载路径。数据库迁移集中在 `db/migration/`，不能给每个模块各自从 `V1` 开始编号。

`plan/advisor/` 预留业务层的建议接口及其输入输出类型，未来由 `ai/` 实现。当前不预建模型 SDK 配置或工具执行代码。

前端除预留的 `ai` 外，各功能按需使用 `api/`、`components/`、`views/`：分别放业务接口调用、功能内组件与页面。功能专用的类型或状态按需放回该功能目录，共享目录只放真正跨功能的内容。

2026-10-18 暗紫主题：`styles/variables.css` 维护全站与 Element Plus 共用的语义色，`styles/index.css` 负责通用控件、弹窗和可访问性；`styles/layout.css` 组织桌面侧栏、今日插画区及手机底栏，`styles/today.css` 负责今日页。`components/AppIcon.vue` 提供统一细线图标，`ArchiveArtwork.vue` 使用 `assets/theme/` 中桌面插画、手机插画和旧纸污蚀三张独立 WebP 素材。插画不包含任务、统计或文字，业务界面仍由真实 DTO 渲染。手机“更多”由原布局内的 Element Plus Drawer 承载，所有现有路由保持可达；素材进入应用外壳预缓存，不扩大个人数据缓存范围。

后端测试已按八个当前业务模块建立同名包。AI 的测试与真实模型评估在智能阶段建立并隔离执行。

## 实际创建的工程文件

手动交付增加 `deploy/compose.bundle.yaml`、`deploy/nginx/bundle.conf.template`、
`deploy/scripts/build-bundle.ps1`、`export-local-database.ps1` 与 `bundle-*.sh`。
这是同一应用的上传产物部署方式，不新增服务模块；服务器不执行 Maven/pnpm 编译。
生成的程序包位于被忽略的 `release/`，个人数据备份在被忽略的 `deploy/backups/`，二者分离。
启动顺序与 HTTPS、备份恢复说明见 `docs/operations/manual-deploy.md`。

工程基础与业务模块已实现，下表文件均已创建并经过真实构建与测试：

| 位置 | 文件 |
| --- | --- |
| `backend/` | `pom.xml`、`mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties`（Maven 3.9.16） |
| `backend/src/main/java/com/kaoyan/study/` | `StudyApplication.java`（`@MapperScan` + `@ConfigurationPropertiesScan`） |
| `backend/src/main/resources/` | `application.yml`、`application-dev.yml`、`application-prod.yml` |
| `backend/src/main/resources/db/migration/` | `V1`–`V22`（V13 会话大小写归一化、V14 今日用时与部分复习、V15 计划工作区与导入防重、V16 课程目录与学习归属、V17 接续题目、V18 时间型安排、V19 每日完成确认、V20 按书学习接续、V21 实际复习数量、V22 自填题号；V22于2026-10-18上线） |
| `backend/src/main/java/com/kaoyan/study/config/` | 安全（Spring Security 7 + JDBC 会话 + CSRF）、OpenAPI、预设账号配置 |
| `frontend/` | `package.json`、`pnpm-lock.yaml`、`index.html`、Vite/TypeScript/Vitest 配置 |
| `frontend/src/` | `main.ts`、`App.vue` 及各功能页面 |
| `deploy/` | `compose.yaml`、`.env.example`、`docker/*.Dockerfile`、`nginx/default.conf`、`scripts/*.sh` |
| `.github/workflows/` | `ci.yml`（提交触发）、`release.yml`（手动触发的生产发布） |
| `docs/decisions/` | 已记录 5 项工程决策（版本锁定、Boot 4 模块拆分、复习参数可配置、复习安排落点、仅记录用时） |

`progress` 的实际完成量只从学习记录推导，目录结构和归属是新增的持久化元数据，不能成为第二份完成量事实。`plan/advisor/` 仍为智能阶段的预留位置。

记录回看由 `learning/views/LearningView.vue` 和 `RecordArchiveTree.vue` 组织，月/周/日/课程汇总由 `RecordArchiveService` 生成。记录页独立使用 `RecordEditorForm.vue`、`recordEditorValidation.ts`，通过 `RecordClassificationDialog.vue` 更正目录归属。今日保留原有 `RecordForm.vue` 及 `validation.ts`，两页表单演进互不影响。

进度首页 `ProgressView.vue` 只显示四个方向；`DirectionProgressView.vue` 根据路由及 query 组织分科、课程和完整目录。`CourseRing` 显示课程完成比例，`CatalogNodeTree` 展示内容，`CatalogEditorDialog` 与 `CatalogImportDialog` 共用后端预览确认流程。`CatalogService` / `CatalogMapper` 管理本模块目录和归属，通过学习业务服务检查原记录版本；旧 `ProgressMapper` 的区间查询保留兼容。

LeetCode课程详情的 `completedProblemNumbers` 由 `CatalogMapper` 只读关联自填题号与有效完成记录，`CatalogService` 沿用明确课程归属汇总、去重排序；前端在完成数量下方仅展示数字。无需迁移，不保存第二份完成事实；题号明细不写入原进度概览快照。

记录与进度共用 `styles/retrospective.css`；记录日期筛选使用 `PeriodFilter.vue`。进度概览通过原 offline store 保存，`useCatalogWorkspace` 不新增本地缓存层。目录说明、Schema 与示例统一放在 `docs/business/progress-catalog*`，由 Vite 和 Docker 构建打包。

旧 `RecordTimeline`、`EChart`、`SubjectProgressChart`、`TrendChart` 和无调用方的 `progress/api/index.ts` 已移除。当前进度请求集中在 `progress/api/catalog.ts`，课程环形图使用 SVG，无需 ECharts；旧进度响应类型仍供离线快照兼容读取，后端区间统计 API 仍保留。`useFormat` 的 `amountText` 与 `weekStartOf` 被新页面使用，必须保留。

Docker 构建上下文取仓库根目录（`deploy/compose.yaml` 中 `context: ..`），
手动发布工作流也使用仓库根目录（`context: .`）。
因此 Dockerfile 内路径以 `backend/`、`frontend/`、`deploy/` 开头，仓库根的 `.dockerignore`
排除 `.git`、`node_modules`、`target`、备份与环境文件。

写学习记录和修改任务通过 `TaskService.requireExistingForUpdate` 共用任务行锁。
复习模块通过 `StudySettingsService.lockForReviewScheduling` 取得额度锁，不直接写设置 Mapper。
计划外复习通过学习业务服务核验 `sourceRecordId`；前端从已有学习记录选择来源。
备忘录转任务先锁定备忘录，已有转换结果直接返回；任务仍由计划业务服务创建。

`.gitkeep` 占位文件在对应目录已有真实文件后即失去作用，可按需删除，不影响构建。

今日清单的完成量与并发校验归 `learning/service/StudyRecordService`，每日预计用时归计划模块，部分复习与完成反馈归 `review/service/ReviewService`。`today/service/TodayService` 只组合这些业务服务并汇总分区、时间与任务项数，不写独立任务表；`today/dto/TodayTaskCounts` 返回顶部已完成、进行中、尚未开始的互斥统计。接口与规则见 [今日清单](business/today-checklist.md)。前端使用 `ChecklistTask`，已移除旧 `PlannedTaskTable` 与 `ReviewedTaskList`。

计划工作区由 `plan/service/PlanWorkspaceService` 汇总日安排，`PlanImportService` 校验外部 JSON 并调用任务、目标、日安排业务服务事务追加。`WorkspaceMapper` 的关联查询同日同任务只计算一次学习量，排除复习。旧周表/API 保留兼容，新页面不重复编辑周排期。旧 `WeekPlanPanel`、`DayPlanPanel` 已移除；日编辑统一使用 `DayScheduleEditor`。

前端 `features/plan/views/PlanView.vue` 组织四种时间视图；`DayScheduleEditor` 和 `PlanImportDialog` 负责日期编辑与文件预览。`workspace.ts` 只做类型和日历操作，统计由后端返回。本周日安排经 `offline/useOfflineSnapshot` 保存至原 week 切片的 schedule 字段，只读，不缓存完整工作区。导入规划书、Schema 和示例以 `docs/business/plan-import*` 为唯一源，Vite 和 Docker 构建均读取这些文件。

V17 增加任务序列及学习记录题目关联。`TaskSequenceService` / `TaskSequenceMapper` 属于计划模块，管理顺序、题名和链接；`SequenceLearningService` 属于学习模块，依据本模块记录推导当前题并校验每日名额。今日只保存当前展示题的只读快照，完整题目目录通过认证接口联网读取。

V18 增加 `task_activity` 时间型安排元数据，允许日计划量为空，并在学习记录保存当天结束标记。`TaskActivityService` / `TaskActivityMapper` 属于计划模块；学习业务服务检查复盘不能增量，依据有效记录推导当天状态。`TodayService` 只组合展示，不保存独立完成量。v3 导入仍由原 `PlanImportService` 事务应用，不新增模块或模型依赖。

V19 为 `task_activity` 增加 `CHECK_ONLY` 模式，v4 合同支持无数量、无用时的每日确认。`DayConfirmationRequest` 与 `StudyRecordService.confirmDay` 管理勾选／取消，任务行锁防止并发重复确认；revision 包含软删除历史以防完成／取消后旧设备重放。`StudyRecordView.activityMode` 区分有意不计时的确认，记录、今日和计划统计均不把确认当作新学数量或缺失用时。仍使用原学习记录及软删除机制，没有新增完成事实表。

V20 新增学习接续规划表与原学习记录的 `continuation_delta`。`TaskContinuationService` / `TaskContinuationMapper` 属于计划模块，仅维护书籍、内容顺序与参考日安排；`ContinuationLearningService` / `ContinuationProgressMapper` 属于学习模块，依据有效学习记录派生累计百分比和日期执行内容。今日使用 `ContinuationBook.vue` 按书收纳各段；不另存完成游标，不改变普通任务和原每日一题规则。导入 v5、Schema 与可下载首周样例仍放在原 `docs/business/plan-import*`，详见导入规划书。

2026-10-16 不计时记录：表单移除实际／预计用时和预算输入，保留历史字段兼容；不增加迁移或第二套完成事实。`ReviewConfirmRequest.manualSelection` 区分当前手动确认与旧额度模式。`DayProgressRequest` 和 `ReviewDayProgressRequest` 的用时允许为空；接续进度计算处理空估算。导入仍使用 v1—v5，时间字段可省略。

2026-10-16 英语实际数量：V21 新增原学习记录的 `review_amount`，首次学习仍用 amount，复习量不混入进度。v6 COUNT_STUDY／COUNT_REVIEW 由原导入服务校验；今日、记录页分别展示两类实际数量。计划模块 `TaskQuantityService`／`TaskQuantityMapper` 提供已有任务的预览转换，`QuantityModeDialog.vue` 位于任务库。转换使用任务与工作区版本，事务中保留原任务、日期、学习记录，清除固定计划量；不新增完成事实表。

2026-10-17 英语数量进度：`CatalogService.library` 的 activities 从原数量任务与有效学习记录生成累计数量、次数、天数及历史未知数量次数；`CountedProgressCards.vue` 在所属方向页展示，复习单列，不创建课程总量或额外事实表。概览进入原 `useCatalogWorkspace` 只读统计快照；旧快照没有 activities 时兼容读取。无表结构迁移。

2026-10-17 LeetCode 自填题号：V22 为原 task_sequence 添加 mode，SELECT_BY_NUMBER 使用原题目身份与学习记录。TaskSequenceService 按实际输入解析题号；SequenceLearningService 校验每日一题、首次题量与日期；ProblemNumberModeService 在计划模块组织预览转换，通过 StudyRecordService 修改历史题号。今日和 SequenceRecordItem.vue 增加题号输入，ProblemNumberModeDialog.vue 位于任务库；导入 v7 不预设 items，不新增完成事实表。


2026-10-18 学习日截止：学习日期规则归 `settings/service/StudyDay`，统一以 Asia/Shanghai 的09:00切换，供今日、序列执行、统计与复习服务调用；不放在 common。前端 `composables/useFormat.ts` 的 `todayIso` 使用同一规则，已有默认日期与离线快照共用。真实时间戳、日期运算和明确选中的历史日期不变；无需数据库迁移。

2026-10-19 LeetCode每日多题：学习模块的 `CompletedProblemNumbersRequest` 与 `POST /api/learning/records/problem-numbers` 接收批量完成题号；`StudyRecordService` 在任务锁与单个事务内保存每题原学习记录，派生子令牌处理整批重试。复用 `SequenceLearningService` 的题号去重、原进度与课程归属查询，不增加完成事实表或数据库迁移。今日页提供批量输入并保留追加入口。

2026-10-30 数学同日继续学习：`ContinuationLearningService` 从有效记录推导可自选的下一段，`Context.optional` 区分未开始的可选内容。`TodayService` 将其排除于还剩项数和预计负担；`ContinuationBook.vue` 的展开仅为本地界面状态，保存仍走原百分比记录入口，无新表或迁移。
