# LeetCode 自填题号发布（2026-10-18）

已完成：2026-10-18 10:38:31 CST正式发布V22，10:48:07最终保护校验通过。五次题号已按用户最终确认改为31、32、49、128、283；31张业务表保护校验、原11条记录及真实页面验收通过。

## 范围与产物

源码提交0c77355，develop；恢复时git status --short与git diff均为空。功能已通过本地75项相关MySQL集成测试、61项相关前端测试，后续提交前全量前端109项通过，前后端构建完成。

本机包release/leetcode-number-release-20261018.tar.gz，37120165字节，SHA256 04a3fd1439ffaa2a18a32c71b9f65f774bd70c319b22d7864907c3e9671e4f60。JAR为24ef15ca076bd7db729901d76488d1fae4a176f62e4b66bd51bf5c22c761d16c，index为e580c8d54aeeb10beb88b05318adfef02d0806011b4692d3f4244150844f24fe。

用户上传至服务器根目录/leetcode-number-release-20261018.tar.gz，已通过终端sha256sum核对与本机一致。解包目标/root/leetcode-number-release-20261018。

## 生产预检与保留范围

实例8.217.241.16，实际目录/srv/408doing/study-20260928-150827，compose.bundle.yaml；发布前V21，三个容器healthy。旧JAR为1262155427fd2fe63f951c2f384af70c9384d784d341e0f6474b024ba291f762，旧index为b180db642d437d4662634edbeaab08145e6a2e8cf516b7f77ea5392b3c1b0ad4。

原记录已增至11条，不能沿用17日晚7条的旧基线。LeetCode任务31有五次记录：ID1/4/7/10/11，日期2026-10-14至18日，原题号31/32/33/34/35。用户最终确认五次应为31/32/49/128/283；保留原记录ID、日期、完成量、内容与复盘，其他英语记录不变。

原计划目标2、导入1及7条月周纲要、进度课程1与节点1已更新旧固定清单文字；31个日名额、起止日期、总目标与原关联身份保持。

## 阶段目录与规则

状态目录/srv/408doing/private/leetcode-number-20261018，恢复库leetcode_number_verify_20261018，候选容器leetcode-number-verify-20261018。每个阶段只执行一次，恢复时先查标记与日志。候选复用既有MySQL的独立库、受限账号、256MiB内存、320MiB内存加交换上限和0.75CPU，不开放端口，结束后停止并锁定测试账号。

V22只添加显式模式；发布脚本按迁移前列逐表哈希校验原业务数据。题号更正经正常预览转换接口进行；其余记录、目标日期与目录身份有单独保护校验。不向正式库写验收学习记录。

## 控制恢复

浏览器文件选择器报No node found for given backend id；随后原生Windows控制因无法可靠识别浏览器URL而强制停止，未用旁路绕过。用户手动上传后通过浏览器文件面板确认完成、终端核对hash。一次浏览器自动审核超时，按工具允许方式重试后成功；通过现有页面的命令终端图标切回原终端。

prepare成功输出：MYSQL84_BACKUP_RESTORE_ALL_BUSINESS_ROWS_VERIFIED 31 11。备份gzip与SHA验证通过，完整路径和hash保存在状态目录RESTORED及preflight-backup.log，候选仍按独立库运行。

## 候选启动检查

首次candidate在启动测试容器和创建测试账号之前被保护条件中止。随后诊断输出RESTORED_DIFF=[]、PRODUCTION_DIFF=[]、MEM_AVAILABLE_KB=403216；确认两库与备份基线一致、内存恢复到原阈值之上后，保留candidate-preflight-failed.log，再按相同限制重试，未降低资源保护条件。

备份恢复标记：/srv/408doing/private/leetcode-number-20261018/preflight-backup/study-20261018-102129.sql.gz；SHA256 a51483c3b9ff720fd94796870878ff79ac396f16104a0de6b65b701d312521c6。RESTORED确认31张业务表、11条学习记录。

用户最终确认五次为31、32、49、128、283。补充脚本prepare_accepted.py校验原包release.py不变、两库仍V21且完整哈希未变后，保存原保护基线副本，并将授权题号范围扩展为五次。输出USER_CONFIRMED_31_32_49_128_283_SCOPE_ACCEPTED_ALL_BUSINESS_ROWS_UNCHANGED。后续使用release_accepted.py，原签名产物和清单保持不变。候选资源由320MiB调整为256MiB（JVM Xmx128MiB），启动仍要求至少320MiB可用内存，保留64MiB余量。允许新增128/283两个题目身份，原31个身份完整保留；不新增正式学习记录。


## 五次记录候选结果

release_accepted.py candidate退出0，MySQL8.4 V21→V22升级、五次题号31/32/49/128/283原地转换、任意3000题号、令牌重试、改为3001、删除后统计重算均通过。课程完成量仍5题，正式库完整哈希不变。候选已停止（exit143），正式三个服务healthy，可用内存411MiB。新增旁边日志终端连接失败，但原终端与正式网站均正常；并非正式服务断开。随后启动publish阶段。

## 正式发布与最终验收

`python3 release_accepted.py publish`退出0，2026-10-18 10:38:31 CST保存PUBLISHED。后端停止写入后完成最终备份、原子替换JAR并force-recreate，V22迁移成功；迁移前全部业务列哈希不变。前端保留旧hash资源，Nginx检查与重载通过，HTTPS入口与包一致，未登录API返回401，三个服务healthy。候选另已验证CSRF保护403及登录、注销。

最终备份：`/srv/408doing/private/leetcode-number-20261018/final-backup/study-20261018-103702.sql.gz`，SHA256 `cfa00a02452116b9851273fbc464e926fff1c0ff29a7065627ff97bde4e26f12`。恢复旧版必须同时评估V22及发布后数据，不能只换JAR或直接覆盖正式数据库。

正式网站任务库“自填题号”读取五条原记录及版本，确认保存31/32/49/128/283。目标2经正常编辑保存，标题为“养成每日一题习惯，记录实际题号”，日期保持2026-10-14至2026-11-13。目录JSON预览显示31→31题、新增0项，确认更新原课程1/节点1及任务31关联。`python3 release_accepted.py text`退出0，仅事务更新导入1及7条月周说明，其余保护行哈希不变。

`python3 release_accepted.py verify`退出0，2026-10-18 10:48:07 CST保存CONVERTED。核验：

| 原记录ID | 日期 | 最终题号 | 原完成量 |
| --- | --- | --- | --- |
| 1 | 2026-10-14 | 31 | 1题 |
| 4 | 2026-10-15 | 32 | 1题 |
| 7 | 2026-10-16 | 49 | 1题 |
| 10 | 2026-10-17 | 128 | 1题 |
| 11 | 2026-10-18 | 283 | 1题 |

原11条学习记录、日期、完成量、内容、复盘及历史用时保留；其他科目、目标日期、31条日安排和目录关联身份保留。只新增128/283两个按需题号身份，原31个身份逐行不变，无新增正式学习记录。候选容器Running=False，测试账号account_locked=Y。

真实浏览器刷新新版后，记录页逐条核对五题及原日期；进度仍5/31题，计划整体/月周旧固定题号文字已消除。下一日显示“填写题号／记尝试”，表单可填3000且无计时字段，取消后切回今天，没有提交验收记录。截图保存在忽略目录：`release/leetcode-number/production-records-20261018.png`、`production-progress-20261018.png`、`production-number-entry-20261018.png`。已完成线上实际验收，未重跑全后端及Linux Docker会话专测。本轮未提交、推送或合并Git。
