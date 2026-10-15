# 2026-10-15 简化高数计划替换

用户确认仅替换 onlystudy.loc.cc 的旧高数方案，保留原有学习记录、LeetCode、英语两份计划及进度目录。已完成，未部署或修改应用代码，数据库仍为V19，三个正式服务均healthy。

状态说明（2026-10-15 进展同步）：以上为本次操作当时的验收结果，未在本轮同步中重新连接生产核验。后续确认的 V20 两书接续需求见 [当前数学摘要](../business/zhangyu-math-plan.md)，其发布与完整方案导入仍 [暂停](../development/continued-study-deploy-handoff-20261015.md)。本记录只证明较早的时间型方案替换，不能据此认定 V20 已上线。

## 替换范围与结果

- 旧方案 `zhangyu-2027-math1-calculus-20261015-20270217`：478个任务、478条日安排，目标ID3，无任何关联学习记录。
- 新方案 `zhangyu-2027-math1-simple-20261015-20270209`：2026-10-15—2027-02-09，38个跨天共用任务、118天、236条日安排；5个月及18个周纲要。
- 每日仅内容与例题80分钟、对应章首次练习40分钟，练习可追加最多30分钟；全期基本14160分钟。全部为TIMED_STUDY，计划量NULL；不包含RECAP，不虚构实际题量或讲量。
- 旧任务／日安排、旧目标及旧方案导入纲要已移除，未保留两套重复排期。全站task从481变为41，daily_plan_item从755变为513，task_activity从479变为39，plan_period_outline从77变为76。整体目标及导入条目仍各4个。
- 原有2条学习记录（LeetCode、英语复习各1条）完整保留；所有非目标业务行按主键顺序逐表哈希核对一致，包括LeetCode序列、英语安排、进度课程／节点／归属。没有创建生产测试记录。

## 实际执行与验证

1. 本机解析JSON，核对连续118天、每天两项、80+40分钟、练习flexibleMinutes=30、全部数量NULL；文件SHA256为 `3ee089886f6f1f180984f7be0a9fa3b8b8b9b70f59a77e1fd7702d40d575f4d2`。
2. 正常网站Java预览通过，确认38任务／118天／236安排。用户确认替换范围后才开始线上清理。
3. 执行 `python3 /root/math-simple-20261015/replace-math.py prepare`：生产备份、gzip校验，恢复至现有MySQL8.4的独立库 `math_simple_check_20261015`，全部业务表哈希与备份前一致；隔离副本先演练清理，验证非目标内容未变。
4. 执行同脚本 `clear`：再次核对生产快照、旧方案目标／任务／日安排逐项匹配、目标无学习记录或其他引用，事务锁定工作区与目标数据，仅清理确认范围，提交前校验其他数据计数，增加workspace revision。
5. 在正常网页重新预览（旧token不可复用）并确认追加新版。没有通过SQL写入新版个人计划。
6. 执行同脚本 `verify`：逐日／逐任务核对新版原始JSON；按主键顺序比较所有非目标业务内容，检查精确增减量、迁移版本与服务健康。随后真实今日页面确认高数仅两项，原英语已完成状态及LeetCode当前题32保留。

验收输出：

```text
BACKUP_RESTORE_MYSQL84_AND_SCOPED_478_TASK_CLEANUP_VERIFIED
OLD_MATH_478_TASKS_AND_ITEMS_REMOVED_OTHER_CONTENT_PRESERVED
MATH_SIMPLE_38_TASKS_118_DAYS_236_ITEMS_80_40_FLEX30_VERIFIED
ALL_LEARNING_RECORDS_LEETCODE_ENGLISH_CATALOG_CONTENT_PRESERVED
NO_PRODUCTION_TEST_RECORDS V19 ALL_SERVICES_HEALTHY
```

## 证据与接续

私有状态 `/srv/408doing/private/math-simple-20261015` 保存备份、SHA256、原始目标范围、所有业务表哈希以及PREPARED／CLEARED／VERIFIED标记。备份SHA256：`7e9da5b8cc021c7d301ea276e3abb41e7a10cacf366b9bc8cc7faa4bf9aa6ef9`。隔离恢复副本保留用于审计，未启动额外数据库或应用容器。

本机脚本及截图在被忽略的 `release/math-simple-tools/`：`production-prepared.png`、`production-cleared.png`、`production-import-preview.png`、`production-plan.png`、`production-db-verified.png`、`production-today.png`。个人计划书已更新为已导入，旧文件仅作历史备查。

替换已完成，不重跑prepare／clear，不重复导入。恢复需要单独核对替换后新增数据，不得直接还原全库覆盖真实学习记录。原操作结束时仅追加本说明、未提交或推送 Git；本记录现纳入 2026-10-15 的项目进展同步，远程结果以 Git 实际状态为准。
