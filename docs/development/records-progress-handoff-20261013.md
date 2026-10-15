# 记录与进度第二轮重构：暂停交接（2026-10-13）

## 后续发布状态（2026-10-13 19:23）

用户授权部署后已完成生产发布、MySQL 8.4.11 隔离恢复与 V16 迁移验收、公网资源和真实原账号页面检查。生产新版本运行，业务表原有数量保留；没有 Git 提交或推送。详情及备份/回退证据见 [发布记录](../operations/records-progress-release-20261013.md)。下文“没有部署”描述本地实现交接时的历史状态，不代表最新线上状态。

## 继续后的最新状态（2026-10-13，本地实现与验收完成）

用户已明确要求继续，本文件下方的“未接通”清单与“未执行验证”是暂停时的历史记录，已由本节取代，勿据此回退代码或重复实现。

- 已补齐目录 API、快照类型、方向详情、分科／课程路由、完整目录、环形图、导入／手动维护／计划候选及任务关联界面。
- 记录页月周日归档接通，单次明细支持原表单编辑、删除及课程归属更正；待归类记录也可在方向详情确认归属。
- 目录导入说明、Schema、示例及下载入口已写入；新文件由 Vite 与 Docker 构建打包，未调用内置 AI。
- 完成量不另存，课程各末级条目分别封顶计算完成度；实际累计保留真实学习量。追加或修改总量先预览确认，保留既有记录。
- 前端 9 个文件 76 项测试通过，类型检查／生产构建通过；最后候选空白行修正后构建和浏览器路径验证通过。
- 本机真实 MySQL 8.0.45 上 44 项相关后端测试最终通过（31 项兼容与核心闭环 + 13 项目录）；jar 打包通过，隔离库 V1—V16 迁移校验通过。
- 桌面／手机完整导入确认、归类、目录追加、月周日回看、记录编辑、候选整理、任务归属解除与恢复、离线概览／重连通过；手机截图检查与排版修正完成。
- 本次隔离服务 15174／18082／33312 均已关闭，并核验无监听。代码、测试数据与证据保留。
- 今日／计划及原 RecordForm／validation／原测试仍无 Git 差异。没有提交、推送、部署或处理其他任务的生产删除。
- 最终验证命令、失败修正过程和限制见 [第二轮验收](../testing/records-progress-catalog-20261013.md)；完整业务见 [工作区说明](../business/records-progress-workspace.md)。记录用时折线图仍为待细化想法。

如后续继续，只需先检查实时 git diff 与本节，然后按用户新请求工作。未运行全部后端／Docker／MySQL 8.4 检查，也未重新做 Service Worker 离线冷启动验收。

---

以下保留原暂停检查点：

用户在实现过程中要求“准备暂停”。已停止功能开发；以下是当前真实检查点，不代表第二轮已交付。

## 重启后先读

1. 完整阅读 `PLAN.md`、`AGENTS.md`、`docs/STRUCTURE.md`，再读本文件。
2. `git -c safe.directory=D:/Project_JAVA/408doing status --short` 与 `git diff`；保留工作区全部改动。
3. 本轮停在 `develop`，HEAD 为 `6fae842`，没有提交、推送或部署。
4. 当前代码包含第一轮未提交实现及第二轮半成品。第一轮验收报告不能证明当前第二轮代码已通过测试。

## 已确认的最新产品方案

用户已退出 grill-me，并明确授权“按这个开始修改代码”。继续时无需重新进行已完成的设计访谈。

- 今日与计划保持现状。本轮不得修改其页面、原有记录表单或业务操作路径。
- **记录**：月 → 周 → 日 → 学科、课程当日汇总 → 单次记录，只包含实际学过的内容。
- 默认展开本月与本周，其他月份收起。月份展示学习天数及已记录累计用时；缺少用时须明确标注，不冒充完整时长。
- 同一天同一课程多次学习先合并，展开看原始记录；支持补记、修改、删除，统计同步变化。
- **进度**首页只显示数学、英语、政治、408 四个方向入口，不在首页堆具体学习内容；点击跳转方向详情。
- 方向 → 分科 → 课程／资料 → 完整目录。展示学了什么、多少、剩余内容，不以日期组织。
- 课程分母是完整课程/资料总量，不是目前已排期数量。完整目录含未安排内容。
- 从已有计划整理候选，由用户确认课程归属、目录和总量；不通过标题猜测或悄悄自动建正式课程。
- 目录支持规范文件导入 → 预览 → 确认，以及手动维护。追加目录或变更总量先展示差异，确认后更新；已有完成量保留。
- 关联明确的任务学习记录自动累计；未归类记录先进入学科累计，明确归类后计入课程，不重复计量。
- 每门课程可以用环形图显示已完成/剩余；不同单位不混加。记录页用时折线图仅提出了想法，尚未细化，不应声称已实现。

## 本轮已经写入的代码

### 后端

- 新增 `V16__create_progress_catalog.sql`：课程、完整目录节点、任务归属、单条记录归属、全局目录版本及持久化预览。没有改写 V1—V15，也没有执行迁移到业务库。
- 新增 `progress/dto/CatalogContracts.java`、`mapper/CatalogMapper.java`、`service/CatalogService.java`、`controller/CatalogController.java`。
- 目录合同暂定：`schemaVersion: 1`，courses 含 key/direction/branch/title/subjectId/unitId/nodes；节点含 key/parentKey/title/totalAmount/sortOrder/taskIds。
- 章节容器没有数量；末级节点有完整数量，课程总量由末级相加。同课程固定一种单位；不跨单位求和。key 稳定，重复导入是按 key 更新，不累加复制。
- 预览有 15 分钟期限，确认检查目录版本、关联任务及科目/单位上下文；确认重试按同一个 token 防重。导入为追加/按 key 更新，未出现在文件中的旧节点保留。
- 一项任务仅关联一个末级目录；明确归属只保存关联，实际数量查询未删除 `study_record`，修改/删除自动重算。
- 显式记录归属优先；任务或科目/单位改动后不沿用不匹配的归属。
- 学习模块新增 `StudyRecordService.lockForClassification` 与 Mapper 行锁方法：归类通过学习业务服务检查记录版本，不绕过服务写学习记录。
- 新增 `learning/dto/RecordArchive.java`、`service/RecordArchiveService.java`、`controller/RecordArchiveController.java`。
- 新 API `GET /api/learning/archive` 返回后端统计的月/周/日/课程组及原始记录。周从周一开始，跨月周在各自月份分段，不重复计量；缺少用时独立计数。
- 新 API 前缀 `/api/progress/catalog`：读取、任务候选、预览、确认、记录归类、目录任务关联。

### 前端

- 新增 `RecordArchiveTree.vue`：原生 details 逐级展开，默认本月、本周，单次明细可修改/删除。
- `LearningView.vue` 已改接 `getRecordArchive`，初始查看全部日期，以月份组织；原有独立表单及失败保留输入逻辑保留。
- 学习 types/API 已加归档响应和读取函数。
- `PeriodFilter.vue` 已新增 initialPeriod，记录页设置 all。
- `ProgressView.vue` 已替换成四个方向纯入口页面。
- 新增 `catalog.ts` 目录类型及转换、`CourseRing.vue` 环形图、`useCatalogWorkspace.ts` 草稿（读取目录、概览快照、在线状态、过期请求保护）。
- Router 已声明 `/progress/:direction`。

## 必须先补上的未接通部分

**前端目前不是可交付状态，不要直接发布或声称可以完整预览。** 一次大补丁仅部分落盘，已逐文件检查，具体缺口如下：

1. `frontend/src/features/progress/api/catalog.ts` **尚不存在**，但 `useCatalogWorkspace.ts` 已引用它。补上 getCatalog/getTaskCandidates/previewCatalog/confirmCatalog/classifyRecord/linkNodeTasks。
2. `DirectionProgressView.vue` **尚不存在**，但 Router 已引用它。补齐方向详情、分科、课程选择、完整目录、课程环形图、待归类列表，以及明确的离线概览状态。
3. `frontend/src/features/progress/types.ts` 的 planningBasis 仍只有 `'daily'`，尚未增加 `'catalog'` 和可选 `catalog: CatalogSummary`。composable 草稿已使用新类型，需接齐。
4. 尚未建立目录导入/手工编辑/从计划候选整理/任务关联界面；尚未写导入说明、JSON Schema、示例和下载入口。它们应在 progress 模块，不改计划导入或今日。
5. `retrospective.spec.ts` 仍是上一版列表/周进度接口 mock 与断言，要适配新归档 API、展开行为、纯方向首页和目录详情；不要靠删掉关键测试掩盖缺口。原今日 RecordForm 与测试保持不变。
6. 本轮目录导入、归类、目录总量增减、版本/过期/防重、修改删除联动、单位不匹配、跨月统计等后端测试尚未写，也尚未执行真实 MySQL 验证。
7. 审核 `useCatalogWorkspace.ts` 的异步快照写入：状态请求已防过期，快照写入与 notice 也须防旧结果覆盖新结果；保留身份错误不回退缓存。只缓存概览，不缓存完整目录或原始记录。
8. 审核完整目录校验合并后的任务唯一性、叶节点变为容器的历史归属、空/超大文件及严格 JSON 类型；测试应覆盖整个预览确认事务回滚。

## 实际验证

- 后端执行 `backend/.\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' -DskipTests compile`。
- 初次编译因 java.time.Month 与 RecordArchive.Month 同名失败；显式导入 LocalDate/YearMonth 后重跑，**BUILD SUCCESS**（142 个源文件）。
- 本轮没有运行前端 typecheck/test/build；上述未接通路径和类型已知需要补齐。
- 本轮没有运行集成测试、没有启动数据库或浏览器预览，也没有检查 UI。第一轮 71 项前端/31 项后端测试结果属于本轮之前的版本。
- 本轮最后 `git diff --check` 通过，仅有 LearningView 的 CRLF→LF 提示。
- 已核对今日/计划前后端目录及原 RecordForm.vue、validation.ts、recordForm.spec.ts **无 Git diff**。
- 暂停时端口 15174/18082/33312 无监听。本轮没有临时服务需关闭；上轮测试数据/日志仍在 ignored `release/retrospective-20261012/`。

## 保留其他任务与旧实现

不得清理或重置这些已有改动：`.gitignore`、部署相关 README/STRUCTURE 内容、docs/operations/deploy.md、docs/testing/acceptance.md、SessionTableCaseMigrationTest、deploy/compose.bundle.yaml、部署脚本和运维交接。

另一任务的 `docs/operations/plan-deletion-pause-20261012.md` 与生产数据操作不属于本轮。不要续做删除、操作线上数据或连接业务 MySQL。

上一轮留存 `RecordTimeline.vue`、SubjectProgressChart/TrendChart 及相关测试不代表本轮最终页面；接通新结构后再判断复用或移除。不要现在为整理而删除。

## 文档收尾仍需完成

PLAN.md 已按用户确认替换第 2.2 节；业务说明/STRUCTURE/API/README 与旧验收报告尚未全部改到第二轮最终结构。旧交接顶部已指向本文件。待实际实现与验证后再更新完成结论，不要提前写通过。
