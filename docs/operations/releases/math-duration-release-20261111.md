# 数学每次学习可选用时发布

2026-11-11 20:44:37（北京时间）已部署到[正式网站](https://onlystudy.loc.cc/)。用户授权部署并自行上传差分包；恢复Workbench窗口后完成发布。部署时尚未提交或推送Git，未重启整台服务器；后续用户授权将本次限定源码及相关文档提交并推送，具体版本以Git历史为准。

## 上线行为

- 今日数学学习的完成／剩余弹窗可选填本次实际分钟数，部分完成也能填写；留空为未知，分次分别记录。
- 数学补记及原记录修改可填写、补填、更正或清空用时，与原任务和进度增量一起保存。只记用时不推进内容进度。
- 单次学习详情显示本次用时；JSON沿用 `facts.studyRecords[].durationMinutes`，Markdown明确显示本次实际分钟数或未记录。
- 当前只积累样本供后续讨论校准，不自动改估算或排期。其他科目、复盘和复习保留原方式。

## 限定范围与构建

基于最近一次线上[数学名称与接续修复](math-plan-display-carryover-20261111.md)的源码快照，在独立目录覆盖本次16个源码／测试文件。学习服务只加入新用时的重试一致性检查，保留其他已部署代码；未夹带工作区中并行的记录安全Mapper／DTO修复、TaskPanel、配置、安卓或文档整理。

新旧JAR逐条比对只有以下3个class内容不同：`StudyRecordService`、`PlanningContextExportService`、`PlanningContextMarkdown`。全部依赖、迁移、Mapper和其他资源逐字节一致；无新增数据库迁移。

实际发布源码验证：

- 前端 `pnpm test`：21文件188项通过；`pnpm build` 类型检查、Vite和PWA构建通过。保留线上源码快照中的旧兼容测试，故数量不同于开发工作区的184项。
- 后端在本机独立 `math_duration_release_test` MySQL8.0.45测试库完整应用V1—V26后运行 `mvnw.cmd -o -B -Dmaven.repo.local=... -Dtest=ContinuedStudyIntegrationTest,PlanningContextExportIntegrationTest,TodayChecklistIntegrationTest test`：19＋7＋10，共36项通过，0失败／错误／跳过。
- `mvnw.cmd -o -B -Dmaven.repo.local=... -DskipTests package`：打包通过；测试结果以上一条为准。临时本机MySQL实例随后正常关闭。
- 此限定包未纳入并行安全修复的14项测试。开发工作区原验证另见[实现验证](../../testing/reports/math-duration-20261111.md)，不能用工作区产物替换本次限定发布包。

## 备份、切换与真实网站验收

上传包位于服务器 `/math-duration-transfer.tar.gz`，SHA256与本机一致。解压到 `/root/math-duration-20261111/`，运行 `python3 hydrate.py`，重建完整前后端产物并校验全部文件；随后执行 `python3 release.py prepare`。

预备阶段备份旧JAR、前端及正式库，在现有MySQL中新建隔离恢复库 `math_duration_restore_20261111`，按原字节恢复备份、逐行比对业务表与迁移历史通过。未启动第二个数据库或候选Java服务。读取备份保留二进制字段，切换隔离库与输入dump分开执行。

执行 `python3 release.py publish`：短暂停止后端，另做最终备份与业务快照；安装644权限的候选JAR并强制重建后端。健康后重载Nginx，按资源先于index／Service Worker的顺序发布前端，保留旧hash资源。三个服务健康，HTTPS入口哈希匹配，匿名业务API返回401。

真实网站点击“立即更新”后，验证数学剩余进度弹窗的可选用时、输入／清空与记录页的历史数学补填入口。表单取消退出，未保存演示学习数据。正值保存、用时修改／清空、并发版本、防重与删除重算由上述真实隔离MySQL测试覆盖，未把页面打开当作正式库写入验证。

从真实规划页下载同一快照的JSON与Markdown，核对47条学习记录、1条复习记录、用时规则、实际用时列与一致数据指纹 `fade1c7441b5e7d174b49022ad02ea0e3be93998d96213738381621e88d0613c`。下载完成于本机；浏览器下载事件监听超时，但实际文件已落盘并验证，未重复生成数据。

发布前后和全部页面查看／下载之后，除会话表外全部业务表、账号及Flyway历史逐行一致，47条学习记录完整保留。`UI-VERIFIED.json`记录最终数据复核。没有更改计划或学习事实，也没有生成测试复习记录。

## 产物与回退

| 产物 | SHA-256 |
| --- | --- |
| 旧JAR | `da76f3e2f64236b98ea3172f0c14f82d9aee52f7a1969e68e513b821ebe05450` |
| 新JAR | `1a3ae4b55ac3be3286359ff8aa91edfa673d26ecc861ad4e4a6520f05956f06f` |
| 新index | `0b42d5092854966c96b0fa9151e2e4fc6b5f5ef4e8dc189811a456360fc00e07` |
| 114843字节差分包 | `51fc013fb5d72f9088cf7506e9ae569802aa569431ca1a8f0cdecef8febf5706` |
| 最终数据库备份 | `d86002ef5e7c09531d10ecdf7da471ac6665a047845d86fe6775d33a31dae15e7` |

服务器恢复点 `/srv/408doing/private/math-duration-20261111/`，包含旧前后端、`PREPARED.json`、`PUBLISHED.json`、`UI-VERIFIED.json`及前后业务快照。最终备份为 `final-backup/study-20261111-204351.sql.gz`，gzip与哈希验证通过。

程序回退使用恢复点的旧JAR强制重建后端、恢复旧前端入口并重载Nginx；不为程序回退覆盖发布后新增的真实学习记录。此次使用既有字段，没有结构或迁移变更。不要重复运行已完成的publish。

本机限定源码、范围、完整差异、构建日志、下载文件和真实页面截图均在忽略目录 `release/math-duration-deploy-20261111/`。未进行HarmonyOS真机验收。本次发布文档链接检查仍有原先5处缺失的源码交接报告引用，新增发布链接有效。
