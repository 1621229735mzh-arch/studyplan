# 数学教材按页码填写进度（2026-11-11）

## 交付范围

用户要求教材使用页码时直接填写学到第几页，省去手算完成百分比。今日数学教材连续阅读段的“剩余”默认按印刷页码输入，可切换百分比表达页内进度；“完成”仍直接表示整段完成，可选用时保留。

- 后端根据原接续资料及明确的教材／印刷页码范围给出 `continuation.pageRange`。不读取PDF页码；合并连续范围时不重复计算共用边界页。题册、独立习题、混合阅读与独立练习、范围缺失、歧义或不连续时不提供页码模式。
- 请求沿用 `/api/learning/records/day-progress`，`completedPage` 与 `completionPercent` 二选一。按含首尾的覆盖页数换算累计比例，保留两位小数并向下取整，末页为100%；只保存相对原记录的增量。原始页码和范围以 `[教材页码]` 说明存入原记录的note，随原规划上下文导出，不建立另一份完成事实或新增迁移。
- 如第25—27页分次完成：25页为33.33%，26页为66.66%，27页为100%，实际增量分别33.33、33.33、33.34。百分比接口兼容原整数请求，并支持两位小数；切回百分比时保留原33.33%，只补用时不会被取整为34%。
- 失败重试保留页码与用时，原样重试不重复生成记录，改变页码换令牌；后端拒绝不同页码复用令牌、越界、倒退、版本过期及覆盖后续日期进度。删除后由有效原记录重算。历史百分比不反推精确页码，不改参考安排和预计用时。

## 实际验证

前端目录：

```powershell
pnpm test -- tests/unit/todayChecklist.spec.ts
pnpm typecheck
pnpm test
pnpm build
```

- 今日清单50项通过，新增页码默认入口、页码与用时载荷、失败保留与重试令牌、切回百分比、保留小数进度、整段完成及旧数据兼容场景。
- 全量20文件187项通过；类型检查、Vite与PWA构建通过。小数精度补强后另复跑今日清单50项与构建。

后端使用Java21、Maven Wrapper，以及仅监听本机13327端口的全新MySQL8.0.45实例，独立库 `math_page_test` 完整应用V1—V26：

```powershell
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13327/math_page_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
.\backend\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' -f backend/pom.xml '-Dtest=ContinuedStudyIntegrationTest,TodayChecklistIntegrationTest,PlanningContextExportIntegrationTest,TextbookPageProgressTest' test
.\backend\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' -f backend/pom.xml -DskipTests package
```

- 42项通过、0失败／错误／跳过：接续21、今日10、导出7、页码及请求校验4。页码换算与原记录、防重、同页只加用时、切回小数百分比只加用时、到末页完成、删除重算、越界、倒退、混合任务拒绝及后续记录保护均在真实MySQL验证。
- 后端打包通过；打包跳过测试，测试结果以上一条命令为准。首次沙箱连接临时实例受限，改为本机隔离测试后通过；临时MySQL已通过mysqladmin正常关闭。
- 本次修改文件的 `git diff --check` 通过。`node tools/check-doc-links.mjs` 仍有原先5处缺失的 `source-handoff-clean-20261103.md` 引用，无新增缺失链接。

2026-11-12后续：用户授权部署，在11月11日上传后暂停，11月12日继续并于11:13:48上线。限定发布源码前端191项、真实MySQL42项及构建、备份恢复、正式网页页码／百分比切换及用时输入验收通过，48条学习记录和全部业务数据保持。见[正式发布记录](../../operations/releases/math-page-release-20261112.md)。

原开发验证日志保留在忽略目录 `release/math-page-20261111/`；限定发布源码和证据在 `release/math-page-deploy-20261111/`。后续用户已授权将限定源码和相关文档提交、推送，具体版本以Git历史为准；未进行HarmonyOS真机验收。并行的记录安全修复、安卓和文档整理改动保留。
