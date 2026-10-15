# 2 GB 服务器手动部署与数据迁移

这是现有 Nginx + Spring Boot + MySQL 单体的手动交付方式；不依赖 GitHub/GHCR。
程序在 Windows 构建后上传，服务器只运行固定摘要的官方镜像。
使用 `compose.bundle.yaml`，不要与原 `compose.yaml` 混用（共享同一 Compose 项目名和数据库卷）。
本次目标域名 `onlystudy.loc.cc` 仅由服务器 `.env` 设置，不写入业务规则。

## 本地构建

在仓库根目录的 PowerShell 执行：

```powershell
./deploy/scripts/build-bundle.ps1 -Offline
```

需要 Java 21、pnpm 9.15、已安装的前端依赖及 Maven 缓存。默认运行后端全部测试；
集成测试必须使用 Docker/Testcontainers 或**独立的一次性测试库**，不得指向正在使用的业务库。
如暂时只有本地 MySQL 业务库，可明确加 `-UnitTestsOnly` 制作候选包；
它仅运行后端 26 个单元测试，并在 `VALIDATION.txt` 标明集成测试未运行，不能据此宣布可生产上线。
首次无缓存时去掉 `-Offline`。

生成 `release/study-时间戳.tar.gz`，仅包含 jar、前端 dist、指定部署配置和脚本。
不含 `.env`、数据库、证书、node_modules 或源代码。SHA256SUMS 校验包内文件。
运行前仍须完成 MySQL 8.4 迁移、容器健康检查与 HTTPS 登录验收。

## 保留本地数据

先确认实际数据库名和数据库账号，暂停本地网站的写入，然后在本机 PowerShell 执行：

```powershell
./deploy/scripts/export-local-database.ps1 -Database 实际数据库名 -DbUser root
```

密码在本机终端的提示中输入，不发送到聊天、不出现在命令行参数中。
结果保存在被 Git 忽略的 `deploy/backups/`。导出包含应用账号哈希、Flyway 历史及业务数据；
不包含 MySQL 系统账号，不使用 `--databases`，便于恢复到服务器的 `kaoyan_study` 库。
备份属于敏感文件，通过 SSH/SFTP 上传，禁止放进前端目录或公开下载链接。
先把备份恢复到隔离 MySQL 8.4 测试实例，并验证迁移、任务/记录数量，再执行生产恢复。
切换完成前保持旧数据库可恢复，不让本地和服务器两份数据库同时接受新记录。

## 服务器首次准备（Ubuntu，Docker Compose）

使用 Workbench 文件上传/SFTP，将程序包与 SQL 分别上传到 `/srv/408doing/` 和私有备份目录。
以 root 操作时可创建权限 700 的备份目录；不要将数据库密码或 SQL 截图上传。
解压到独立版本目录，进入该目录：

```bash
sha256sum -c SHA256SUMS
bash scripts/bundle-init.sh
docker compose -f compose.bundle.yaml config --quiet
docker compose -f compose.bundle.yaml pull
```

初始化脚本不覆盖 `.env`，数据库密码随机生成，文件权限 600。
早期 `study-20260928-150827.tar.gz` 的 SHA256SUMS 使用 Windows CRLF 换行。
如果校验报文件名末尾含 `$'\r'`，使用
`tr -d '\r' < SHA256SUMS | sha256sum -c -` 校验同一清单，再执行初始化。
这只移除清单输入中的回车，不跳过哈希校验、不改变程序或数据库；打包脚本已改为固定 LF。
应用用户名应与原本网站登录用户名一致。备用密码仅在账号缺失时创建账号，不能重置恢复进来的账号密码。
不得在日志或截图中展示 `.env` 或完整 `docker compose config` 输出。

服务器空库先导入，不要先运行后端：

```bash
bash scripts/bundle-import.sh /绝对路径/本地备份.sql
```

脚本只接受没有表的目标库，并核对迁移版本及任务/记录数量。部分导入失败时保留现场，
不要删除卷或强行重试；先检查错误。首次启动后 Flyway 会执行备份尚未包含的新迁移；
当前 V12 会补充数学一轮计划，因此迁移后计数可能增加，需要与预期变化对照。
恢复验证通过后才能启动网站。

## HTTPS 与启动

先确认域名 A 记录正确、没有错误的 AAAA 记录，公网 80/443 放行。
Nginx 尚未启动时运行初次签发（交互确认 CA 协议）：

```bash
bash scripts/bundle-cert.sh issue
docker compose -f compose.bundle.yaml up -d --wait --wait-timeout 360
docker compose -f compose.bundle.yaml ps
docker compose -f compose.bundle.yaml exec -T frontend nginx -t
docker compose -f compose.bundle.yaml exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health
curl -I https://你的域名/
curl -i https://你的域名/api/today
docker stats --no-stream
```

未登录业务 API 应返回 401；浏览器应使用可信 HTTPS，原账号能登录，任务/记录与本地核对一致。
人工验证手机与电脑同步、CSRF 写请求、修改记录、退出及离线快照。不要仅凭 healthz 宣布发布成功。
只有 Nginx 发布端口，MySQL 和后端不发布端口，管理端点由 Nginx 拦截。
Java 堆最大 384 MB，后端容器 768 MB、MySQL 512 MB、Nginx 96 MB；这是初始运维配置，
必须结合实际 `docker stats`、OOM 和响应延迟调整，不能保证任意负载都适合 2 GB。
后端文件日志写入可写的临时目录，主要日志通过 `docker compose ... logs` 查看并受轮转限制，
避免宿主机日志目录与非 root UID 不匹配。容器重建后临时文件日志不保留。

## 证书续期、备份与回退

### Windows 备份迁移后的会话表修复

如果日志报 `Table 'kaoyan_study.SPRING_SESSION' doesn't exist`，而导入的是小写
`spring_session` / `spring_session_attributes`，使用包含 V13 的新后端程序。
V13 在大小写敏感 MySQL 上原子重命名已有小写表，保留数据及外键；新建 Linux 库和
大小写不敏感的 Windows 库无需重命名。若大小写同名表同时存在，迁移会报错而非覆盖。
不要删除数据库卷、重放 V2、修改既有迁移或更改已初始化 MySQL 的 lower_case_table_names。

现有手动包只需更新 `artifacts/app.jar`：先核对上传 jar 的 SHA256，停止后端，
运行 `bundle-backup.sh` 备份当前库，并另存旧 jar；保留 `.env`、证书和数据库卷。
安装新 jar 后执行 `docker compose -f compose.bundle.yaml up -d --no-deps --force-recreate --wait --wait-timeout 360 backend`。
后端健康后执行 `docker compose -f compose.bundle.yaml exec -T frontend nginx -s reload`，
使 Nginx 重新解析可能改变的后端容器地址。确认 Flyway V13 成功、匿名业务 API 401，
再用原账号登录核对数据。旧包的 SHA256SUMS 不再描述更新后的 jar，应保存新旧哈希与升级记录。
若启动失败，保留日志及备份，不重复导入或删除数据；先核对迁移结果再决定回退。

### 定时续期与备份

```bash
bash scripts/bundle-cert.sh check-renewal
bash scripts/bundle-cert.sh renew
bash scripts/bundle-backup.sh /srv/private-backups
```

续期使用 HTTP webroot，不停网站。dry-run 成功后通过 `crontab -e` 配置定时任务：
将下面的 `/srv/408doing/当前版本目录` 换成实际的绝对路径（升级时同步更新）。

```cron
17 3,15 * * * /bin/bash /srv/408doing/当前版本目录/scripts/bundle-cert.sh renew >> /var/log/study-renew.log 2>&1
35 3 * * * /bin/bash /srv/408doing/当前版本目录/scripts/bundle-backup.sh /srv/private-backups >> /var/log/study-backup.log 2>&1
```

检查 cron 运行状态、日志与剩余证书天数；为日志配置 logrotate。备份脚本不会自动删除旧备份，需监控磁盘，
并将备份复制到独立设备/存储。仅 gzip 校验不等于恢复验证，定期在独立测试实例恢复并核对数据。
程序升级使用新的独立版本目录，安全复制旧 `.env`，共享同一数据库/证书卷。
应用回退切回旧版本目录启动；涉及不兼容迁移时必须先制定恢复方案，不能先启动新应用再恢复旧库。
不要运行 `docker compose down -v`，它会删除持久化数据卷。
