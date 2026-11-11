# 数学每次学习可选用时与导出（2026-11-11）

## 范围与行为

用户确认先实现可选用时记录，随规划上下文导出，积累后再讨论校准。此为PLAN中数学的不计时例外；其他科目、复盘与复习保留原方式。

- 今日数学完成／剩余、今日数学补记和记录页数学补记／修改增加本次实际分钟数，可留空。按科目归属识别数学，不根据任务标题判断。
- 每次只填本次有效学习用时、扣除休息；分次分别保存，与原记录ID、任务和进度增量关联。只记用时不增加内容进度；未知不填0、不从计划估算补造。历史0编辑时仍保留。
- 编辑可补填、更正、清空。原样失败重试复用令牌，改变用时换新令牌；后端拒绝同令牌的不同用时，保留原防重、版本与事务约束。
- 单次学习明细显示本次用时；JSON沿用 `facts.studyRecords[].durationMinutes`，Markdown明确显示本次实际用时。修改、清空或删除同步反映到导出及指纹。
- 每条记录不代表一份完整任务样本；同任务有未计时部分时，已知分钟之和不是完整总耗时。混合内容不会自动拆分阅读／做题用时。
- 不实现校准建议、固定采样阈值、自动改估算／重排或模型调用。无需新迁移。

## 实际验证

前端目录执行：

- `pnpm test -- tests/unit/todayChecklist.spec.ts tests/unit/recordForm.spec.ts tests/unit/recordEditorForm.spec.ts tests/unit/mathDurationForms.spec.ts`：4文件76项通过。
- `pnpm typecheck`：通过。
- `pnpm test`：20文件184项通过。覆盖选填、未知值、分次记录、失败重试与更换令牌、非数学不显示、补记／编辑／清空、旧0值保留及原有离线／学习日流程。
- `pnpm build`：类型检查、Vite及PWA构建通过。

后端目录执行（Java21.0.6、Maven3.9.16）：

```powershell
# 指向此次全新、仅监听本机13326的独立MySQL8.0.45测试实例。
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13326/math_duration_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=ContinuedStudyIntegrationTest,PlanningContextExportIntegrationTest,LearningRecordSafetyIntegrationTest,TodayChecklistIntegrationTest' test
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' -DskipTests package
```

- 首次完整应用V1–V26，50项测试通过、0失败／错误／跳过：接续19项、记录安全14项、上下文导出7项、今日清单10项。
- 新增真实数据库场景验证40%／45分钟与后续60%／70分钟分别保存、只记10分钟不推进、不同用时重用令牌拒绝、编辑／清空不改变百分比、旧版本修改拒绝、删除重算及参考安排不被重排。
- 导出场景验证45分钟和未知用时原样保留、JSON字段及Markdown列存在、仅修改用时改变指纹而不改变完成量、删除后不再导出；导出不写业务数据。
- 后端打包成功；打包命令跳过测试，测试结果以上一条实际测试命令为准。日志保留于忽略目录 `release/math-duration-20261111/`。
- 临时实例完成测试后通过mysqladmin正常关闭，测试数据与正式学习库隔离。

仓库根执行 `node tools/check-doc-links.mjs`：存在5处已有的 `source-handoff-clean-20261103.md` 缺失引用；本轮新增文档链接有效。相关源码 `git diff --check` 通过。

## 交付边界

后续用户已授权并完成正式部署，限定发布源码及真实网站结果见[数学用时发布记录](../../operations/releases/math-duration-release-20261111.md)。下段保留首次本地实现完成时的范围；上线状态以后续发布记录为准。

本次仅本地实现与验证，无提交、推送或部署；未修改正式计划和学习数据。未进行真实浏览器或HarmonyOS真机验收，不能以组件测试及构建代替线上验收。保留开始任务时已有的并行修改。
