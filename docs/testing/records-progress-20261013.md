# 记录与进度重构验收（2026-10-13）

> 本文记录第一轮验收。用户随后确认了月周日记录与课程目录进度的新结构，第二轮实现及当前证据见 [第二轮验收](records-progress-catalog-20261013.md)，不要将本报告作为当前页面的最终验收。

## 交付范围

记录页改为日期时间线和补记/修改弹窗；进度页按科目展示同期日安排与实际学习量，趋势和同单位对比按需查看。详情见 [业务说明](../business/records-progress-workspace.md)。本次没有数据库迁移，没有操作业务库或部署生产。

## 自动验证

前端在 `frontend/` 执行：

```powershell
pnpm typecheck
pnpm test
pnpm build
```

全部通过：9 个测试文件、**71 项测试**。新增验证包括空任务列表时编辑历史记录、内容与数量精度保留、保存失败重试令牌、修改输入后更换令牌、提交中防重、版本冲突保留输入、过期响应保护、日期区间校验、身份错误不回退缓存、旧口径快照阻止展示、缓存写入失败保留在线结果。

按用户最新要求，记录页使用独立的 RecordEditorForm / recordEditorValidation；今日使用的原 RecordForm、validation 及其测试恢复为 HEAD，今日和计划无本轮代码差异。上述类型检查、71 项测试与构建均在拆分后重新执行通过。

后端在 `backend/`、独立 MySQL **8.0.45** 的 `retrospective_test` 库执行：

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' '-Dtest=ProgressWorkspaceIntegrationTest,PlanLearningFlowIntegrationTest,TodayChecklistIntegrationTest,StudyFlowApiIntegrationTest' package
```

**31 项测试通过、0 失败**。其中新增 5 项 ProgressWorkspaceIntegrationTest 覆盖同期日安排、多个安排来源不重复完成量、排除复习安排与未排期任务总量、独立记录、缺失用时、单位分离、编辑删除重算、归档任务的历史安排、只记时长趋势。

该次 package 的最后 repackage 步骤因本机预览 Java 占用 jar 而失败。关闭预览进程后执行以下命令，最终 **BUILD SUCCESS**；没有重复执行已通过的测试：

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' -DskipTests package
```

未运行全部后端测试或 Docker 专用 SessionTableCaseMigrationTest；没有本次 MySQL 8.4 验证。历史 Flyway V1–V15 未修改，隔离测试库通过迁移校验。

## 浏览器验证

使用本机隔离库 `retrospective_preview`、测试账号、headless Edge + Playwright；桌面 1440×1000，手机 390×844。通过以下场景：

- 登录，记录日期分组，任务关联记录修改；`1.123456` 保存后精度和学习内容均保留。
- 手机无任务关联补记：选择科目、单位、完成量与用时；修改完成量后接口统计更新，删除后记录及对应统计消失。
- 本周与自选日期切换；未来无记录/无进度日期显示空状态。
- 桌面与手机进度、趋势、同单位对比图，检查无水平溢出。
- 日期范围原双月弹层超过手机宽度，已改为独立原生日期输入并验证；弹窗限制在视口内，表单内容可滚动，标题和关闭按钮可见。
- 已加载页面离线后可读进度快照及真实时间范围；趋势提示联网，记录写入禁用；恢复网络自动重载。
- 拆分前曾检查今日页共用补记表单可展开、无水平溢出；最终已恢复今日原表单，此项不作为最终今日页面的新增验收。
- 浏览器脚本未发现页面 JavaScript 异常。

浏览器检查在记录表单独立拆分前执行；拆分保留新编辑器实现，拆分后未重跑浏览器检查。这里的离线检查针对已打开的 SPA 页面和快照切换，不是新一轮 Service Worker 离线冷启动验收；应用外壳缓存实现未变动。

## 证据与收尾

本地脚本、截图和日志在被忽略的 `release/retrospective-20261012/`：

- `browser-check.cjs`、`browser-flow.cjs`、`browser-final.cjs`。
- `desktop-records.png`、`desktop-progress.png`、`desktop-trend.png`。
- `mobile-records.png`、`mobile-progress.png`、`mobile-trend.png`、`mobile-record-form-final.png`。
- `mobile-date-picker.png`、`mobile-empty-progress.png`、`mobile-comparison.png`、`mobile-offline-progress.png`。
- `backend-final.log`、`backend-package-final.log`。

本次结束前已核对命令行后关闭 Vite 15174、Java 18082、隔离 MySQL 33312，三个端口均无监听；测试数据和日志保留。Git 改动未提交、未推送；原有部署文件改动和其他任务的计划删除交接文件均保留。
