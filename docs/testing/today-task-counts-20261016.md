# 今日顶部任务项数验收（2026-10-16）

用户确认在 develop 将截图中的顶部时间概览替换为“已完成 / 进行中 / 还剩”三列任务项数。部分完成以及仅记用时的未完成任务归进行中，还剩指尚未开始。学科分区与任务内用时继续显示。首次完成本地开发验证后，用户另行要求部署；已完成真实发布，见 [发布记录](../operations/today-task-counts-release-20261016.md)。发布验收时尚未提交；实现、测试及发布记录现纳入本次 develop 提交。

## 实现

- `TodayService` 基于当日清单、有效学习记录和复习统计返回 `taskCounts: { completed, inProgress, remaining }`；按书接续沿用跨日累计百分比，不把书籍分组计为额外任务。
- 不增加数据库状态或迁移，不合计不同单位的学习量。完成和修改／删除原记录后读取时重新计算；CHECK_ONLY 取消后恢复未开始。
- 今日快照保存服务端项数，旧快照缺少字段时提示联网刷新，离线保持只读。

## 已执行验证

前端在 `frontend` 目录执行：

```powershell
pnpm test -- tests/unit/todayChecklist.spec.ts
pnpm run build
pnpm test
```

结果：今日组件 18 项通过；全量前端 10 文件、89 项通过；`build` 中的 `vue-tsc --noEmit` 与 Vite/PWA 生产构建通过。

后端在 `backend` 目录使用 Maven Wrapper 3.9.16、Java 21.0.6 执行：

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' -DskipTests package
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:33416/today_task_counts_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=TodayChecklistIntegrationTest,ConfirmationActivityIntegrationTest,TimedActivityIntegrationTest,ContinuedStudyIntegrationTest,SequenceLearningIntegrationTest' test
```

结果：编译、测试编译和 jar 打包通过；5 类、35 项真实 MySQL 8.0.45 集成测试通过，无失败、错误或跳过。隔离实例仅监听 `127.0.0.1:33416`，使用本次新建的一次性库，空库执行 V1–V20，重跑时通过 20 份迁移校验。

新增断言覆盖：多来源安排不重复计数、计划外记录不增加任务数、只记用时的零比例任务归进行中、完成与删除后重算、无任务、复习只记用时、CHECK_ONLY 确认取消、时间型学习结束及删除重开、按书具体内容计数与跨日部分进度。第一次运行发现测试库中其他接续计划也会出现在未来日期，已将断言改为相对当前基线验证，随后 35 项全通过；未修改真实业务规则来迁就测试。

## 页面检查

使用本次构建的 `frontend/dist`、临时无头 Chrome 和固定模拟 API 数据检查布局；这是本地界面验收，不是线上数据验收。

- 1366px、390px、320px 下，三个标题顺序与数字正确，同一行无重叠，顶部无时长文字。
- 已目视检查桌面概览与窄屏全页截图，无浏览器运行错误。
- 本地脚本、后端日志和截图位于被忽略的 `release/today-task-counts-20261016/`。

以上是首次开发验收结果。后续发布验证与正式站结果见下文及对应发布记录；既有启动命令保持不变。

## 发布构建补充验证

用户随后明确要求部署。正式打包脚本 `./deploy/scripts/build-bundle.ps1 -Offline` 首次运行时，138 项测试通过，2 项 `SessionTableCaseMigrationTest` 因本机缺少可用 Docker 报错，故该次打包中止，未将失败包用于上线。

使用新建的一次性 `today_release_full_test` 库，将依赖 Linux Docker 的专测明确排除后执行：

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=*,!SessionTableCaseMigrationTest' verify
```

结果：138 项测试通过，无失败、错误或跳过，jar 打包成功。这不代表两项 Docker 专测已通过。前端沿用本次已通过的 89 项测试及类型检查／生产构建产物，源代码未再变化。

生产 MySQL 8.4 的隔离恢复与受限候选应用验证通过，31 张业务表逐表哈希一致；候选验证今日项数、匿名 401、无 CSRF 403、登录、会话和退出。正式切换后业务行不变、服务健康、HTTPS 入口与构建一致，真实网站 PWA 更新后显示三列任务项数。详情与备份、产物哈希见上述发布记录。
