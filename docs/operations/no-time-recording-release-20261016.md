# 不计时记录发布（2026-10-16）

## 当前状态

用户明确授权部署，已于 **2026-10-16 16:18:21 CST** 完成正式切换。候选、正式服务和数据保留检查通过；16:20 在正式网站点击 PWA“立即更新”，16:23 真实今日页已显示不计时新版。接续前先读状态标记与实时 jar，不得重复 prepare/publish。

## 范围与构建

移除所有手动填写实际用时、预计用时与时间预算的入口；保留日期、进度、掌握反馈、历史用时及已有估算。无新迁移，Flyway 应保持 V20。不重跑旧方案清理、导入或历史发布。

后端 144 项及 verify 打包、前端 94 项及类型检查／构建通过；2 项 Linux Docker 会话专测未运行。详见 [本地验证](../testing/no-time-recording-20261016.md)。Git 分支 develop，工作区保留未提交改动，本轮不提交、推送或合并 main。

## 产物

- 本地发布包：release/no-time-release-20261016.tar.gz，37,082,469 字节。
- 发布包 SHA256：a06ee713cc7e61663accc49ed58bf0699498d6069a24395988a3f11bad67b111。
- 新 jar SHA256：70e8719b4bc0edcac910540f140c36cf2a485f423d19f5f1582e356f986403a6。
- 新 index.html SHA256：bcee042db72fc736414d0fa6c51ad5d557b8684a980845ac4615e9dd4c724dff。
- 线上预检旧 jar SHA256：bf9a0f029d3f794f847c0d320a2360ce76e94d08cdc0d1e42d13b9d3712c4487，与上一版今日项数发布一致。

## 线上预检与恢复点

2026-10-16 15:57:55 CST 核对实际部署目录 /srv/408doing/study-20260928-150827，沿用 compose.bundle.yaml。三个正式服务 healthy，服务器可用内存约 504 MB；不新建数据库容器。Workbench 免密重新连接后完成文件上传，上传路径 /root/no-time-release-20261016.tar.gz；外部 SHA256 和内部 SHA256SUMS 校验通过。

本轮私有状态：/srv/408doing/private/no-time-recording-20261016/。旧 jar、旧前端、预检及最终数据库备份、业务表行哈希及 RESTORED / CANDIDATE-VERIFIED / PUBLISHED 标记保存在该处。

候选计划使用现有 MySQL 8.4 的独立恢复库 no_time_verify_20261016；容器 no-time-verify-20261016 限制 320 MB 内存、0.75 CPU、128 进程、2 个数据库连接，不公开端口。只在恢复库写候选验收记录；生产数据切换前后逐表比较，不写入生产演示记录。

## 发布前验收进展

预检备份 GZip 与 SHA256 校验通过，SHA256 为 9e5640039fe52f884be30bddc49b109145fc8b77001aa5e3a05a05e9000694d7；在现有 MySQL 8.4 的新隔离库中恢复，31 张业务表数量及行哈希一致，输出 BACKUP_RESTORE_MYSQL84_ALL_BUSINESS_TABLES_VERIFIED 31。

首轮候选的无用时写请求已返回 201，但验收脚本直接访问被 JSON 序列化省略的空 durationMinutes 字段而失败。修正为 get() 后采用独立恢复库 no_time_verify_20261016b 和受限候选 no-time-verify-20261016b 重验；旧候选已停止、账号已锁定，生产未切换。修正版 release-retry.py 的 SHA256 为 81ea1816b9ff8db6c902e4102776c32a58e93c679f1c189fb9fbba54b3fe985b，原始程序包及产物保持原哈希。中途恢复命令因宿主 shell 展开数据库环境变量失败，空隔离库未恢复；随后复用已审阅脚本的容器内连接方式重试，不输出秘密，不修改生产库。

修正版候选验收输出 MYSQL84_NO_TIME_SAVE_RETRY_DELETE_REVIEW_AUTH_PRESERVATION_VERIFIED。今日项数为 0／0／5，昨日为 3／0／2；learningSave、retry、staleRejected、deleteRecalculated、unplannedContent、manualReview、partialAndCompleteReview 均为 true。匿名读取 401、缺 CSRF 写入 403、登录／会话／退出通过；候选写入仅限恢复库。原有业务行逐项保留，生产业务表哈希仍与预检一致。候选已在 finally 中停止并锁定其数据库账号。

正式发布命令已启动：python3 /root/no-time-release-20261016/release-retry.py publish。按受保护流程停止后端写入、最终备份、原子替换 jar、强制重建后端、等待健康、比较业务行，再追加前端资源并切换 index / Service Worker，检查重载 Nginx。正式完成标记与网页验收均已完成，见下文。

## 正式结果与数据保留

发布输出 PUBLISHED_NO_TIME_RECORDING_ALL_BUSINESS_ROWS_PRESERVED，PUBLISHED 时间为 2026-10-16 16:18:21 CST。新 jar 与 index.html 哈希与上述包内产物一致；三个服务 healthy、Flyway 保持 V20、匿名 /api/today 返回 401，HTTPS 返回的新入口与发布包完全一致。31 张业务表在停止写入后的最终基线与切换后数量／行哈希完全一致。

最终备份位于私有状态目录的 final-backup/ 下，SHA256 为 2c3a1101ebe64f81872ff5c485962bf5257e034c88da4afe2b212c84b50fa53f。预检备份与最终备份均校验 GZip 和哈希，旧前后端配对产物保留；当前无新增迁移，不允许恢复旧全库覆盖发布后的新学习数据。

两个候选均在 finally 中停止并锁定各自数据库账号。没有新增数据库容器、没有公开候选端口，没有写入生产测试学习记录。临时隔离库及日志作为验收证据保留。

## 正式浏览器验收

使用原 Chrome 登录会话，刷新后点击“立即更新”激活 PWA 新版本。

- 今日顶部仍为已完成 0 项／进行中 0 项／还剩 5 项；数学、英语、政治、408 的原清单保留，分区时间和缺用时提醒消失。
- 打开数学完成弹窗，仅显示任务与取消／保存；打开剩余弹窗，只显示累计已完成百分比，不含用时。只查看并取消，没有提交生产学习记录。
- 记录页原归档为 2 天、4 次学习；补记表单保留日期、内容、科目、单位、完成量与复盘，无用时输入。
- 设置页保留考试日期与复习日期间隔，无每日学习预算或复习时间额度。
- 复习清单和建议确认页无需用时、额度或缺时间提醒。计划“添加日安排”表单没有预计用时输入，原四份导入方案继续显示。
- 最终回到正式今日页，状态为在线；登录会话保持。没有在正式网站写测试记录或保存设置／计划。

截图保存在被忽略目录 release/no-time-production-evidence/：today.jpg、complete-dialog.jpg、progress-dialog.jpg、record-form.jpg、settings.jpg。正式网页截图已直接检查，未见布局异常。最后读取管理面板时 Workbench 会话断开；断开发生于发布成功且网页验收完成之后，不影响已核实的发布结果，未因此重启服务器。

发布阶段未提交 Git；本轮实现、此前任务项数改动及发布记录现一并纳入 develop 提交，不合并 main。
