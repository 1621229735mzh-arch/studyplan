# 验收项与验证方式

## 2026-09-29 会话表跨平台修复（最新）

- 问题：Windows 导出的 `spring_session` / `spring_session_attributes` 在 Linux
  `lower_case_table_names=0` 下与程序使用的大写表名不匹配，匿名业务 API 返回 500。
- 新增 V13，只重命名会话表，保留 V2 及其校验和，不删除表或业务数据。
- 执行 `backend/mvnw.cmd -B -Dmaven.repo.local=../.m2repo -o verify`（工作目录 backend）：
  67 项测试通过，0 失败/错误/跳过；新增测试分别覆盖大小写敏感与不敏感 MySQL，
  验证会话及属性保留、读写、删除、清理、迁移重复执行；原测试覆盖全新库 V1–V13。
- 原 SQL 的隔离恢复副本修复前 `/api/today` 实测 500，修复后匿名 401；
  `release/test-restored-session.ps1` 验证无 CSRF 登录 403、带 CSRF 测试账号登录 200、
  会话读取、今日/任务 200、退出 204、退出后 401。
  测试仅绑定本机 127.0.0.1:18081，并为 HTTP 测试覆盖 Secure Cookie；生产配置未改。
- 业务数量仍为 30 任务、1 阶段目标、15 周计划、113 每日安排、0 学习记录。
  原备份哈希不变，Windows MySQL80 未修改。测试副本新增了虚拟登录账号，不用于生产导入。
- 前端未修改，本轮未重跑前端测试；生产服务器尚未应用修复，原账号登录仍待用户验证。

## 此前验证记录

2026-09-28 手动部署准备补充：本地已启动 Docker Desktop，执行
`./deploy/scripts/build-bundle.ps1 -Offline`。后端在 Testcontainers MySQL 8.4.11
通过 65 个测试（0 失败/错误/跳过），V1–V12 迁移成功；前端 31 个测试与生产构建通过。
独立的 `study-smoke-20260928` 项目运行部署包，三服务 healthy；本地自签 HTTPS
登录、CSRF、Secure Cookie、未登录 401、管理端点 404 均通过。
同日晚，用户实际导出的备份通过包内 `bundle-import.sh` 恢复到另一独立 MySQL 8.4 项目，
后端健康检查 UP、Flyway V12 校验通过；30 个任务、0 条学习记录符合用户确认的情况。
原备份哈希未变化，源 MySQL80 未修改。公网证书签发、服务器上线、原账号浏览器登录与离线流程尚未完成。
详细命令、包路径与恢复位置见 [部署交接](../operations/DEPLOYMENT-HANDOFF.md)。

对应 `PLAN.md` 第五节列出的重点验证项。状态列只反映**实际执行过**的验证。

2026-09-22 修复后验证：Windows、Java 21.0.6、Maven Wrapper 3.9.16，
使用独立临时 MySQL 8.0.45 实例（仅监听 `127.0.0.1:33308`），没有连接现有业务库。
数据库从空库迁移至 V11，新增回归覆盖任务单位/科目保护、删除引用、复习来源、并发额度与备忘录转换。

本次实际执行的后端命令（在 `backend` 目录，PowerShell）：

```powershell
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:33308/study_review_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD='' # 仅用于这次隔离的、运行结束即停用的临时实例
.\mvnw.cmd -v
.\mvnw.cmd -B "-Dmaven.repo.local=../.m2repo" verify
# 首轮 63 个测试通过；补充 HTTP 契约用例后，使用已缓存依赖完整重跑：
.\mvnw.cmd -o -B "-Dmaven.repo.local=../.m2repo" verify
# Tests run: 64, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

前端实际执行 `corepack pnpm test`、`corepack pnpm run typecheck`、`corepack pnpm run build`，
最终 5 个测试文件 / 31 个用例通过，类型检查及生产构建通过。
发布工作流的两个构建上下文与 Dockerfile 的全部本地 COPY 来源已静态核对。
本次未运行 Docker 镜像构建、MySQL 8.4 或 Playwright，不能据此宣称这些验证通过。

常规环境复现命令：

```bash
# 后端：真实 MySQL 上的单元 + 集成 + 端到端测试
cd backend
export TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/kaoyan_study_test?allowPublicKeyRetrieval=true&useSSL=false'
export TEST_MYSQL_USERNAME=kaoyan TEST_MYSQL_PASSWORD=...
./mvnw -B verify          # 当前包含 64 个测试；需要可用的真实 MySQL

# 前端：类型检查、单元测试、生产构建
cd frontend
pnpm install              # 依赖按 package.json 固定版本安装，生成 pnpm-lock.yaml
pnpm run typecheck        # vue-tsc --noEmit 通过
pnpm run test             # 5 个测试文件 / 31 个用例
pnpm run build            # 构建成功，生成 dist 与 PWA Service Worker
```

此前交付记录还包含一次打包后 jar 的部署形态验证（本次未重复）：在空库上启动 → Flyway 建出 19 张表（V1–V11 全部成功）
→ `/actuator/health` 返回 UP → 未登录访问业务接口 401 → 缺 CSRF 的登录 403 → 带 CSRF 的登录成功并可读取今日数据。

## 后端业务规则

| 验收项 | 状态 | 对应测试 |
| --- | --- | --- |
| 任务分多天完成时累计数量正确 | 通过 | `PlanLearningFlowIntegrationTest.accumulatesAcrossDays` |
| 修改学习记录后进度同步变化 | 通过 | `PlanLearningFlowIntegrationTest.progressRecalculatesAfterUpdate` |
| 删除学习记录后进度同步变化 | 通过 | `PlanLearningFlowIntegrationTest.progressRecalculatesAfterDelete` |
| 创建计划不增加完成量 | 通过 | `PlanLearningFlowIntegrationTest.planDoesNotIncreaseCompletion` |
| 不同单位不混加 | 通过 | `PlanLearningFlowIntegrationTest.unitMustMatchTask`；进度接口按 `(科目,单位)` 分组，不提供跨单位合计 |
| 复习不重复增加首次学习量 | 通过 | `ReviewFlowIntegrationTest.reviewDoesNotAddToFirstTimeStudyAmount` |
| 重复提交不会生成重复记录 | 通过 | `PlanLearningFlowIntegrationTest.duplicateSubmissionIsIdempotent`、`ReviewFlowIntegrationTest.duplicateReviewSubmissionIsIdempotent` |
| 未完成任务不会自动堆到次日 | 通过 | `PlanLearningFlowIntegrationTest.unfinishedItemsAreNotCarriedOver` |
| 复习建议不越过额度 | 通过 | `ReviewFlowIntegrationTest.overQuotaItemsStayPending` |
| 未经确认不进入今日安排 | 通过 | `ReviewFlowIntegrationTest.confirmRequiresConfiguredQuota`（未确认时复习项保持 `PENDING`，不进入 `SCHEDULED`） |
| 缺少用时或预算时不宣称满足额度 | 通过 | `ReviewFlowIntegrationTest.missingEstimateMustBeSupplied`、`confirmRequiresConfiguredQuota` |
| 复习间隔为待定参数，不写死 | 通过 | `ReviewFlowIntegrationTest.nextDateFollowsConfiguredInterval`（未配置时返回空日期而不是编造日期） |
| 未登录无法读取学习数据 | 通过 | `AuthenticationIntegrationTest.businessApiRequiresAuthentication`、`StudyFlowApiIntegrationTest.businessEndpointsRequireLogin` |
| 退出后服务端会话失效 | 通过 | `AuthenticationIntegrationTest.logoutInvalidatesSession` |
| 门户数据保存在服务端（跨设备读同一份） | 通过 | `AuthenticationIntegrationTest.loginStoresSessionInDatabase`（会话行写入 MySQL）；业务数据本身即单一数据库 |
| 修改/调整的并发保护（多设备编辑检查） | 通过 | 各服务 `*_STALE` 冲突路径；`TargetServiceTest`、`MemoServiceTest` 覆盖版本过期 |
| 只记时长的记录只计入时长、不计入完成量 | 通过 | `PlanLearningFlowIntegrationTest.timeOnlyRecordCountsDurationOnly` |
| 有历史记录的任务禁止修改科目/单位 | 通过 | `recordedTaskCannotChangeUnitOrSubjectEvenAfterSoftDelete`；未学习任务仍可修改 |
| 软删除记录与非任务外键引用仍阻止删除 | 通过 | `softDeletedAdHocRecordStillProtectsUnitAndSubject`、`subjectWithoutTasksStillChecksOtherReferences` |
| 计划外复习必须关联真实、未删除的学习记录 | 通过 | `adHocReviewRequiresARealNonDeletedSourceWithMatchingSubject`、`reviewCreationOverHttpRequiresLearnedSource` |
| 两事务持有旧快照时同时确认也不超额 | 通过 | `concurrentConfirmationsShareQuotaEvenWithEarlierTransactionSnapshots` |
| 备忘录并发转换与重复请求只创建一个任务 | 通过 | `MemoConversionIntegrationTest.concurrentConversionAndRetryCreateOnlyOneTask` |
| 端到端闭环可用 | 通过 | `StudyFlowApiIntegrationTest.fullFlowWorksOverHttp` |

## 前端

代码通过类型检查（`vue-tsc --noEmit`）、31 个测试与生产构建。下列行为由测试覆盖；
**浏览器内的实际操作流程尚未用 Playwright 跑过**（`frontend/tests/e2e/README.md` 只写了应覆盖的场景）。

| 验收项 | 状态 | 验证方式 |
| --- | --- | --- |
| CSRF 请求头与 403 重试逻辑 | 单测通过 | `tests/unit/csrf.spec.ts` |
| 离线快照只含今日/本周/进度且带同步时间 | 单测通过 | `tests/unit/offlineSnapshot.spec.ts` |
| 历史/未来日期不覆盖今日，跨日不误用旧快照 | 单测通过 | `tests/unit/offlineSnapshot.spec.ts` |
| 计划外复习选择记录来源，提交失败保留输入 | 组件测试通过 | `tests/unit/reviewSource.spec.ts` |
| 缺额度/缺用时不得宣称“符合额度” | 单测通过 | `tests/unit/reviewQuota.spec.ts` |
| 表单校验与失败后保留输入 | 单测通过 | `tests/unit/recordForm.spec.ts` |
| 离线只读、退出清理本地缓存、PWA 只缓存外壳 | 待浏览器验证 | 逻辑已实现于 `src/offline`、`src/pwa`，需人工或 Playwright 验证 |

## 尚未实现（按计划留到智能阶段）

| 项 | 说明 |
| --- | --- |
| AI 计划建议、只读工具、结构化输出与业务校验 | 阶段五，当前未安装 Spring AI 依赖；`plan/advisor/` 为预留位置 |
| 模型异常/超时不改变原计划 | 随智能阶段一并实现与验证 |
| 错题录入方式 | PLAN 中明确待定，第一版不实现拍照识别或题库 |

## 每日完成确认（2026-10-15）

ConfirmationActivityIntegrationTest 在真实MySQL8.0.45通过5项：确认／取消／重试、并发同日单记录、删除历史防重放、统计不增加词量或缺失用时、拒绝其他写入口造数量与时间。相关后端63项回归通过，前端84项通过；类型检查和构建通过。隔离HTTP导入两份123天方案以及真实浏览器无弹窗确认、取消通过。生产候选及正式导入须另行验收，不以本机通过替代线上结果。
