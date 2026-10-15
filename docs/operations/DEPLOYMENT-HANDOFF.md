# 部署交接：2026-09-29

已根据本次完整对话整理 [首次部署全过程记录](deployment-journal-2026-09.md)，
按 30 个步骤说明操作、作用、结果与待验事项。回顾完整流程优先阅读该文档；
本文件保留阶段性历史记录，旧段落的“待执行”等描述不代表最新状态。

## 最新确认：生产备份已下载到本机且校验一致

用户已将日常备份保存至 `个人资料/study-20260929-002555.sql.gz`。
本地文件 9,821 bytes，SHA256
`b0b7af8c65f9ede9fd06c8434b7c8e4feea257148a34caf6a3b7fbf65ffc90be`，
与服务器输出一致。仅计算文件哈希，未读取个人数据内容。
`git check-ignore -v` 确认被 `.gitignore` 的 `/个人资料/` 忽略，`git ls-files` 确认未跟踪。
这是一份服务器以外的有效传输副本，但尚未对这份新生产备份做隔离恢复演练。
定时任务首次实际执行、日志轮转、磁盘检查及持续异机备份安排仍待验证或完成。

## 最新停点：续期演练成功，cron 已安装，待下载异机备份

用户回传 `bundle-cert.sh check-renewal`：all simulated renewals succeeded，
Nginx 配置校验及 reload 均成功。已手动生成日常备份：
`/srv/408doing/private/daily-backups/study-20260929-002555.sql.gz`，
SHA256 `b0b7af8c65f9ede9fd06c8434b7c8e4feea257148a34caf6a3b7fbf65ffc90be`。
cron 服务 active；用户使用 nano 编辑 root crontab，完整回传 `crontab -l` 确认：

```cron
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
17 3,15 * * * /usr/bin/flock -n /run/lock/study-renew.lock /bin/bash /srv/408doing/study-20260928-150827/scripts/bundle-cert.sh renew >> /var/log/study-renew.log 2>&1
35 4 * * * /usr/bin/flock -n /run/lock/study-backup.lock /bin/bash /srv/408doing/study-20260928-150827/scripts/bundle-backup.sh /srv/408doing/private/daily-backups >> /var/log/study-backup.log 2>&1
```

两条定时任务已安装，实际到点自动执行尚未观察；时间以服务器时区为准，时区未单独核对。
下一步请用户从 Workbench 下载上述日常备份到电脑私有目录，并校验下载后 SHA256。
还需安排新生产备份的隔离恢复验证、日志轮转、磁盘检查及定期异机备份；目前未设置备份自动清理。
不要把“定时配置已安装”说成“已观察到自动执行成功”。后面的未配置 cron 等均为历史状态。

## 当前停点：原账号已登录，开始续期与备份配置

用户此前登录提示 BAD_CREDENTIALS，随后记起原密码并明确表示已成功登录，没有重置密码。
询问任务与周计划是否正常后，用户回复“ok”，按人工检查通过继续。
下一步在服务器旧版本目录运行 `bash scripts/bundle-cert.sh check-renewal`，
回传 dry-run 结果后再安装定时续期与备份任务。此测试不会替换正式证书，不停止网站。
cron 尚未配置，异机备份位置尚未选择；不要将后面的历史待登录/待上传视为当前状态。

## 最新线上进展：修复已部署，待原账号人工验收（00:16）

用户已上传修复 jar，服务器 SHA256 与上述交付值一致。
已停止后端并成功执行升级前备份：
`/srv/408doing/private/pre-session-fix/study-20260929-001449.sql.gz`，
SHA256 `9a749ed0ea802ba5970004758c579ac689c5b037824fd15061031465c1aa03b6`。
旧 jar 已另存 `artifacts/app.pre-session-fix.jar`，新 jar 已安装为 `artifacts/app.jar`。
用户回传输出确认：后端强制重建后 21.9 秒 Healthy；Nginx 配置校验成功并已 reload；
正常校验证书的 `curl -i https://onlystudy.loc.cc/api/today` 返回 401 UNAUTHORIZED，
不再是 500，响应会话 Cookie 带 Secure/HttpOnly/SameSite=Lax。

下一步请用户打开 HTTPS 网站，以原账号 Sherryiing 和原网站密码登录，
确认任务/计划完整（此前 30 任务、15 周计划、113 每日安排；学习记录 0 符合预期）。
不要发送密码。原账号登录与线上业务数量尚未人工核对，V13 迁移记录尚未单独查询。
确认后继续证书续期 dry-run、定时续期/备份及独立存储安排；这些仍未完成。
下面“待上传/生产尚未更新”等均为历史状态，以本节为准；不要重复执行替换命令。

## 当前停点：会话修复已验证，待用户上传（优先于下面历史记录）

生产错误日志已收到：`Table 'kaoyan_study.SPRING_SESSION' doesn't exist`。
确认原 Windows SQL 导出小写会话表，Linux MySQL 的 `lower_case_table_names=0` 区分大小写。
本地真实备份副本已复现匿名 `/api/today` 500，不是业务数据丢失。

已新增 `V13__normalize_session_table_case.sql`，不改 V2，仅在大小写敏感库原子重命名
现有小写会话表，保留数据和外键；全新库和大小写不敏感库不改名；同名冲突时失败，不覆盖。
后端完整 `mvnw.cmd -B -Dmaven.repo.local=../.m2repo -o verify`：67 测试全通过。
真实备份测试副本已执行 V13；匿名 401、CSRF 拦截 403、虚拟账号登录 200、会话读取、
今日/任务 200、退出 204、退出后 401 均通过。业务数量 30/1/15/113/0 保持不变。
原 SQL 哈希不变，Windows MySQL80 未修改。

待上传修复文件：

- 本地 `release/study-session-fix-20260929.jar`，40,435,538 bytes（约 38.6 MiB）。
- SHA256 `77b69faa2e74f773dde80f79e55ffeda2e024cbcb87ab53ae82ad61ee4933176`。
- 上传目标 `/srv/408doing/study-session-fix-20260929.jar`，不要直接覆盖运行中的 app.jar。
- **生产尚未更新，仍不能宣称线上登录恢复。** 本轮最终提示用户上传并运行 sha256sum。

用户回传校验后，分步指导：进入旧版本目录 `/srv/408doing/study-20260928-150827`，
停止 backend，执行 `bash scripts/bundle-backup.sh /srv/408doing/private/pre-session-fix`，
成功后将现有 `artifacts/app.jar` 备份为 `artifacts/app.pre-session-fix.jar`（若已存在先停，不覆盖），
用 `install -m 644 /srv/408doing/study-session-fix-20260929.jar artifacts/app.jar` 替换。
用 `docker compose -f compose.bundle.yaml up -d --no-deps --force-recreate --wait --wait-timeout 360 backend`
强制重建后端以重新绑定 jar，健康后 `docker compose -f compose.bundle.yaml exec -T frontend nginx -s reload`。
核对迁移 V13、业务数量、匿名 HTTPS 401，然后请用户使用原账号 Sherryiing 验证浏览器登录。
不要重导 SQL、重建库、删除卷或重新签发证书。后续证书续期 dry-run、cron、独立备份尚待完成。

本地隔离副本测试容器再次停止，保留卷；本轮测试使用忽略目录中的
`release/session-fix-restore-override.yaml` 和 `release/test-restored-session.ps1`，
后端仅绑定 127.0.0.1:18081，测试 HTTP 显式覆盖 `SERVER_SERVLET_SESSION_COOKIE_SECURE=false`；
生产配置仍强制 Secure。测试副本新增 `session-fix-test-only` 虚拟账号，绝不能用于生产重新导出。
初次测试误用了 SESSION_COOKIE_SECURE 覆盖，被 prod 配置压过；正确测试覆盖后会话完整验证通过。
原旧程序包未改，上传修复 jar 后旧 SHA256SUMS 不再代表现有 app.jar，要保留修复哈希记录。

## 最新线上状态（优先阅读）

用户已完成上传、CRLF 兼容校验、bundle-init、真实 SQL 导入（V1–V12 成功、30 任务/0 记录），
Certbot 已成功签发 onlystudy.loc.cc 证书（截图到期日 2026-12-27），
`docker compose -f compose.bundle.yaml up -d --wait --wait-timeout 360` 后三服务 healthy。
原账号 Sherryiing 的浏览器登录尚未确认；证书自动续期和定时备份尚未配置。

本地公网 curl 验证（未跳过 TLS 验证，2026-09-28）：

- HTTPS `/` 200，HTTP `/` 301 到正确 HTTPS 域名。
- `/actuator/health` 404，管理端点未公开。
- `/api/auth/me` 200，`authenticated:false`。
- **匿名 `/api/today` 返回 500 而非预期 401**，JSON 为 INTERNAL_ERROR，path `/error`。
  该问题尚未定位，不能宣布完整验收通过。需用户从 Workbench 提供后端最近错误日志，
  下一步命令 `docker compose -f compose.bundle.yaml logs --since 10m --tail 150 backend`。
  此前本地虚拟数据 HTTPS 测试的匿名 401 成功，不能代替当前线上实际结果。

下面各节保留历史过程，涉及“尚未上传/未启动”的表述以本节为准。

## 用户目标与约束

用户希望把当前个人学习网站部署到阿里云；需要逐步指导 Workbench/控制台操作。
已明确选择预算优先的 2 核 2 GB，并明确要求迁移保留本地已有学习数据。
不要重置业务库、不要先把空库网站当成迁移完成；不要索取明文密码发到聊天。
先读 PLAN.md 和 docs/STRUCTURE.md，再继续修改。

## 服务器已完成（由用户操作，未取得本地可用 SSH 认证）

- 香港 ECS，Ubuntu 22.04，公网 IP `8.217.241.16`，2 核 2 GiB，40 GiB 磁盘。
- 用户通过 Workbench 以 root 登录。
- Docker 29.8.1 / Compose v5.5.1，服务 active；当时没有运行中的容器。
- `/swapfile` 1 GiB，已 swapon 并追加到 /etc/fstab。
- 安全组已允许公网 TCP 80/443；SSH 22 保留。UFW inactive，80/443 无占用。
- 域名 `onlystudy.loc.cc`（NodeLoc 免费域名），A 记录指向上述 IP，TTL 3600。
- 用户 `getent ahostsv4 onlystudy.loc.cc` 已返回正确公网 IP。
- 没有上传代码/备份，没有启动生产应用，没有签发公网证书。

## 本地状态与修改

工作区 `D:\Project_JAVA\408doing`。现在已有 .git，开始本轮时工作树干净。
由于沙箱初始化发生 `helper_unknown_error: setup refresh had errors`，
普通 exec 无法启动；经 require_escalated 执行成功。不要误报为项目或服务器故障。
Git 在沙箱外有 ownership 提示，检查用单次参数
`git -c safe.directory=D:/Project_JAVA/408doing ...`，没有修改全局 trust。

新增：

- `deploy/compose.bundle.yaml`：使用固定官方镜像摘要的手动上传交付，同一单体应用。
  不在 2 GB 服务器编译。MySQL 512m、后端 768m（JVM Xmx384m）、Nginx 96m。
  MySQL/后端不发布端口，后端 UID 10001，日志临时目录可写，Docker 日志轮转。
- `deploy/nginx/bundle.conf.template`：HTTP 跳转 HTTPS、ACME webroot、管理端点拦截。
- `deploy/scripts/build-bundle.ps1`：本地测试/构建并用白名单打包，不含数据/密码。
- `deploy/scripts/export-local-database.ps1`：本机交互式密码输入导出单库，备份存 deploy/backups。
- `deploy/scripts/bundle-init.sh`：创建私密 .env，不覆盖旧文件，DB 密码随机生成。
- `deploy/scripts/bundle-import.sh`：仅导入空目标库，发现已有表或后端运行即拒绝。
- `deploy/scripts/bundle-cert.sh`：初次 standalone 签发，后续 webroot 续期。
- `deploy/scripts/bundle-backup.sh`：单库一致性备份，无自动删除旧备份。
- `docs/operations/manual-deploy.md`：完整手动步骤。
- README、STRUCTURE、原运维文档增加入口，.gitignore 忽略 release/。

原 GHCR compose/release 工作流没有修复，手动包独立使用 compose.bundle.yaml。
不要混用两个 Compose 文件启动同一项目。新增内容尚未提交 Git。

## 已完成验证

执行 `./deploy/scripts/build-bundle.ps1 -Offline`：

- 后端 `./mvnw.cmd -B -Dmaven.repo.local=../.m2repo -o verify`：
  Testcontainers 隔离 MySQL 8.4.11，65 tests，0 failures/errors/skipped；V1–V12 成功。
- 前端 `pnpm run test`：31/31；`pnpm run build`（含 vue-tsc）成功。
- `docker compose -f deploy/compose.bundle.yaml config --quiet`（临时测试变量）通过。
- 每个 bundle-*.sh 使用 Git Bash `bash -n` 通过，字节检查无 CRLF。
- PowerShell AST 解析脚本通过；`git diff --check` 通过。

本地隔离 Compose 项目 `study-smoke-20260928`：

- 通过包内 compose up --wait 启动 mysql/backend，再用 release/smoke-override.yaml
  把前端仅绑定 `127.0.0.1:18080/18443`。使用一天有效期自签证书，未调用真实 CA。
- 三服务 healthy，`nginx -t` 通过。
- 本地 HTTPS 未登录 `/api/today` 为 401；`/actuator/health` 为 404；
  登录不带 CSRF 为 403，带 CSRF 成功，SESSION Cookie 有 Secure，/me 已认证，/api/today 200。
- 当时空闲内存 MySQL 249.3 MiB，Java 250.4 MiB，Nginx 28.84 MiB。
  这不是负载测试，不能据此保证任意负载下的 2 GB 稳定性。
- 停止本次测试容器后可用同一独立项目继续验证；不要操作本地 MySQL80 业务库。

程序包已生成：

`release/study-20260928-150827.tar.gz`，37,226,033 bytes。

SHA256：`28f0bbb43ca22fd3364d0817640f0e5235701373ed296b9df4f44102524dcc51`

包内 VALIDATION.txt 记录构建时状态，当时尚未进行上述后续容器测试；以此交接补充说明。
测试生成的自签证书在忽略的 release/smoke-certs/，不在程序包中，不可用于生产。

## 真实备份恢复验证已完成（2026-09-28 晚）

- 用户在本地交互登录确认数据库名 `kaoyan_study`，并运行导出脚本。
- 备份：`deploy/backups/kaoyan_study-20260928-191657.sql`，54,433 字节。
  SHA256 `ff3013df9bc493217c14919cdf304fa01d42061c22b69794814a64daf797faea`。
- 使用包内 `scripts/bundle-import.sh`，通过环境变量
  `COMPOSE_PROJECT_NAME=study-restorecheck-20260928-191657` 导入全新 MySQL 8.4 隔离卷。
  该项目没有数据库/后端的宿主机端口，不连接 Windows MySQL80。
- 恢复结果：30 个任务、1 个阶段目标、15 个周计划、113 条每日安排、0 条学习记录。
  **用户明确确认：只有计划/任务，还没保存学习记录，0 条符合预期。**
- 原网站登录用户名 `Sherryiing`（注意大小写），不是 MySQL 的 root/kaoyan。
  原密码哈希保留在备份中，服务器初始化 PRESET_ACCOUNT_USERNAME 必须填写 `Sherryiing`。
- 第一次测试环境用了默认 kaoyan，测试副本中可能额外创建了虚拟账号；后续改为 Sherryiing
  再启动验证通过。原始 SQL 和源数据库未变；不要把测试副本重新导出后作为生产备份。
- 部署包后端连接恢复副本，Compose `up -d --wait backend` 成功，健康端点 UP，
  Flyway 最高版本 12、失败迁移 0；启动后仍为 30 任务、0 记录。
- 未使用用户网站明文密码，所以真实账号的端到端登录仍需由用户在上线后验证。
- 测试容器已 `docker stop`：
  `study-restorecheck-20260928-191657-backend-1` 和 `study-restorecheck-20260928-191657-mysql-1`。
  临时测试卷保留在本机；原 MySQL80 继续运行。备份 SHA256 在测试前后相同。

## 下一步：上传服务器

最新进度：用户已上传上述原始程序包和 SQL，服务器 `sha256sum` 两个结果与本地完全一致。
程序已解压到 `/srv/408doing/study-20260928-150827`。
执行包内 SHA256SUMS 校验时报每行文件名末尾含 `$'\r'`，因此 `&&` 后的 bundle-init.sh 尚未运行。
原因是旧打包脚本 WriteAllLines 在 Windows 生成 CRLF，已修正源码为 WriteAllText 显式 LF。
无需重新上传原包：用 `tr -d '\r' < SHA256SUMS | sha256sum -c -`，通过后执行 bundle-init.sh。
仍未创建服务器 .env、导入数据或启动生产应用。下面第 1–2 步上传已完成，其余待执行。

1. 指导用户在 Workbench 终端创建 `/srv/408doing` 和仅 root 可访问的 `/srv/408doing/private`。
2. 上传程序包 `release/study-20260928-150827.tar.gz` 到 `/srv/408doing/`，
   上传**原始** SQL `deploy/backups/kaoyan_study-20260928-191657.sql` 到 `/srv/408doing/private/`。
   尚未执行任何上传。服务器上校验两文件 SHA256 后解压程序包。
3. `bundle-init.sh` 填入域名和原应用用户名 `Sherryiing`，数据库密码自动生成。
   不发送 .env 或密码到聊天。备用密码不会重置恢复后的原应用账号。
4. 使用 `bundle-import.sh` 向服务器空库导入，核对上述数量，之后才启动后端。
5. 用户确认 CA 协议、提供证书联系邮箱后签发真实证书，启动网站；公网 HTTPS 验收。
6. 证书续期 dry-run、定时任务、独立备份位置、服务器 `bundle-backup.sh` 导出/恢复演练仍待完成。
7. 本地网站继续避免写入；若导出后又新增数据，需要重新导出和验证，避免漏记录。

**不要把当前“本地部署包验证通过”说成“服务器已经上线”。**
