# 2026-10-15 每日完成确认发布与英语计划导入

## 完成范围

服务器 `8.217.241.16`，部署目录 `/srv/408doing/study-20260928-150827`，网址 `https://onlystudy.loc.cc`。2026-10-15 11:08:11 CST 已正式发布 V19，11:12:51 CST 完成导入后的只读数据库验收，随后在真实网页核对今日英语任务。用户授权必要实现、发布与导入；本轮未提交或推送 Git，保留 develop 原有 dirty 工作区。

新增 CHECK_ONLY 完成确认型安排：数量、预计与实际用时均可为空，今日直接勾选／取消，沿用学习记录、事务、任务行锁、防重与历史 revision 校验，不另存完成量事实。复习不增加新词学习量，不作为缺少用时，不与同科目新词目标混加。今日与计划沿用原布局。

## 验证与发布

本机证据位于 `release/english-plan-tools/`：真实 MySQL 8.0.45 的63项相关后端测试与 package 通过，修正测试账号配置后确认测试5项再次通过；前端84项／10文件通过，类型检查与生产构建通过。完整两份123天导入以及确认、取消、重试、旧版本冲突、删除重开、新词实际30后次日仍50、身份与CSRF在本机隔离环境通过，浏览器确认／取消也已验收。没有执行手机尺寸浏览器验收。

发布包 `release/english-plan-20261015.tar.gz`，37,066,977字节：

- 包 SHA256：`22efce894eb7dc955fea8c5f962d4d86e78291df10f3feede64dff5aed8c6984`。
- JAR SHA256：`cf4cc5e29ac6b755f25b402bf8c3a44d16b6509cf4917a0ffe53eb4d35763da7`。

用户上传后实时核对 hash，再依次执行 `python3 english-release.py prepare`、`python3 english-release.py candidate`、`python3 english-release.py publish`。prepare 保存旧产物、生产备份并在 MySQL 8.4 隔离库恢复、比较旧表计数；candidate 在恢复副本迁移 V18→V19，通过正常接口全量导入及确认／取消验收。候选输出：

```text
ENGLISH_123_DAY_BOTH_IMPORTS_CONFIRM_CANCEL_RETRY_STALE_NO_NEW_WORDS_VERIFIED
AUTH_CSRF_SESSION_VERIFIED
MYSQL84_V18_TO_V19_CANDIDATE_VERIFIED
```

publish 停止后端后保存最终备份、替换产物并迁移，原业务表计数保持，输出 `PUBLISH_DONE_V19_BUSINESS_COUNTS_PRESERVED`。随后实时确认 MySQL／backend／frontend 均 healthy、候选 exited、测试数据库账号 account_locked=Y。

## 正常网页导入与保留验证

用户已登录，网站应用 PWA“立即更新”。在设置新增并启用“英语一”“词”，未更改预算和复习参数。两份原始 JSON 分别通过网页粘贴、Java 校验、可读预览和确认追加，无 SQL 写入个人计划。

| 方案 | 日期 | 任务／安排 | 规则 |
| --- | --- | --- | --- |
| `english-new-words-50-20261015-20270214` | 2026-10-15—2027-02-14 | 1／123 | 每天50词，总目标6150，预计用时NULL |
| `english-word-review-check-20261015-20270214` | 2026-10-15—2027-02-14 | 1／123 | CHECK_ONLY，数量与用时NULL，flexible_minutes=0 |

两份计划各含5个月、18个周纲要。新任务ID为510、511。新增2任务、246日安排、2整体目标、2导入条目、46纲要和1个task_activity。task从479增至481，daily_plan_item从509增至755，plan_import从2增至4，plan_period_outline从31增至77；study_record仍1，progress_course仍1。

在服务器执行 `python3 verify-production.py`，只读核对每个日期、每日数量、目录纲要及所有旧业务表计数；逐项比对原高数478个时间型任务、LeetCode31天及序列、进度课程／节点／归属、原始学习记录内容与恢复副本一致。没有在生产创建验收学习记录。输出：

```text
ENGLISH_TWO_PLANS_123_DAYS_246_ITEMS_NEW6150_CHECK_ONLY_VERIFIED
OLD_MATH_LEETCODE_COURSE_AND_ORIGINAL_STUDY_RECORD_CONTENT_PRESERVED
NO_PRODUCTION_TEST_RECORDS HEALTHY_MYSQL_BACKEND_FRONTEND V19
```

真实今日页面显示“背诵50个新词：从当前进度继续”和未勾选的“每日单词复习”，复习明确标示无需数量或时长、不增加新学量；原 LeetCode 当前题仍为32。完整词单及词书总量未提供，因此没有自动建立英语课程目录；6150只是本期计划目标。

## 证据与恢复

私有状态目录 `/srv/408doing/private/english-plan-20261015` 保存 PUBLISHED、发布前后计数、旧产物、候选日志、备份及 `production-import-verified.json`。包及脚本目录 `/root/english-plan-20261015`。

- 预检备份 SHA256：`825ce5c0b6cbc469d1d39155070a2d0de4b5c1d4b473947513f451cf29e9682c`，已在MySQL8.4恢复验证。
- 最终备份 SHA256：`4f69f5aacd75ae7eb8aef430bde979ee80e587b0c5141db9c2dbb3861341deb5`，压缩及hash验证通过。
- 本机截图：`production-publish.png`、`production-new-preview.png`、`production-review-preview.png`、`production-plan.png`、`production-db-verified.png`、`production-today.png`、`production-today-english.png`。

发布和导入已完成，不重跑 prepare、candidate、publish 或重复导入。回退须单独评估V19确认型数据兼容性，不得直接还原旧库覆盖新增计划或真实记录。未修改V1–V18已应用迁移。两份个人计划书已更新为已导入状态。
