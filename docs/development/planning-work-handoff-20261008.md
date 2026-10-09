# 计划页改版暂停交接（2026-10-08）

## 最新状态：已完成本地验收并上线

用户已授权发布，2026-10-08 21:45:31 部署至 https://onlystudy.loc.cc/plan 。生产 MySQL 8.4.11 的隔离恢复及 V14 → V15、登录/会话/CSRF/计划接口检查通过；切换前停写备份，16 张业务表数量保持一致。Nginx 与三个生产容器健康，公网新版文件和鉴权检查通过，真实浏览器已查看四视图及今日。详见 [发布记录](../operations/planning-workspace-release-20261008.md)。

恢复后完成了导入确认、日新增/编辑/排序/重复提示/移除、手机四视图及本周离线验收。手机月历改为显示每日安排数量，点击日期进入完整日列表；按日期列表汇总的本周快照经原 offline 入口保存，离线禁止写入。旧新增日安排 API 不再覆盖已有条目，特殊来源只能通过专用入口新增。

最终后端 92 项测试及可执行 jar 打包成功，前端 49 项测试、类型检查与生产构建成功。PLAN.md、README、STRUCTURE、接口及数据库文档已同步日期优先流程；导入规划书/Schema/示例已完成。本地浏览器真实运行并目视检查截图，详见 [本轮验收记录](../testing/planning-workspace-20261008.md)。

2026-10-09 按用户要求将本轮计划页代码及相关文档整理为 develop 提交；未推送远端，原部署改动与 stash 保留。Docker 专用会话迁移测试及 Docker 镜像构建未执行；生产固定镜像的挂载运行、Nginx 配置和 MySQL 8.4 升级已实际验证。下方两次暂停内容为历史记录，不能作为当前待办。

验收结束已按命令行核对并关闭临时 Java/Vite 与 33309 MySQL；33309、18089、15173 无监听，截图与日志保留在忽略的 release 目录。

## 第二次暂停：额度不足（本节覆盖下文旧状态）

用户恢复开发后再次要求尽快暂停。代码已保存于 develop 工作区，未提交、推送或部署；原有部署改动和保留 stash 不动。下文是第一次暂停时的历史记录，不能据此判断当前完成状态。

### 本轮已实现

- PlanView 已改为整体/月/周/日四视图：默认月时间轴、科目筛选、月历、周历（手机纵向）、日任务编辑/移除/上下排序；任务库和阶段目标保留次级入口。
- DayScheduleEditor 支持当天新建任务或选择已有任务、预计用时、重试令牌。PlanImportDialog 支持原始 JSON 文件/粘贴、下载规划书/Schema/示例/目录、服务端预览、勾选确认追加；失败保留输入。
- 规划书、Schema、示例已在 docs/business/plan-import* 保存。内置 AI 无依赖、无调用、无生成入口。
- 后端新增 PlanWorkspaceIntegrationTest（19 项），补充删除 version 校验、复习条目边界、导入逐条追加优化、重复提示查找优化；末次修正新增任务排在已排序列表末尾、数量 1 与 1.0 的内容哈希归一化。
- 导入窗口 busy 时禁止 Escape 关闭，分页属性改 size="small"，今日链接修正为 /。这几处最后的小改尚未重跑前端验证。
- Vite 增加可选 STUDY_API_PROXY；Nginx 导入路径提高 body 上限至 5m（原始文件仍严格限制 2,000,000 UTF-8 字节）。Dockerfile 与 .dockerignore 增加导入文档打包路径；Docker 未验证。

### 真实验证状态

- 独立 MySQL 8.0.45 已执行 V1–V15；V15 现已应用，不得改写，需要更改 schema 时新增迁移。
- 后端曾完整 verify 成功：91 项测试通过。最后一轮排序/哈希修正后测试仍 91 项全通过，但 repackage 因预览进程占用 jar 导致 Windows rename 失败；恢复后关闭占用并重新 package/verify，不能把本轮最终打包称为成功。
- 命令（backend）：设置 TEST_MYSQL_URL 指向 127.0.0.1:33309/planning_workspace_test，TEST_MYSQL_USERNAME=root、TEST_MYSQL_PASSWORD 为空（仅隔离临时库），执行 `.\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' '-Dtest=*,!SessionTableCaseMigrationTest' verify`。原有 Docker 专用 SessionTableCaseMigrationTest 两项未执行，Docker 不可用。日志 release/planning-verify-20261008/backend-verify.log。
- 前端 48 项测试、类型检查及 build 曾全部通过；最后 UI 小改后待复跑。日志同目录 frontend-build.log。
- Headless Edge 已真实登录隔离预览、显示整体时间轴和导入可读预览，截图 desktop-overall.png、desktop-import-preview.png 已保存，但尚未逐张目视审查。
- UI 脚本 browser-check.cjs 在 `.getByRole('checkbox').check()` 卡住：Element Plus 原生 input 隐藏，需要点击可见 checkbox label 再断言 checked。未完成确认导入/手机布局/日编辑浏览器验收，不能宣称通过。
- 示例科目“数学一”不等于预览库实际“数学”；首次被校验正确拒绝。浏览器测试替换为“数学”后预览成功；正式导入必须按下载目录填写准确名称。
- 工具 Node REPL/CUA 运行失败，可用依赖包 Playwright 经 shell 启动 headless Edge。PLAYWRIGHT_MODULE 指向 Codex bundled node_modules/playwright（通过 load_workspace_dependencies 获取，不写死进正式代码）。

### 恢复后的最短路线

本次暂停已按命令行核对并停止预览 Java/Vite，通过 33309 端口 mysqladmin shutdown 关闭临时 MySQL；停止后 33309、18089、15173 无监听。代码保存在磁盘，Git diff --check 通过。

1. 先看本节和 Git 状态，保留所有未提交代码与原有部署变更。读取 PLAN.md / AGENTS.md / STRUCTURE.md。
2. 核对临时进程实际状态；隔离 MySQL data 在 release/planning-verify-20261008/mysql-data，端口 33309；预览库 planning_workspace_preview，后端 18089、前端 15173。旧 PID 不可直接复用，先核对命令行。
3. 修正 browser-check.cjs 点击可见确认 label；重启最新 jar（最好复制到 release 下运行以免锁 target），继续手机/桌面和新增/编辑/排序/导入完整验收。补查快速新增后排序、加载状态和离线周视图是否保留既有只读能力。
4. 复跑前端测试/build、后端 package（必要时 verify）；审查旧 DailyPlanService.addItem upsert 的版本边界、导入与其他写路径的一致性。
5. **PLAN.md 尚未更新到用户认可的日期优先规划流程；README、STRUCTURE、API/数据库文档和本轮验收报告也未完成同步。** 继续时必须补齐，保留原部署历史。
6. 最终 diff --check，说明实际验收与未验证部分。未授权部署或推送。

---

用户要求先暂停以便关机。本文件记录未完成工作；不是验收报告。恢复时先读本文件、根 PLAN.md、AGENTS.md 和 docs/STRUCTURE.md，再检查实际工作区。不要重新询问下列已确认需求。

## 已确认的需求

- 计划页是制定、调整计划的地方，默认进入整体视图；每日执行及学习记录留在今日页。
- 顶部固定整体、月、周、日导航，支持直接切换与点击月份、日期进入详情。
- 整体：按月份时间轴展开，科目可筛选。
- 月：本月概览加月历，点击日期进入日详情。
- 周：电脑七列日历，手机按日期纵向排列。
- 日：可调整顺序的任务列表，显示学习内容、计划量、实际进度；日期下直接新增，也可选已有任务。
- 视觉：浅色、适当留白、清晰文字层级，少量颜色突出重点。
- 用户自己按日安排；周展示日安排的集合，不再重复维护周排期。
- 外部 AI 从整体逐层拆解到月、周、日；完整区间全部细化到具体日期，不采用相对第几天。
- 外部 AI 按仓库内的导入规划书生成 JSON；网站校验、预览，用户确认后追加，不覆盖已有安排；提示疑似重复项，拦截同方案重复导入。
- 手动安排与确认后的外部方案进入同一份日安排。
- 内置 AI 当前封锁：不安装依赖、不调用模型、不展示生成入口，仅保留未来扩展边界。
- 完成量仍来自学习记录；不同单位不混加；复习不增加首次学习量；未完成不自动顺延。

## 分支及原有改动

- 当前分支 develop，已经从 e198f54 快进到本地已有 origin/develop 的 10cd044，包含今日清单的两项更新。没有执行网络 fetch、提交、推送或部署。
- 开始时工作区有部署相关改动，全部保留。README.md 和 docs/STRUCTURE.md 的 stash 恢复冲突已手动合并，Git 冲突状态已清除。
- 保留了名为 `preserve deployment edits before develop planning work` 的 stash，暂不删除；包含快进前的 README、STRUCTURE 和原先未跟踪的 V13 文件。恢复时按 stash 名称确认，不盲用固定序号。
- 既有未跟踪文件包括手动部署配置/脚本、部署记录及 SessionTableCaseMigrationTest。不要误删或把这些当成本次新增。

## 本次已保存的代码（均未提交）

- 新增 V15__planning_workspace.sql：工作区修订号、导入批次及内容哈希、月/周说明、日条目排序、快速新增提交令牌。
- 新增 WorkspaceMapper + XML：同日同任务合并读取，实际完成量只查一次；规划查询排除 REVIEW，复习仍由现有模块管理；旧周表未删除。
- 新增 PlanWorkspaceService、PlanWorkspaceController 和相关 DTO：时间视图汇总、快速新增事务、完整日条目版本排序、导入预览和确认端点。
- 新增 PlanImportDocument、PlanImportService：版本 1 JSON 严格解析、日期全覆盖（休息日空 items）、已配置启用的科目/单位、目标和任务引用、任务总量与日安排之和校验、疑似重复预览、内容/ID 防重复、预览快照校验和事务追加。
- DailyPlanService 增加工作区锁及修订号更新；PlanMapper 的日读取按排序号排序。
- PlanSource 新增 IMPORT，前端对应类型及来源文字已同步。
- 从 origin/develop 快进后编译发现 StudyRecordController / ReviewController 缺少 Operation、Tag、Valid 导入，已补回这六个 import；没有改动今日业务实现。

## 实际验证及运行状态

- 在 backend 执行 `.\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' -DskipTests compile`：BUILD SUCCESS，135 个源文件编译成功。
- **没有运行本次业务测试、数据库迁移验证、前端类型检查/测试/构建或浏览器验收。不能称为功能完成。**
- 初始化过独立 MySQL 8.0.45 数据目录 `release/planning-verify-20261008/mysql-data`，仅监听 127.0.0.1:33309；没有连接现有业务库，没有在测试库执行迁移。
- 暂停前已通过核对进程命令行并调用该端口 mysqladmin shutdown，关闭此临时实例。
- mysql.pid 和输出日志留在同目录（被忽略）。PID 可能已被系统复用，恢复时不能直接按旧 PID 停进程；须核对命令行和数据目录。
- Maven 使用真实项目 Wrapper 3.9.16，Java 21.0.6。已有 target 文件在沙箱内写入受限，编译用执行审批后成功；不是自动审批拒绝。

## 恢复后优先完成

1. 审查当前后端草稿，补齐合同/事务/并发/类型错误测试，再启动隔离 MySQL 测试库执行 V1–V15 真实迁移与回归。V15 尚未部署，现阶段可修正草稿；任何已应用历史迁移不可改。
2. 检查快速新增已有任务路径、请求防重、删除版本校验与复习编辑边界；全面核查所有排期写路径与预览快照的一致性。当前原日删除 API 未新增客户端 version 检查。
3. 实现前端四视图、时间导航、日直接新增/编辑/排序及 JSON 上传、可读预览、确认、错误保留输入。**PlanView 目前仍为旧四标签表格页面，尚未改版。** 原任务库和阶段目标能力应保留在次级入口。
4. 编写仓库内外部 AI 导入规划书、JSON Schema、合法完整示例与可给外部 AI 使用的提示词；提供下载与当前科目/单位信息。文档必须与后端 DTO/校验一致，不能声称任意 AI 输出保证成功。
5. 更新 PLAN.md 的规划流程和本轮业务规则（用户当前要求优先于原流程），同步 README、STRUCTURE、相关接口/测试文档，保留现有部署内容。
6. 运行后端真实 MySQL 测试、前端类型检查及测试/构建，验证桌面和手机浏览；记录真实结果。没有授权发布生产，不自行部署。

## 当前 JSON 草稿约定（最终以代码和补充测试为准）

顶层：schemaVersion、planId、title、startDate、endDate、goals、months、weeks、tasks、days。

- goals：key、subject、title、description（可空）、startDate、endDate。
- months：month（YYYY-MM）、title、description（可空）。
- weeks：startDate（周一，可为与区间重叠的第一周）、title、description（可空）。
- tasks：key、subject、unit、title、goalKey（可空）、plannedAmount。
- days：date、items；整个起止区间每天出现一次；items 为 taskKey、amount、estimatedMinutes（可空）的数组。
- 月/周只保存目标说明，数量由日安排汇总；任务总量必须等于其全部日安排之和。
- 科目和单位使用已配置且启用的准确名称；不自动创建目录。
- 预览请求 `{document: 原始JSON字符串}`；确认请求增加 previewToken。不要把文档先 JSON.parse 后再回传，服务端需要检查原始重复字段。
- resource limits 目前草稿为 2,000,000 字符 / 5000 个日 / 20000 个任务或安排；这是输入资源保护，不是学习预算规则；还需核对代理限制及最终规划书。
