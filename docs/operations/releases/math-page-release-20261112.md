# 数学教材按页码填写进度发布

2026-11-12 11:13:48（北京时间）已部署到[正式网站](https://onlystudy.loc.cc/)。11月11日上传包后按用户要求暂停；11月12日用户要求继续，恢复登录后完成备份恢复、切换和真实页面验收。未重启整台服务器。发布时尚未提交或推送；随后用户授权将本次限定源码及相关文档纳入Git交付，具体版本以Git历史为准。

## 上线行为与范围

- 连续教材阅读段的“剩余”默认填写“已学完到第几页”，使用印刷页码，后端换算原累计百分比，保留原始页码说明和可选用时；页内进度可切回百分比。
- 页码范围只采用原资料中明确、连续的教材阅读范围；题册、独立习题、混合任务和范围不明确的内容保留百分比。原完成、用时、防重、并发及删除重算规则保留，不改参考日期和估算。
- 基于[上次数学用时发布](math-duration-release-20261111.md)源码快照，只覆盖10个源码／测试文件。学习服务只替换本次进度用例，DTO保留原已部署的数量上限；未夹带工作区记录安全Mapper／DTO修复、TaskPanel、配置、安卓或文档整理。
- 新旧JAR仅6个条目变化：`DayProgressRequest`、`StudyRecordService`、`ContinuationLearningService`及其`Context`、`TextbookPageProgress`及其`Range`。依赖、迁移、Mapper及其他资源逐字节一致，无新迁移。

## 发布源码验证

限定源码在 `release/math-page-deploy-20261111/source/` 独立构建，不使用整个工作区的产物：

- 前端 `pnpm test`：21文件191项通过；`pnpm build`：类型检查、Vite及PWA构建通过。数量与开发工作区的187项不同，包含原发布快照保留的兼容测试。
- 后端全新 `math_page_release_test` MySQL8.0.45测试库完整应用V1—V26，`mvnw.cmd -o -B -Dmaven.repo.local=... -Dtest=ContinuedStudyIntegrationTest,TodayChecklistIntegrationTest,PlanningContextExportIntegrationTest,TextbookPageProgressTest test`：42项通过，0失败／错误／跳过；`-DskipTests package`打包通过。
- 页码换算、部分与末页、同页只记用时、小数精度、防重、范围和进度校验、后续记录保护及删除重算的数据库验证见[实现报告](../../testing/reports/math-page-progress-20261111.md)。正式库不保存演示记录。

## 备份、切换和正式验收

核对服务器原JAR和HTTPS入口仍为上次版本；上传包 `/math-page-transfer.tar.gz` 与本机哈希一致。解压到 `/root/math-page-20261111/`，执行 `hydrate.py`，完整产物及全部文件校验通过。

`release.py prepare`备份原前后端和数据库，在现有MySQL实例的 `math_page_restore_20261111` 隔离库恢复，全部业务表、账号及迁移历史逐行比对通过；未启动第二个数据库或测试应用。48条学习记录保持。

`release.py publish`短暂停止后端，执行最终备份，再以644权限安装新JAR并强制重建后端；健康后重载Nginx，按资源先于入口／Service Worker的顺序更新前端，保留旧hash资源。三个服务健康，公网HTTPS入口与候选一致，匿名学习及计划接口返回401。

真实网站点击“立即更新”后，在当日线代教材第6—10页内容打开“剩余”：默认按页码，输入页码、实际键入可选用时并失焦后保留，百分比切换正常；《1000题》原百分比入口保持。清空并取消演示表单，未保存完成事实。截图在本机 `release/math-page-deploy-20261111/online-page-dialog.png`。未进行HarmonyOS真机或本轮手机尺寸验收。

页面验收后再次读取全部业务表，与最终切换前快照逐行一致，48条学习记录、账号及Flyway历史均保持，三个服务仍健康。服务器 `UI-VERIFIED.json`已记录结果；读取和会话表变化不视为学习事实变更。

## 产物与恢复点

| 产物 | SHA-256 |
| --- | --- |
| 原JAR | `1a3ae4b55ac3be3286359ff8aa91edfa673d26ecc861ad4e4a6520f05956f06f` |
| 新JAR | `4f39f391dfae7af00da394d9e213f5e13669040d5da2287766a6b364818989e0` |
| 新index | `d71bd87b863bca049895fd6f8b8f932c6cdb62ec8681b43c90a923bc66958cdf` |
| 94752字节差分包 | `510dbfc50aaea89f51015798e93764671fa810b951f797b5fce6d89549a589cb` |
| 最终数据库备份 | `ac2a54bcfb2c9fe37d6e0bcc359132af9dc6f4f3ed1f5c5f3b0c282803daa46d` |

恢复点 `/srv/408doing/private/math-page-20261111/` 保留旧前后端、`PREPARED.json`、`PUBLISHED.json`、`UI-VERIFIED.json`和业务快照；最终备份 `final-backup/study-20261112-111307.sql.gz`。目录日期沿用包准备日，实际发布时间以上述11月12日为准，禁止重复执行publish。

程序回退使用恢复点旧JAR强制重建后端，恢复旧前端入口并重载Nginx；此次无结构变更，不为程序回退覆盖发布后新增的真实学习记录。本机源码、包、哈希及构建日志保留于忽略目录 `release/math-page-deploy-20261111/`。
