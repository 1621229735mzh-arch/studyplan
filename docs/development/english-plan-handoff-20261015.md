# 英语新词与每日复习完成确认（2026-10-15）

## 当前状态与用户授权

用户要求补齐“无数量、无时长，只确认完成”的功能，并将两份 2026-10-15—2027-02-14（123天）英语计划导入线上。必要部署和导入在本次授权范围；未授权Git提交或推送。保留develop原有dirty内容、原今日／计划布局、高数与LeetCode及真实学习记录。

当前功能、生产发布及两份英语计划导入均已完成。2026-10-15 11:08:11 CST 发布 V19，11:12:51 CST 完成只读数据库验收；真实网页已更新并核对今日英语入口。不要重跑 prepare、candidate、publish 或重复导入。详细结果见 `docs/operations/english-plan-release-20261015.md`。

## 代码与规则

- V19扩展task_activity约束，新增CHECK_ONLY，flexible_minutes必须为0；不改V1–V18。
- 导入v4允许CHECK_ONLY任务和日安排数量、预计用时为空，不允许虚构数字；v1–v3保持兼容。
- POST /api/learning/records/day-confirmation 接收taskId/date/completed/revision/clientToken。确认记录amount与duration_minutes均null；取消软删除。任务行锁确保同日单份有效确认，重复目标状态不重复写，历史revision防止完成／取消后的旧设备重放。
- 今日确认／取消无需弹窗，离线只读；完成记录可从记录页删除，不能编辑成数量／用时。故意不计时不作为missingDuration，不混入学习量或新词月目标。
- 每日复习是本人／外部软件维护已学范围的日清单，不预建复习模块的具体内容，不触发掌握评分或自动调期。
- 任务、日安排、工作区和记录返回activityMode；月份confirmationCount独立展示，避免同科“词”的新词计划总量被空量吞掉。

## 验证证据

日志在release/english-plan-tools/：

- backend-verified.log：真实MySQL8.0.45共63项（确认5、接续8、时间型5、计划20、目录13、进度5、今日7），package成功。
- confirmation-final.log：修正测试配置前缀后，确认5项再次通过。初次失败由沙箱本机端口权限和新测试缺少预设测试账号引起；没有产品测试失败遗留。
- frontend-verified.log：84项／10文件通过；frontend-build.log类型检查与构建成功。
- full-import-local.log：两份完整计划正常登录、CSRF、预览、确认导入，复习确认／取消／重试／旧版本冲突／删除重开，新词30后次日50，旧课程保留均通过。
- 真实浏览器本机15191验收：确认无需数量或时长、已完成可展开取消，截图local-confirmed.png。没有执行手机尺寸验收。
- git diff --check通过；无Git提交／推送。

## 发布包与个人文件

发布包release/english-plan-20261015.tar.gz，37,066,977字节。

- 包SHA256：22efce894eb7dc955fea8c5f962d4d86e78291df10f3feede64dff5aed8c6984
- JAR SHA256：cf4cc5e29ac6b755f25b402bf8c3a44d16b6509cf4917a0ffe53eb4d35763da7
- 包含artifacts、nginx、new-words.json、review.json、english_smoke.py、english-release.py、VALIDATION、SHA256SUMS，不含生产凭据或备份。

原新词JSON保持不变，v1，1任务123安排，每日50词，总目标6150，题库位置自行接续、不积压。新增同目录每日单词复习-完成确认-20261015-20270214.json，v4，1任务123安排，数量和预计用时均null，5个月、18个周纲要。不要用1次或1词代替复习确认。

## 生产基线与后续

2026-10-15约10:50实时读取：V18:1，task479、daily_plan_item509、有效study_record1、progress_course1；三容器healthy。这条真实学习记录必须保留，不使用昨天“记录为0”的旧基线。

服务器8.217.241.16，DEPLOY=/srv/408doing/study-20260928-150827。本次私有ROOT=/srv/408doing/private/english-plan-20261015，候选schema=english_upgrade_20261015、容器english-upgrade-20261015；首次创建前确认不存在。

用户连接的Chrome Workbench标签当前1458832491，browser3；可能漂移，恢复时先查实况。终端用paste(null,command)+pressKey(null,Return)，结果需截图读取。已请用户把包上传/root；不要重试已知反复阻塞的大文件setFiles接口。

上述发布流程已依次完成：包 hash 一致，prepare 备份与 MySQL 8.4 恢复通过，candidate 全量导入和确认／取消验收通过，publish 输出 `PUBLISH_DONE_V19_BUSINESS_COUNTS_PRESERVED`。三个正式容器 healthy，候选 exited，测试账号锁定。私有备份和回退产物保留。

真实网站已应用 PWA 更新，在设置中新增并启用“英语一”“词”，未更改预算。两份原始 JSON 均经 1 任务／123 天／123 安排预览确认，通过正常页面追加；没有直接 SQL 写入个人计划。新任务 ID 为 510、511，task 从479增至481，daily_plan_item从509增至755；study_record仍1、progress_course仍1。只读脚本逐项核对新词每日50／总6150、复习 CHECK_ONLY 且数量／时间 NULL，旧高数478、LeetCode31天、课程目录及原记录内容与恢复副本一致。生产未创建验收学习记录。证据在 `release/english-plan-tools/production-*.png`；两份个人计划书已同步导入状态。

## 本机隔离进程

MySQL33312，父PID15016；datadir=release/retrospective-20261012/mysql-data。后端18091，PID43512；Vite15191，PID10812。库english_check_test_20261015及english_preview_20261015均为本机验收。关闭前实时核验完整命令，不能复用旧PID停止其他任务。不要删除数据目录或其他任务进程。
