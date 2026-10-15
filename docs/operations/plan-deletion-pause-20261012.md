# 导入计划删除暂停交接（2026-10-12）

## 最新状态：2026-10-13 已核验完成

用户明确要求继续后，先检查当前代码、Git status/diff 和两份暂停交接。记录/进度重构与原部署改动均保留；本任务没有改动它们，也没有提交、推送或部署。

通过重启后新建的 Workbench 会话只读查询，确认上次事务已经提交成功，**没有再次执行删除脚本**。目前 task、stage_goal、daily_plan_item、weekly_plan_item、study_record、plan_import 均为 0，workspace revision=1，information_schema.innodb_trx 为 0。weekly_plan 只剩非导入空周：id=1、2026-09-21、note=NULL。

清理总量：30 个数学任务、1 个对应阶段目标、113 条每日安排、43 条周条目、14 周导入计划。原来没有 JSON 导入批次，也没有学习记录或手动每日安排。

执行本地保存的只读核验脚本 `release/verify-plan-clear-20261013.py`（经正常 Workbench 终端调用 Python），将当前 mysqldump 的 INSERT 数据按原始字节与完整备份比较：account、subject、study_unit、study_settings、study_record、review_item、review_record、memo、target_school、target_link、plan_adjustment、plan_import、plan_period_outline、plan_quick_submission、flyway_schema_history 共 15 张非目标表完全一致。输出 `PRESERVATION_VERIFIED 15 non-target tables`，备份 SHA256 仍为下文原值。初次按 UTF-8 读取完整 dump 遇到会话二进制字段，改用原始字节后通过；未修改备份和数据库。

用户在网站登录原账号后，真实浏览器核验：计划总览显示“从第一天开始，建立你的计划”，任务库显示“还没有任务”；今日各科均“今天暂无任务”；进度显示“暂无进度数据”。已在线刷新对应页面。结果截图 `release/plan-delete-after-20261013.png`。

这里只验证本机浏览器在线读取后的显示，不声称其他设备离线快照已刷新。没有临时服务需要关闭。下方为历史暂停记录和恢复步骤，不再是待办；**不得再次执行一次性删除脚本**。

---

用户要求删除线上 onlystudy.loc.cc 的全部既有导入计划，保留学习记录、手动安排、账号和设置。用户已经回复“确认删除”，无需重复请求同一范围的授权。随后用户要求先暂停，准备重启 Codex。恢复前等待用户明确继续。

## 已验证的删除范围与备份

- 阿里云香港实例 i-j6c1ulq7dru9ovj3df56，root@8.217.241.16；通过 Chrome 的阿里云 Workbench，现有免密连接登录成功。直接 SSH 的现有本机密钥仍认证失败。
- 线上三个容器 study-workbench-{mysql,backend,frontend}-1 均 healthy。
- plan_import 为 0，没有 JSON 导入批次。既有计划全部属于旧 V12 数学导入：stage_goal id=1，标题“2027考研数学一轮：张宇基础30讲＋1000题”；task id=1..30，全部关联目标 1；每日安排 113 条，全部 WEEKLY；周条目 43 条；导入周计划 id=2..15，日期 2026-09-28 至 2026-12-28。非导入空周计划 id=1、2026-09-21、note=NULL 必须保留。
- study_record、review_item、memo、plan_adjustment 均为 0；workspace revision=0。以上为执行尝试前真实查询，不是暂停后状态。
- 完整备份已生成，gzip -t 成功并输出 SHA256：
  `/srv/408doing/private/pre-plan-delete-20261012/study-20261012-230823.sql.gz`
  SHA256 `e955d740e4a4eee929b485f91c1a616ff02db414603bdc57490d4496fd6d3f8e`。
- 没有做该新备份的隔离恢复验证。

## 当前未确认状态：不可宣称删除成功

- 已保存本地事务脚本 `release/clear-imported-plans-20261012.sql`，包含前置数量与版本检查、workspace/目标/任务/周计划行锁、删除任务及级联安排、删除目标和 14 周计划、revision+1、提交前检查。mysql 不得加 --force；SQL 错误会退出连接并回滚未提交事务。
- 通过 Workbench 粘贴 bash heredoc 包装脚本（先验证备份 hash/gzip，再执行该 SQL）。长段输入的终端展示出现底部文本重叠、结束标记显示异常。没有收到备份 OK、SQL 成功标记 `IMPORTED_MATH_PLAN_CLEARED` 或错误输出。
- 尝试 Ctrl+C 和 Enter 后，终端 DOM 仍保持相同内容，不能确认按键是否送达，也不能确认删除已提交或回滚。**恢复时必须先通过新的只读会话核对数据库；不得直接再次执行删除，亦不得在旧终端盲按 Enter。**
- 用户暂停后只尝试重载网站作只读检查，工具未返回可用 DOM，因此没有取得暂停后的网页验证结果。
- 删除前截图 `release/plan-delete-before-20261012.png`，显示旧数学路线。
- 没有启动新临时服务、停止线上容器、修改应用代码、提交、推送或部署。原未提交部署改动保持原状。

## 重启后的最短恢复步骤

1. 用户明确继续后，先完整读取 PLAN.md、AGENTS.md、docs/STRUCTURE.md；核对 Git 状态，保留原有工作区。
2. 重新初始化 browser/CUA 绑定（此前 Chrome id=3，重启后重新发现），读取当前 API 文档。通过 browser.user.openTabs() 找到网站和 Workbench，避免复用旧标签 ID/绑定。浏览器连接在本轮恢复后已实际成功读取 DOM；早前失败不代表当前故障。
3. 创建/使用新的 Workbench 终端会话，**先查询 task、stage_goal、daily_plan_item、weekly_plan_item、weekly_plan、study_record、plan_import 和 workspace revision**，判断是否已经提交。若 0/0/0/0/1/0/0 且 revision=1，删除已经成功，只做网页和保留项核验。若仍 30/1/113/43/15/0/0 且 revision=0，确认无悬挂事务/旧命令，再执行已授权脚本。其他结果先查明，勿放宽脚本保护。
4. 避免再次提交未经核验的巨大多行终端输入。可通过终端粘贴单行 Python/base64 将脚本写入服务器私有目录，核对 SHA256 后用短命令执行；仅正常 UI 操作，不使用页面隐藏终端对象或私有 API。
5. 核对 SQL 成功/失败输出及实际数据，刷新网站计划、任务库、阶段目标、今日/本周/进度快照，确认旧计划清空、学习记录及设置未受影响；保存并展示结果截图。未验证前不得宣布完成。

本文件是用户请求暂停所需的项目交接，不是对 Codex 个人记忆的更新。
