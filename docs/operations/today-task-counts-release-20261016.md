# 今日任务项数发布（2026-10-16）

## 当前状态

用户明确要求部署，已于 **2026-10-16 11:25:10 CST** 完成正式切换。发布包校验、生产备份与 MySQL 8.4 隔离恢复、候选程序验收、正式站与业务数据保留检查均已通过。浏览器原登录会话保持，PWA 已点击“立即更新”；11:27 的真实今日页显示“已完成 0 项 / 进行中 0 项 / 还剩 5 项”。

变更为今日顶部左“已完成”、中“进行中”、右“还剩”三列任务项数。部分完成及当日仅记用时的未完成任务归进行中；还剩指尚未开始。学科分区与任务内用时保留。没有新增迁移，Flyway 保持 V20，不写生产测试学习记录。

## 版本与产物

- Git 分支为 `develop`，本轮没有提交、推送或合并 main；保留了工作区原有改动。
- 本地发布包：`release/today-counts-release-20261016.tar.gz`，37,103,731 字节。
- 发布包 SHA256：`a2a7c836e1cabb8b364d05a6c1ef4a749b414451d25cda8ec535ad08e7b37bdd`。
- 新 jar SHA256：`bf9a0f029d3f794f847c0d320a2360ce76e94d08cdc0d1e42d13b9d3712c4487`。
- 新 `index.html` SHA256：`12f91cf7362f5be9ab468919763f1ffc8bb5a3227c0062c08c3d6fab39e8a7c1`，HTTPS 返回内容与发布包一致。
- 旧 jar SHA256：`cdc558e15f5dce2057ac25e28c39aa5287627e593be9769ba338e999d9336a07`，发布前与实时产物一致。
- 包上传至 `/root/today-counts-release-20261016.tar.gz`，外部 SHA256 与内部 `SHA256SUMS` 均通过。程序及脚本位于 `/root/today-counts-release-20261016/`。

## 已完成验证

本地 Java 21.0.6、Maven Wrapper 3.9.16、隔离 MySQL 8.0.45 上，后端 138 项及 `verify` 打包通过；前端 89 项、类型检查与生产构建通过。1366px、390px、320px 本地布局检查通过。

正式打包脚本首次执行因 2 项 Linux Docker 会话表大小写专测缺少 Docker 而中止；随后明确排除 `SessionTableCaseMigrationTest` 后运行可用的 138 项。没有把这两项记为通过，也没有修改它们来规避环境限制。详见 [开发与构建验收](../testing/today-task-counts-20261016.md)。

生产沿用 `/srv/408doing/study-20260928-150827` 的 `compose.bundle.yaml`，正式域名为 `https://onlystudy.loc.cc/`。发布前核对三个服务 healthy，旧 jar 与上次 V20 发布一致，MySQL 为 8.4。

使用现有 MySQL 服务的新隔离库 `today_counts_verify_20261016` 恢复备份，31 张业务表逐表行哈希和数量一致，输出 `BACKUP_RESTORE_MYSQL84_ALL_BUSINESS_TABLES_VERIFIED 31`。会话表及 Flyway 历史不混入业务行哈希；迁移版本单独核对 V20。

预检备份 SHA256：`1127a5b669adb0f300670058d98ad768a937c5e7521714b75bb85a95fcd511f8`。

候选容器 `today-counts-verify-20261016` 限制内存 320MB、堆 176MB、CPU 0.75、进程数 128、数据库连接池 2，不公开端口，不新增 MySQL 实例。基于隔离恢复库验证匿名业务 API 401、无 CSRF 写请求 403、登录、会话、退出及今日统计，与 SQL 有效记录和清单核对一致：

- 2026-10-16：已完成 0、进行中 0、还剩 5。
- 2026-10-15：已完成 3、进行中 0、还剩 2。

候选没有写入业务学习记录，只有隔离库中的临时验收账号与会话。验收后停止候选并锁定其数据库账号，生产业务行哈希仍与预检基线一致。输出 `MYSQL84_CANDIDATE_AUTH_CSRF_SESSION_COUNTS_PRESERVATION_VERIFIED`。

## 恢复点与接续

私有状态位于 `/srv/408doing/private/today-task-counts-20261016/`，含旧 jar、旧前端、预检及最终数据库备份、业务表哈希和验收标记。`RESTORED`、`CANDIDATE-VERIFIED`、`PUBLISHED` 均已完成，不得重复执行发布。接续时先只读核对标记与实时状态，不覆盖新学习记录。

- 预检备份：`preflight-backup/study-20261016-111901.sql.gz`。
- 最终备份：`final-backup/study-20261016-112357.sql.gz`，SHA256 为 `013376837455dc2ddf6cb6f64e87c26a9c3b4f97e20cea2909c8866d995f9256`。

最终停止后端写入后备份，再原子替换 jar 并强制重建 backend；新版本健康且业务行哈希一致后，追加前端新资源、保留旧 hash 文件，最后切换入口与 Service Worker 并检查／重载 Nginx。三个服务均 healthy，匿名 `/api/today` 返回 401。

输出 `PUBLISHED_TODAY_TASK_COUNTS_ALL_BUSINESS_ROWS_PRESERVED`，31 张业务表数量及行哈希在切换前后完全一致。关键表保持：task=255、daily_plan_item=529、study_record=4、plan_import=4、study_continuation_stream=2、task_continuation=252。原学习记录、英语、LeetCode、课程目录及两书接续数据全部保留，没有生产测试学习记录。候选容器最终 `State.Running=false`，测试数据库账号已锁定；本机临时 MySQL 也已关闭。

线上截图及终端证据保存在忽略目录 `release/today-task-counts-20261016/production-today.png`、`production-published.png`。

本次只改只读返回和界面，无新迁移；如需程序回退，使用本次保存的前后端配对产物，并重新核对实时数据兼容性。不得直接恢复旧全库覆盖发布后的学习记录，也不得删除数据库卷。
