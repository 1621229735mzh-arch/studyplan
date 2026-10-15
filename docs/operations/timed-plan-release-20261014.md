# 2026-10-14 时间型安排发布与高数计划导入

## 已完成范围

2026-10-14 22:48（Asia/Shanghai）已在 `https://onlystudy.loc.cc` 完成 V17 → V18 更新及高数方案导入。部署目录 `/srv/408doing/study-20260928-150827`，服务器 `8.217.241.16`。

必要功能为按基本分钟安排、未知计划量留空、实际完成量据实记录、结束当天安排及复盘只计用时。今日和计划沿用原布局与操作路径；未自动调整后续日期。用户授权功能、发布与导入，本轮未提交或推送 Git，保留 develop 原有 dirty 工作区。

## 产物与验证

- 发布包 `release/timed-plan-20261014.tar.gz`，37,075,624 字节；用户上传至服务器 `/root`，实时核对 SHA256 一致。
- 包 SHA256：`7a80f861173d48f26210b69610deb709a3ce61a9c58fb348f19f7af8be79c96d`。
- JAR SHA256：`2806cd2421bf787df866a3a80d429e93cd73a5effb2d552b6306aa8ab80e3f99`。
- 本机真实 MySQL 8.0.45：77 项相关测试和 package 成功；前端 81 项测试通过，最终相关 21 项重跑、类型检查与生产构建通过。命令及日志详见开发交接文件和 `release/timed-plan-tools/`，本轮发布接续没有重复整套本机测试。
- 本机完整 126 天导入及浏览器已验证实际量、用时、位置备注和只改分钟的编辑。本次未做手机尺寸浏览器验收。

服务器依次实际执行 `python3 timed-release.py prepare`、`python3 timed-release.py candidate`、`python3 resume-candidate.py`、`python3 timed-release.py publish`，最后执行只读 `python3 verify-production.py`。

## 候选验收中断与恢复

`prepare` 校验生产 V17、健康状态和包内哈希，保存旧 JAR、前端、Nginx，备份并在 MySQL 8.4 隔离库恢复成功，旧业务表计数相同。

首次候选已迁移 V18，并通过正常接口导入全部计划，尚未创建测试学习记录。验收脚本读取空记录回看的 `stats.studyMinutes` 时遇到 KeyError：全局 NON_NULL 会省略此空字段。这是验收脚本问题，未改生产代码或跳过发布门槛。

单独的 `resume-candidate.py` 先核对生产仍 V17、候选 V18、已导入 478 项、业务计数增量及旧课程／序列，再恢复同一隔离容器。空用时以 `.get('studyMinutes')` 读取，已完成的导入通过重复预览返回 409 验证，不再重复创建。原包、SHA256SUMS 和发布脚本均保持原内容。

恢复验收确认：首次 4 题／35 分钟，复盘拒绝增加题量且只记 10 分钟，重试返回同一记录，删除结束记录后重新开放，累计用时 45 分钟，旧进度课程未变，身份与 CSRF 有效。成功输出：

```text
TIMED_126_DAY_IMPORT_QUANTITY_RECAP_RETRY_DELETE_VERIFIED
AUTH_CSRF_SESSION_VERIFIED
MYSQL84_V17_TO_V18_CANDIDATE_VERIFIED
```

候选结束后已停止，数据库测试账号重新锁定；未公开候选端口。

## 正式发布与导入结果

发布前检查 Nginx，停止后端后保存最终备份；原子更新 JAR，迁移至 V18，核对原业务计数，更新前端与 Nginx。输出 `PUBLISH_DONE_V18_BUSINESS_COUNTS_PRESERVED`。随后实时确认 MySQL／后端／前端均 healthy，候选 exited、测试账号 account_locked=Y。

真实网站原账号仍登录；应用“立即更新”后，在计划页粘贴原始 JSON，预览显示 `478 个任务 · 126 天 · 478 条安排`。核对数量为空、复盘只计用时后，通过原网页勾选确认并事务追加。没有通过 SQL 或迁移写入个人计划，也没有创建生产测试记录。

- 方案 ID：`zhangyu-2027-math1-calculus-20261015-20270217`；日期 2026-10-15—2027-02-17，连续 126 天。
- 新增 478 个任务、478 条日安排、1 个整体目标、1 个导入条目、5 个月及 19 个按周一组织的周纲要；原计划的 18 个学习周并未增加日期。
- 每天基本预计用时 120 分钟，弹性最多 30 分钟；基本总量 15120 分钟。任务及日安排计划数量均 NULL，未编造题量、题号或页码。
- RECAP 260 项、TIMED_STUDY 218 项。10月15日真实页面显示回顾10分钟、理解与例题65分钟、首次练习35分钟及订正10分钟，弹性30分钟只出现一次。
- 原 LeetCode 任务、31 个不同日期（2026-10-14—2026-11-13）、31题清单及进度课程保持。只读验收还逐表比对其任务、日安排、序列、目录节点及任务归属内容，与恢复副本一致。
- 有效及全部生产学习记录均为0；导入没有增加学习完成量。其余原业务表计数保持发布后基线。

只读验收输出：

```text
PRODUCTION_TIMED_126_DAYS_478_ITEMS_120_BASIC_30_FLEXIBLE_VERIFIED
LEETCODE_31_DAYS_SEQUENCE_AND_PROGRESS_CONTENT_PRESERVED
NO_PRODUCTION_TEST_RECORDS
VERSION 18:1 HEALTHY mysql/backend/frontend
```

## 证据与恢复

服务器私有状态：`/srv/408doing/private/timed-plan-20261014`，包含 `PUBLISHED`、两次备份、旧产物、候选日志和 `production-import-verified.json`。PUBLISHED 实际时间为 `2026-10-14 22:42:52 CST`；完整导入及只读验收在此之后完成。不要输出 candidate.env 或生产凭据。

- 预检备份：`preflight-backup/study-20261014-223603.sql.gz`，SHA256 `2267350c3da093c7a852fbd95617c489bcf29a05ac44163372a908a46e5e6216`，已恢复核对。
- 最终备份：`final-backup/study-20261014-224206.sql.gz`，SHA256 `2fa755ca3933bdc0ba16ffe37f52b6c06bdc27e06246fafb6328cf55327fdc78`，压缩及哈希检查通过。

本机证据在 `release/timed-plan-tools/`：`production-candidate-verified.png`、`production-publish-verified.png`、`production-import-preview.png`、`production-plan.png`、`production-db-verified.png`、`production-today.png`；恢复及只读脚本也保存在此目录。该目录和个人计划文件均受现有忽略规则保护。

发布与导入已完成，不要重跑 prepare、candidate、resume-candidate 或 publish。回退需单独评估 V18 时间型数据兼容性，不得直接还原旧数据库覆盖新计划或删除已应用迁移。后续内容接续依赖记录的实书位置、手动调整日期；未实现自动章内接续或自动延期。
