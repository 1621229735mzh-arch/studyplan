# 2026-10-15 两书学习接续发布与高数替换

## 当前状态

V20 已正式发布至 `https://onlystudy.loc.cc/`，应用发布输出 `PUBLISH_DONE_V20_BUSINESS_COUNTS_PRESERVED`。原始业务行使用迁移前列清单逐表哈希核对一致，原4条学习记录未改变；没有在生产写入测试学习记录。网站原登录会话保持，PWA 已执行“立即更新”。

完整新方案已通过正式网站 Java 预览：126天、252个内容任务、252条日安排，2026-10-15—2027-02-17，两书各126段、每天新内容合计120分钟，5个月及19个自然周纲要。参考结束日期允许顺延，未核实完整题号，练习按章节和知识主题定位。完整文件在忽略目录 `个人资料/学习计划/张宇2027高数-数学一-两书接续版-20261015-20270217.md` 及同名JSON。

2026-10-16用户明确回复“确认替换”。重新接入Workbench及网站后，只读核对V20、旧数学精确范围及全站业务快照：与替换备份基线一致，旧数学仍无学习记录。随后完成旧38任务、236日安排、1目标、23纲要的事务清理，再通过正式网站重新预览、勾选核对并确认导入新版。网站显示“方案已追加到计划”；计划页已展示新目标与2027-02-17参考结束日期。

数据库验收通过：252内容任务的元数据、顺序、日期、空数量及预计分钟均与JSON一致，2条书籍序列、24条月周纲要正确。排除新数学范围后的全站业务行哈希与替换前保护基线一致，原4条学习记录、英语、LeetCode及课程目录全部保留，三个生产服务healthy。输出 `PRODUCTION_V20_FULL_126_DAYS_252_CONTENTS_2_BOOKS_VERIFIED`、`ALL_ORIGINAL_LEARNING_RECORDS_ENGLISH_LEETCODE_CATALOG_PRESERVED_NO_TEST_RECORDS`。

最终只读补查：旧 `zhangyu-2027-math1-simple-20261015-20270209` 导入为0条，新 `zhangyu-2027-math1-continued-20261015-20270217` 为1条；全站task=255、daily_plan_item=529、study_record=4、plan_import=4、study_continuation_stream=2、task_continuation=252。与替换前41任务／513安排的精确差值吻合。`VERIFIED` 时间为2026-10-16 08:18:34 CST，终端证据 `release/continued-math-20261015/production-verified.png`。

真实今日页2026-10-16显示两本书各自第一段“函数的概念、定义域与基本性质”，预计90＋30分钟；未做的10月15日内容保留，没有积累第二段。未点击学习完成或生成生产测试记录。截图保存在忽略目录 `release/continued-math-20261015/production-plan-replaced.png`、`production-today.png`。

## 包与验收

- 本地发布包：`release/continued-math-20261015.tar.gz`，37,106,123字节，上传至服务器 `/root/continued-math-20261015.tar.gz` 后外部及内部SHA256核对通过。
- 原始包SHA256：`187461058e5f2269f522f7b744bb2e7a72158ae8807e56b06f58233c61b4ffea`。
- 发布JAR SHA256：`cdc558e15f5dce2057ac25e28c39aa5287627e593be9769ba338e999d9336a07`。
- 完整方案JSON SHA256：`dd987fe2563e44eeb9a5dfecfa7ae286180f00d842870d6a62abe32e20ed2881`。
- 本机后端136项及打包、前端87项及类型检查和构建通过；另修复接续测试缺少初始账号配置，在空库且无初始账号环境变量时8项通过。未修改生产账号初始化逻辑。
- 使用现有生产MySQL8.4的独立库恢复备份，逐表哈希一致；未另起数据库。候选应用受限384MB内存、1CPU、128进程，数据库连接池2，不公开端口。
- 首次候选已通过迁移、保留校验、旧方案清理及完整导入，在验收脚本读取被JSON省略的空amount字段时失败。修正为可空读取，保留首次日志，再从原备份建立 `continued_retry_20261015` 新副本，使用 `continued-retry-20261015` 容器完成全部验收，未改动应用代码或生产数据。
- 通过输出：`OLD_38_TASK_SCOPE_CLEANUP_REHEARSAL_VERIFIED`、`FULL_126_DAY_252_CONTENT_IMPORT_TWO_STREAMS_RETRY_DELETE_AND_CONTINUATION_VERIFIED`、`AUTH_CSRF_SESSION_VERIFIED`、`MYSQL84_V19_TO_V20_CANDIDATE_VERIFIED`。候选执行完毕由finally停止并锁定验收数据库账号。

## 生产恢复点与接续

实际部署目录仍为 `/srv/408doing/study-20260928-150827`、`compose.bundle.yaml`。发布脚本 `/root/continued-math-20261015/continued-release.py`，私有状态 `/srv/408doing/private/continued-math-20261015` 保存旧产物、发布前后哈希、生产备份与标记。

已完成 `prepare`、候选验收（重试使用单独副本）、`publish`、`replacement-prepare`、`clear`、网站重新预览确认导入及 `verify`，不得重跑发布／清理或重复导入。私有状态已包含 `CLEARED` 和 `VERIFIED`；恢复时先只读核对这些标记及实时数据。原简化方案备份继续保留，不还原旧全库覆盖新数据。

回退需评估V20百分比记录及接续元数据兼容性，不能只换旧JAR沿用新数据，也不能直接还原旧全库覆盖新记录。原始包中的验收脚本空字段问题已在服务器和本机修复，并更新服务器SHA256SUMS；应用JAR、前端及方案文件未因该脚本修复改变。

浏览器控制恢复后，Workbench终端可读写。上传曾报 `Session has expired, please login again`，刷新Workbench并通过原有免密方式重新登录同一实例后，目录和上传恢复；未修改服务器登录设置或权限。

Git仍在develop；本轮测试修复及发布文档纳入本次提交，不合并main。
