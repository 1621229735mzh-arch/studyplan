# 记录与进度重构暂停交接（2026-10-12）

> 2026-10-13 后续需求已改变，并完成第二轮本地实现与验收。请先读 [第二轮交接的最新状态](records-progress-handoff-20261013.md)。下文“完成与验收”只适用于第一轮，当前工作区证据以第二轮验收为准。

## 最新状态（2026-10-13，已完成本地实现与验收）

用户随后明确要求“今日和计划不要动”。已将记录页表单独立为 RecordEditorForm.vue / recordEditorValidation.ts，今日使用的 RecordForm.vue、validation.ts 及原有测试恢复为 HEAD，今日与计划页面文件均无本轮差异。下文早期“共用表单改造”不再代表最终实现。

暂停后已继续完成：修复任务列表测试 mock、完善提交令牌行为、修复手机日期范围弹层和弹窗滚动，并同步 PLAN/README/STRUCTURE/API 与业务说明。

- 表单独立拆分后，前端类型检查、71 项完整测试、生产构建通过；真实 MySQL 8.0.45 上 31 项后端相关测试通过，jar 最终打包成功。
- 桌面/手机记录操作、进度联动、空状态、日期筛选、同单位比较、已加载页面离线只读已验证；浏览器验证在表单独立拆分前执行，拆分后未重跑。今日原表单已恢复，保持现状。
- 15174/18082/33312 本次临时服务已关闭，端口检查无监听。
- 未提交、推送或部署；不处理其他对话中的生产计划删除。
- 最终结果与限制以 [验收记录](../testing/records-progress-20261013.md) 为准。

以下为 2026-10-12 的历史暂停记录，其中“2 项失败”“浏览器未验收”等状态已被上面的完成结果取代。历史记录保留供追溯。

## 用户意图

- “今日”和“计划”已经体现用户的思路；延续这两页的信息组织、操作路径与视觉层级，优化或重构记录、进度，不只是统一配色。
- 记录回答“学了什么，需要补记或修正什么”；进度回答“各科推进到哪里，是否需要调整计划”。这些是本次实现采用的方向，尚待浏览器验收和用户体验反馈。
- 另一个同步任务将删除现有所有学习任务。本任务不执行数据清理、不操作业务库；必须适配无任务、独立学习记录、空进度场景。恢复时检查其他任务的新改动，不覆盖它们。
- 既有约束保持：人工按日安排，周月汇总；今日负责执行；学习记录决定完成量；单位不混加；内置 AI 封锁；离线只读。澄清时一次问一个问题。

## 分支和文件

- 分支 `develop`，当前 HEAD `6fae842`。本轮没有提交、推送或部署。
- 开始前已有 `.gitignore`、README、STRUCTURE、部署与验收文档改动，以及未跟踪部署脚本和 SessionTableCaseMigrationTest；均保留，不纳入本次修改。
- 本轮已修改：
  - `frontend/src/features/learning/views/LearningView.vue`：默认本周，时间范围/科目/任务筛选，日期时间线，补记/编辑弹窗，请求序号阻止旧响应覆盖，空/离线状态。
  - `learning/components/RecordTimeline.vue` 替代 `RecordTable.vue`（旧文件已删除）：日期倒序，内容、完成量、用时、复盘分层；先显示 14 个有记录日期，可加载更早内容。
  - `learning/components/RecordForm.vue`、`types.ts`、`validation.ts`：保留学习内容；无任务可选科目/单位；编辑允许原记录的零/缺失用时；数量精度 6 位；复盘长度与后端 1000 字对齐；提交中禁用输入和取消。
  - `progress/views/ProgressView.vue`：各科卡片优先，同期日安排对比；学习趋势为次级视图；同单位柱状对比可展开；返回记录/计划/今日入口。
  - `progress/components/SubjectProgressChart.vue`、`TrendChart.vue`：绿色系，减少技术说明，用时/完成数量切换展示。
  - `progress/types.ts`：离线概览的 `planningBasis`、`syncedAt` 可选字段。
  - 新增 `components/PeriodFilter.vue`、`styles/retrospective.css`，供两页共用。
  - `backend/src/main/resources/mapper/progress/ProgressMapper.xml`：修复原先“全部任务总计划量 vs 日期内实际量”的口径，改为日期区间的 daily_plan_item，排除 REVIEW，记录独立聚合防重复；无日安排量/未填时长保留 null。无迁移变更。
  - 新增后端 `progress/ProgressWorkspaceIntegrationTest.java`，前端 `tests/unit/retrospective.spec.ts`；调整 `recordForm.spec.ts` 复盘长度测试。

## 实际验证

- `pnpm typecheck`：最后一次通过。第一次暴露 Element Plus null v-model 类型问题，已修复。
- `pnpm build`：最后一次通过，Vite 和 PWA 产物已生成。
- `pnpm test`：最新 **56 通过、2 失败（总计 58）**。原 49 项仍通过。新测试两个失败都发生在打开记录弹窗，`planTasks.value.map` 收到 undefined。
  - 位置：`retrospective.spec.ts` 的“无任务列表仍能修改”“补记失败保留输入”两项。
  - 优先检查 mock：`listPlanTasks: vi.fn().mockResolvedValue([])` 只在 vi.mock 工厂配置，而 Vitest `restoreMocks: true`；应在 beforeEach 显式重新设置 `listPlanTasks` resolved value，与现有 planWorkspace 测试保持一致。此项还未修复和重跑，不能断言只是测试问题。
- 独立真实 MySQL **8.0.45** 上运行以下命令通过，**31 项通过，0 失败**，包括新增 5 项进度统计测试：

  ```powershell
  # backend/；环境变量指向下述一次性测试库
  .\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' '-Dtest=ProgressWorkspaceIntegrationTest,PlanLearningFlowIntegrationTest,TodayChecklistIntegrationTest,StudyFlowApiIntegrationTest' test
  ```

- 测试覆盖：同期日安排/多个来源/区间边界，不计未排期任务总量和复习量，实际完成量不被关联重复，独立记录空计划量，编辑删除统计，单位分离，移除安排后记录保留，归档已有日安排，只有用时的趋势。
- 后端首次测试夹具失败：普通日安排服务禁止 IMPORT/REVIEW，改为 JDBC 插入这两种测试夹具；事务中直接 JDBC 删除导致 MyBatis 一级缓存未失效，改用正式日安排服务移除后通过。不是生产逻辑绕过。
- `git diff --check` 通过（只有 LF/CRLF 转换提示）。
- **未进行浏览器桌面/手机验收，未完成最终文档同步，未做完整后端测试或 jar 打包。**

## 临时服务已关闭

- 本轮新建隔离目录：`release/retrospective-20261012/mysql-data`，只监听 `127.0.0.1:33312`，库 `retrospective_test`；未接触已有业务数据库。
- 暂停前核对 mysqld 命令行包含本轮数据目录后，执行该端口 mysqladmin shutdown；后续进程查询和监听查询均无结果。没有启动 Vite 或独立 Java 应用；Maven/Vitest/Vite 构建均结束。
- 日志 `release/retrospective-20261012/backend-tests.log` 保留，目录被 Git 忽略。
- 恢复时不能依赖旧 PID。需要测试时启动同一隔离数据目录，或另建独立测试库。Test URL：`jdbc:mysql://127.0.0.1:33312/retrospective_test?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai`。该本机一次性实例采用 initialize-insecure，仅 root 空密码，用后关闭，不用于部署。
- 沙箱无法读取已有 node_modules，需要获准执行 pnpm；Java target 写入及 mysqld 启动也用了工具执行审批。没有修改权限或全局 safe.directory。

## 恢复后顺序

1. 读本文件、PLAN.md、AGENTS.md、docs/STRUCTURE.md；核对 Git 状态和并行任务改动，不重做访谈。
2. 修复两项前端测试并重跑；核验旧记录字段、无任务编辑、输入保留、防重、并发冲突、离线禁写。
3. 浏览器检查桌面/手机、空任务/空记录/空进度、日期切换/筛选、补记/修改/删除、进度联动、断网快照（显示真实区间和独立同步时间）。使用独立测试数据。
4. 检查产品细节：当前进度仍沿用学习记录统计，复习用时独立在复习模块，今日总用时包含复习；应明确展示范围，评估是否需统一，而不是假定两者完全相同。数量输入固定 6 位小数的视觉也待看实页。记录表单为今日共用，须验证今日的补记入口。
5. 检查进度缓存失败逻辑：旧口径快照禁止冒充日安排对比，401/403 不回退缓存，趋势不写入快照；网络异常无缓存时错误文案可改善。
6. 同步 PLAN/README/STRUCTURE/API 或业务说明和验收记录，保留原有部署段落；最终 diff 检查后报告实际结果。当前没有本轮 Git 提交或部署授权。

不要继续沿用 2026-10-08 早期暂停文档中“V15 未部署”的旧状态；V15 已部署，不可改写。
