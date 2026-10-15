# 记录与进度发布（2026-10-13）

## 最新状态：19:23:06 正式发布完成，线上验收通过

原入口保持 `https://onlystudy.loc.cc`。新版记录：`/learning`；新版进度：`/progress`。本轮使用 develop `6fae842` 加未提交工作区产物，没有提交或推送 Git。

- 发布包及包内文件哈希通过，生产 MySQL **8.4.11** 的隔离备份恢复通过；候选 V15→V16、目录/归档/候选/今日/计划读取、鉴权/CSRF/JDBC 会话/退出验收通过，输出 `MYSQL84_V15_TO_V16_CANDIDATE_VERIFIED`。
- 停止生产后端后完成最终备份：`/srv/408doing/private/catalog-20261013-1547/final-backup/study-20261013-192223.sql.gz`；SHA256：`2b9c47f6c953e498fd37bccfab4b3609fe9b61af33e971104902e8f313d94506`。备份 gzip 校验通过，旧程序、前端、模板及两个备份均保留。
- 生产 V16 成功，原业务表数量与停服备份前完全一致；新增课程目录元数据没有生成学习记录或安排。输出 `PUBLISH_DONE_V16_BUSINESS_COUNTS_PRESERVED`，`PUBLISHED` 为 `2026-10-13 19:23:06 CST`。
- jar SHA256：`077489eda59126941d8d1e9390ead8b221037ef77b58058284c1e998a6d5e444`；入口 HTML SHA256：`2231578b0c329027e05b766931c0f3669431f75d6b3722121e2bdd30a08785c7`。
- 三正式容器最终均 healthy；`nginx -t` 成功。仅 frontend 发布 80/443，MySQL/后端没有公开端口。候选容器 Running=false，专用验收数据库账号 account_locked=Y。
- 本机执行 `python release/catalog-public-check.py`，退出码 0：匿名目录/归档 401，目录写请求缺少 CSRF 返回 403，管理端点 404，1.1 MB 合法 JSON 请求达到 CSRF 防护而非 413，所有候选前端文件逐字节匹配公网资源。
- Chrome 原账号 Sherryiing 会话保留；通过 PWA “立即更新”后查看新版四方向首页、数学方向空目录、目录导入说明/Schema/示例/当前目录下载入口，以及记录空状态、全部日期、筛选和补记弹窗。今日与计划默认整体视图正常，浏览器 error 日志为空。生产没有演示导入或演示学习记录，当前空状态与升级前业务数据一致。
- 本轮线上检查是只读验收和迁移兼容性验证，不等同于 MySQL 8.4 的全量业务测试或线上离线冷启动测试。月周日有数据的交互、目录确认与修改删除重算仍以本轮既有隔离测试为证据。

截图和公网验证转录在忽略目录 `release/catalog-production-20261013/`；完整服务端证据在 `/srv/408doing/private/catalog-20261013-1547/`，包括两次备份、业务数量、旧产物、candidate.log、candidate-check.log、publish.log、PUBLISHED、resume-prepare.py。首次 UTF-8 恢复失败保留为历史，不重写为通过；原始字节恢复后成功。恢复和回退应先检查新版数据兼容性，不能删除持久卷或恢复旧数据库覆盖新数据。

以下保留发布前的历史检查点：

## 上传后阶段：备份及隔离恢复通过，候选验证中

用户手动上传后，服务器文件 SHA256 与本地一致（`85cf41b2...4474cd6b`），包内 SHA256SUMS 校验通过。新 Workbench 命令终端已连接，生产仍未切换。

- `prepare` 已保留旧 jar、前端及 Nginx 模板，完成生产备份及 gzip 校验。备份 SHA256：`bde446757d3fa7f086a86618fef06c08abb8a7bd14cc204ff8713081b44bc532`。
- 首次隔离恢复因备份包含 JDBC Session 二进制而 UTF-8 decode 失败，隔离库已创建但无表；原生产不受影响。新增私有 `resume-prepare.py`，先断言隔离库空且生产 V15，然后按原始字节导入备份，核对业务表数量、V15 及恢复标记。输出 `BACKUP_RESTORE_MYSQL84_VERIFIED`，没有重复创建或清理数据。
- 本机发布脚本修正为按字节传递 SQL；上传包中的原脚本及 SHA256SUMS 保持原样，恢复修复由私有伴随脚本完成。
- 已后台启动 `candidate`，日志在 `/srv/408doing/private/catalog-20261013-1547/candidate-check.log`。尚未确认候选或正式发布成功，继续检查该日志和标记文件。

以下保留上传前的历史状态。

用户已明确授权部署。当前 develop HEAD 为 `6fae842`，本次实现仍在未提交工作区；没有提交或推送。

- 已完整阅读 PLAN、AGENTS、STRUCTURE、最新开发交接及部署说明。
- 发布前重新执行 `frontend: pnpm test; pnpm build`：9 个文件 76 项测试通过，类型检查和生产/PWA 构建通过。
- 后端重新执行 `mvnw.cmd -o -B -Dmaven.repo.local=../.m2repo -DskipTests package`：BUILD SUCCESS。本次未重复数据库测试，之前真实 MySQL 8.0.45 的 44 项相关测试见第二轮验收记录。
- 发现 Nginx 默认 1 MB 限制与课程目录 2 MB 规则不一致，已为 `/api/progress/catalog/preview` 单独配置 5m 请求体、180s 读取超时；bundle 和普通部署配置同步。尚待服务器 `nginx -t` 及公网大请求验证。

## 服务器只读核对

经用户重新打开的阿里云 Workbench 检查：

- 服务器：`8.217.241.16`，生产目录 `/srv/408doing/study-20260928-150827`。
- MySQL `8.4.11`，Flyway 最后两条 `15:1`、`14:1`。
- backend/frontend/mysql 三容器 healthy；仅 frontend 发布 80/443。
- 根磁盘可用约 30 GB，available memory 553 MB，swap 1023 MB。
- `study_record` 当前数量 0。这里只核对事实，没有执行任何学习任务删除。
- SSH 已能连接认证阶段，但本地密钥未通过 `root` 认证；没有改动 SSH 权限或账号。

Workbench 文件上传 `setFiles` 报 `No node found for given backend id`，随后观察到远程会话已断开。文件是否上传成功尚未确认。已请用户重连并检查 Chrome 扩展“允许访问文件网址”。**尚未执行备份、隔离迁移或 publish，不能宣称部署完成。**

后续更新：Workbench 已通过既有免密连接恢复；终端 `ls` 明确确认 `/srv/catalog-20261013-1547.tar.gz` 不存在。用户确认扩展权限后，重新取得 filechooser 并以 20 秒超时调用上传，工具仍长时间阻塞并报相同 `No node found for given backend id`。停止重复调用上传工具，交由用户从 Workbench 手动上传该包到 `/srv/`。恢复后首先核对服务器包 SHA256，再继续 prepare/candidate/publish。重连后的终端 input 曾为 333，但下次必须重新读取 UI，不复用旧索引。

## 本地候选包

### 再次继续后的检查点

- 用户要求检查代码及 Git diff 后继续；已复核当前差异、`git diff --check` 和发布包/jar 哈希，均与上述检查点一致，无需重做功能或构建。
- 新版 Workbench 文件管理再次提示 `Session has expired`；完整刷新并重建免密会话后目录读取正常，但上传仍报过期。
- 已切到 Workbench 旧版，重新免密连接，打开独立 File Navigator 并进入 `/srv/`。旧版成功产生 filechooser，但 `setFiles` 依然报 `No node found for given backend id`。已尝试不同 UI，确认自动文件选择接口仍阻塞；没有调用 prepare/candidate/publish。
- 浏览器保留在旧版 `/srv/` 的“上传文件”按钮。服务器列表仍只有旧版 planning 包，没有本次 catalog 包。需要用户在此按钮手动选择 `D:\Project_JAVA\408doing\release\catalog-20261013-1547.tar.gz`；上传后继续校验与发布。
- 更早一次浏览器操作曾因自动审批服务额度耗尽而未执行，后来用户继续后读取和普通点击已恢复；当前阻塞是文件选择器错误，不是安全拒绝。

- `release/catalog-20261013-1547.tar.gz`
- SHA256：`85cf41b2af3103e1a2963e73e7dbf07e908d21ca35b09bb2dfb27dd04474cd6b`
- jar SHA256：`077489eda59126941d8d1e9390ead8b221037ef77b58058284c1e998a6d5e444`
- 包内仅 app.jar、前端 dist、Nginx 模板、发布脚本、验证说明及 SHA256SUMS。不含账号环境文件、数据库或备份。
- `release/catalog-release.py` 与包内副本一致；`release/catalog-public-check.py` 留在本机用于最终公网验证。两个 Python 文件语法检查通过。

## 恢复步骤

1. 重新取得 Workbench 当前 UI，确认终端已连接；检查 `/srv/catalog-20261013-1547.tar.gz` 是否存在并核对以上哈希，未成功才重传。
2. 检查没有已有同名解压目录，解压后运行 `sha256sum -c SHA256SUMS`。不覆盖服务器 `.env`、Compose、证书或数据库卷。
3. 在包目录执行 `python3 catalog-release.py prepare`。脚本要求生产 V15、三容器健康，保留旧程序及前端，备份并 gzip 校验，在新的隔离库恢复，核对业务表数量。
4. 执行 `python3 catalog-release.py candidate`。以现有固定 Java 镜像启动无公开端口的受限候选容器，验证 MySQL 8.4 V15→V16、原业务数据数量、目录与记录 API、今日/计划兼容、登录/会话/CSRF/退出。临时验收账号只在隔离库；结束停止候选容器、锁定专用数据库用户。
5. 只有出现 `MYSQL84_V15_TO_V16_CANDIDATE_VERIFIED` 后才执行 `python3 catalog-release.py publish`。脚本先校验 Nginx，再停止后端并做最终备份，替换 jar，确认 V16 和业务数量，再原子更新前端（保留旧哈希资源）并重建 frontend。
6. 运行本机 `python release/catalog-public-check.py`，核对匿名 401、CSRF 403、管理端点 404、1.1 MB 目录请求到达 CSRF 而非 413、所有线上静态文件与候选包一致。
7. 用户原账号登录后只读核对进度四方向、记录月周日、今日及计划；处理 PWA 更新提示。不得在生产导入演示目录或制造测试记录。

预定私有证据目录 `/srv/408doing/private/catalog-20261013-1547`；隔离库 `catalog_upgrade_20261013_1547`；候选容器 `catalog-upgrade-20261013-1547`。脚本遇到已有状态会停止，应先检查证据，不可盲目重跑或删除状态来绕过检查。

V16 只新增六张课程目录及归属表。失败时保留日志、备份和数据库状态，检查迁移结果后再评估回退；不删除卷或恢复旧数据库覆盖上线后的新数据。
