# 2026-10-14 接续练习发布与计划导入

## 范围

目标为 `https://onlystudy.loc.cc`，服务器 `8.217.241.16`，部署目录 `/srv/408doing/study-20260928-150827`。
本次增加 V17 任务有序目录与学习记录题目关联，保留现有今日、计划的信息层级。
个人计划覆盖 2026-10-14 至 2026-11-13，31 个每日名额，题号 31 至 61。
用户已授权部署和导入；本次未授权 Git 提交或推送。

## 产物与本地验证

- 发布包：`release/sequence-20261014-1535.tar.gz`，37,051,322 字节。
- 包 SHA256：`4c969a585a03fc227d9356daa262c75b94272c532755d92b5b63936b6ab7817c`。
- JAR SHA256：`cafccd646ee7abe39721f2615b371e2afcaade2a998714ff3f38c47116b84273`。
- MySQL 8.0.45：72 项相关测试通过；最终接续／今日 15 项重跑通过。
- 前端：76 项基线通过，最终接续／今日 10 项通过；类型检查与构建通过。未执行一次包含全部新增测试的完整前端命令。
- 本地浏览器已验证导入、尝试只计用时、完成后次日 32 题、两次记录合并及编辑保留原题身份。

## 服务器操作与故障恢复

1. 用户将发布包上传至 `/root`，本轮实际核对 SHA256 一致。
2. 旧 Chrome 标签持续 `Debugger unattached`；新建同一官方 Workbench 地址的标签，使用服务器已有免密连接恢复控制。没有修改 SSH 认证或浏览器安全设置。
3. 执行 `python3 sequence-release.py prepare`：生产 V16 和健康状态校验通过，生产备份与隔离 MySQL 8.4 恢复核对通过。
4. 首次 `candidate` 已迁移至 V17，但计划预览返回 400：缺少已启用的“数据结构”科目，未进行正式发布。
5. 通过真实网站设置新增“数据结构”，分类为 `408`；单位“题”已有且启用。没有调整预算与复习设置。
6. 使用 `resume-candidate.py` 继续已迁移的候选副本；不重跑迁移、不覆盖备份、不绕过发布校验。中文 SQL 校验需要显式 `SET NAMES utf8mb4`。
7. 候选输出三个成功标记：
   - `SEQUENCE_IMPORT_ATTEMPT_ADVANCE_RETRY_DELETE_MYSQL84_VERIFIED`
   - `CATALOG_ARCHIVE_PLAN_TODAY_AUTH_CSRF_SESSION_VERIFIED`
   - `MYSQL84_V16_TO_V17_CANDIDATE_VERIFIED`
8. 候选容器停止，测试数据库账号重新锁定；执行 `python3 sequence-release.py publish`，输出 `PUBLISH_DONE_V17_BUSINESS_COUNTS_PRESERVED`。

私有状态及备份目录为 `/srv/408doing/private/sequence-20261014-1525`。
原 `sequence-release.py` 及包内校验文件未修改；恢复脚本单独保存。

## 最终结果（2026-10-14 18:35，Asia/Shanghai）

- 发布后实时确认 Flyway `17:1`，MySQL、后端与前端全部 `healthy`；Nginx 配置检查通过。正式发布前后业务表计数一致。
- 用户在网站使用原账号登录；更新现有 PWA 应用缓存后，使用原始个人 JSON 完成正常“校验预览 → 核对 → 确认追加”流程，没有通过 SQL 或迁移写入个人计划。
- 线上新增 1 个任务、1 个阶段目标、31 条日安排、1 个导入条目、7 条月／周纲要；其他原业务表计数保持发布后基线。
- `plan_key = ds-leetcode-daily-20261014-20261113-from31`，日期 2026-10-14 至 2026-11-13，31 个不同日期，计划量合计 31，31 条预计用时均为空。
- 有序目录 31 条，唯一题号范围 31–61；有效学习记录仍为 0，未增加任何生产测试学习记录。
- 真实页面核对：整体页 10 月 18 题／11 月 13 题，均已学 0；今日显示 LeetCode 31 题 HTTPS 链接与“记尝试”。尝试弹窗只包含用时，已取消，没有保存。
- 留存截图：`release/sequence-20261014/production-publish.jpg`、`production-db-verified.jpg`、`production-plan.jpg`、`production-today.jpg`。
- 本轮未做手机尺寸浏览器验证；接续尝试、完成、防重与删除重算的写入验收仅在隔离候选数据库执行。
- Git dirty 工作区保留，本轮未提交或推送。

## 后续维护

不要重跑 `prepare`、`candidate`、`resume-candidate.py` 或 `publish`：本次发布及导入已完成，有对应防重复标记。
检查线上部署可读取私有目录中的 `PUBLISHED`、`publish-before-counts.json`、`publish-after-counts.json` 和候选日志；不输出 `.env` 或账号口令。
生产回退需要单独评估 V17 数据兼容性。旧 JAR、前端与数据库备份保留在私有状态目录；不得直接删除 V17 或恢复旧备份覆盖已导入计划。

## 进度归属补充（2026-10-14 21:25）

用户明确要求将每日一题加入“进度 → 408 → 数据结构”。通过现有网页“整理课程／资料 → 带入已有任务 → 校验预览 → 确认保存”完成，未修改代码或再次发布。

- 课程 ID 为 1，稳定 key 为 `leetcode-daily-20261014-20261113-from31`，名称“LeetCode 每日一题（第31—61题）”。
- direction 为 `CS408`，branch 为“数据结构”，subjectId=2，unitId=2（题）。
- 整份练习以一个累计目录项表示，完整总量 31，关联原计划 taskId=31；没有拆分或新增任务。
- 后续学习记录通过既有任务归属自动累计，尝试不增加题量；修改与删除原记录后重算。
- 保存后刷新真实网页，课程仍存在，显示 `0 / 31 题` 和“已关联 1 项任务”。
- 可维护目录文件保存在被忽略的 `个人资料/学习计划/数据结构-LeetCode每日一题-进度目录.json`，其中 ID 仅适用于本次线上目录。
- 验收截图为 `release/sequence-20261014/production-progress-leetcode.jpg`。
