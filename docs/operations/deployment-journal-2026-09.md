# 408doing 首次部署全过程记录

整理日期：2026-09-29。主要构建、迁移与上线发生在 2026-09-28 至 09-29；前期选型讨论不补写无法核实的精确时间。

本文按本次对话与部署交接整理，记录“做了什么、为什么做、结果是什么”，不是逐字聊天记录。
缺少可核实输出的早期截图操作，只记录最终确认的结果，不推测具体按钮、订单价格或安装命令。

> 本文中的初始化、导入、替换程序等命令是历史操作记录，不要在已经上线的服务器上从头重跑。
> 不包含密码、真实 `.env` 内容、证书私钥或 SQL 数据。网站已可使用，但不代表所有长期运维项目都已验收。

## 一、最终部署结果

| 项目 | 本次实际结果 |
| --- | --- |
| 网站 | https://onlystudy.loc.cc/ |
| 云服务 | 阿里云 ECS，香港地域 |
| 规格 | 2 核 CPU、2 GiB 内存、40 GiB 磁盘；另加 1 GiB Swap |
| 系统 | Ubuntu 22.04 |
| 公网 IP | `8.217.241.16` |
| 容器环境 | Docker 29.8.1、Docker Compose v5.5.1 |
| 部署方式 | Windows 本地构建，手动上传产物；服务器只运行容器，不编译项目 |
| 服务 | Nginx 前端入口、Spring Boot 后端、MySQL 8.4.11 |
| 配置入口 | `compose.bundle.yaml`，不是原 GHCR 流程的 `compose.yaml` |
| 当前服务器目录 | `/srv/408doing/study-20260928-150827` |
| 账号与数据 | 原账号成功登录；本地原有计划数据迁移保留 |
| HTTPS | 正式证书已签发；模拟续期已成功 |
| 定时维护 | root crontab 已保存续期检查与每日备份任务 |
| 异机副本 | 一份生产备份已下载到电脑，SHA256 与服务器一致 |

几个组件的作用不同：

| 名称 | 简单理解 | 本次负责的事情 |
| --- | --- | --- |
| ECS 服务器 | 一台放在机房、可以持续运行的电脑 | 承载网站和数据库 |
| 域名 | 网站的文字地址 | 让用户不必记公网 IP |
| DNS | 域名到地址的查询系统 | 把 `onlystudy.loc.cc` 指向服务器 IP |
| Nginx | 网站入口与请求转发程序 | 提供前端文件，处理 HTTPS，将 `/api` 请求转给后端 |
| Spring Boot | 后端业务程序 | 处理登录、任务、计划、学习记录等逻辑 |
| MySQL | 数据存储 | 保存账号、业务数据和登录会话 |
| Docker Compose | 多个容器的管理工具 | 按同一份配置启动和管理网站服务 |

实际访问时：浏览器先查询 DNS，再连接服务器上的 Nginx；页面由 Nginx 提供，业务请求由后端处理并访问 MySQL。
DNS 不代替服务器运行代码，也不保存学习数据；本次没有把 NodeLoc DNS 当作网站反向代理。

## 二、准备服务器与访问入口

### 第 1 步：明确部署目标与预算

- **做了什么：**讨论能否部署到远程服务器、是否购买阿里云、2 GB 是否够用，以及优惠后的续费成本。
- **作用：**先确定实际使用规模和长期预算，再选择资源，避免只看短期优惠。
- **结果：**按个人学习网站的当前规模，采用预算优先的 2 核 2 GB 方案；不是承诺该规格能承受任意访问量。

### 第 2 步：选择产品、地域和系统

- **做了什么：**讨论 ECS 云服务器与轻量应用服务器的区别，最终使用香港 ECS，Ubuntu 22.04。
- **作用：**ECS 提供较灵活的计算与网络配置；轻量应用服务器通常是更简化的套餐与管理入口。实际资源和价格仍取决于具体套餐。
- **理解：**“2 核”是 CPU 计算资源，“2 GB”是运行内存，不是磁盘容量。本次磁盘为 40 GiB。
- **结果：**服务器公网 IP 为 `8.217.241.16`；没有把早期优惠报价写成实际成交或续费价格。

### 第 3 步：通过 Workbench 进入服务器

- **做了什么：**在阿里云控制台打开 Workbench，进入 `root@...` 的远程终端。
- **作用：**通过浏览器管理 Linux 服务器，后续在这里执行部署命令、上传和下载文件。
- **账号区分：**阿里云账号用于管理云资源；Linux `root` 是操作系统管理员；MySQL 账号用于连接数据库；网站账号用于登录学习网站。这些不是同一个账号体系。
- **说明：**`root` 是 Linux 的标准管理员账号，不需要在学习网站注册。早期页面中其他账号的创建来源未获得足够证据，不作推断。

### 第 4 步：准备 Docker 与 Compose

- **做了什么：**准备并确认 Docker、Compose 可用；后续记录的版本分别为 29.8.1 和 v5.5.1。
- **作用：**把 MySQL、Java 运行环境和 Nginx 放在相对独立的容器里，减少服务器环境差异。
- **结果：**后续全部使用 `docker compose -f compose.bundle.yaml ...` 管理本次部署。没有记录到的安装命令不在这里补造。

### 第 5 步：增加 1 GiB Swap

在服务器执行过：

```bash
if [ -e /swapfile ]; then
  echo "/swapfile 已存在，先不要覆盖，请把结果发来"
else
  fallocate -l 1G /swapfile &&
  chmod 600 /swapfile &&
  mkswap /swapfile &&
  swapon /swapfile &&
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

free -h
swapon --show
```

- **作用：**当物理内存紧张时，用一部分磁盘作为缓冲，降低突发内存不足的风险；写入 `/etc/fstab` 是为了重启后继续启用。
- **结果：**系统显示物理内存约 1.7 GiB，Swap 1.0 GiB 已启用，当时使用量为 0。
- **限制：**Swap 比内存慢，不能把 2 GB 服务器真正变成 3 GB 内存服务器。

### 第 6 步：取得域名并认识免费域名的边界

- **做了什么：**了解域名和获取方式，最终使用 NodeLoc 提供的免费域名 `onlystudy.loc.cc`。
- **作用：**给网站提供便于访问的地址，并为 HTTPS 证书绑定域名。
- **风险认识：**免费域名受提供方规则约束，可能被回收；提供方的长期服务声明不等于独立的可用性保证。
- **数据边界：**域名回收本身不会删除 ECS 上的代码和数据库，但原地址会失效；更换域名仍需更新 DNS、入口配置和证书，并处理可能受影响的浏览器缓存、登录状态或链接。因此不能说“完全没有影响”。

### 第 7 步：开通 DNS 管理并添加解析

- **做了什么：**区分 NodeLoc 的 Domain 与 DNS 服务，注册域名后使用 DNS 管理添加解析。
- **作用：**Domain 对应域名使用权，DNS 管理对应把这个名字解析到哪里；二者不是一回事。
- **结果：**A 记录将 `onlystudy.loc.cc` 指向 `8.217.241.16`，记录 TTL 为 3600 秒。TTL 是解析缓存时间，不是服务器租期或域名有效期。
- **核对：**服务器运行以下命令，输出的 IPv4 为 `8.217.241.16`：

```bash
getent ahostsv4 onlystudy.loc.cc
```

NodeLoc 说明中的默认 NS 是 `ns1.locns.xyz`、`ns2.locns.xyz`。NS 是负责回答域名查询的服务器，不是 ECS 的网站端口。

### 第 8 步：开放网站端口并检查服务器初始状态

- **做了什么：**进入 ECS 安全组的入方向规则，允许 TCP 80、443；SSH 22 保留。
- **作用：**80 用于 HTTP 跳转及证书验证，443 用于 HTTPS；控制台里的 Web/HTTPS 标签对应常见端口预设，底层仍是相应网络协议和端口。
- **边界：**面向公众的网站端口可允许 `0.0.0.0/0`；数据库 3306、后端 8080 不对公网开放，不能照搬网站规则去开放所有端口。
- **检查：**运行过 `docker ps`、`ss -lntp | grep -E ':(80|443)\b' || true`、`ufw status`。当时没有运行容器、80/443 没有监听，UFW 为 inactive。
- **理解：**安全组放行只是允许连接，不会自动启动网站；UFW inactive 也不代表云安全组不存在。

## 三、本地准备程序与迁移数据

### 第 9 步：确定“本地构建、服务器运行”的交付方式

- **做了什么：**新增手动部署包，前端构建为静态文件，后端构建为 jar，再一起上传。
- **作用：**避免在 2 GB 服务器上执行 Maven、前端依赖安装和编译，降低部署期间的资源压力。
- **资源设置：**MySQL 容器内存上限 512 MiB；后端 768 MiB，Java 堆最大 384 MiB；Nginx 96 MiB。使用固定镜像摘要，不使用浮动 `latest`。
- **持久化：**数据库和证书放在 Docker 命名卷里，重建应用容器不等于删除数据；但删除卷仍会丢数据。
- **安全设置：**只由 Nginx 发布网站端口；后端以非 root 用户运行，数据库只供内部访问。

此次制作的主要文件：

| 文件（仓库内） | 作用 |
| --- | --- |
| `deploy/compose.bundle.yaml` | 定义容器、资源限制、内部网络、卷与健康检查 |
| `deploy/nginx/bundle.conf.template` | HTTPS 入口、前端静态文件、API 转发与管理接口拦截 |
| `deploy/scripts/build-bundle.ps1` | 本地测试、构建并打包程序产物 |
| `deploy/scripts/export-local-database.ps1` | 在本地输入密码后导出数据库 |
| `deploy/scripts/bundle-init.sh` | 首次创建私密部署配置，不覆盖已有 `.env` |
| `deploy/scripts/bundle-import.sh` | 将备份导入空库，拒绝覆盖已有业务库 |
| `deploy/scripts/bundle-cert.sh` | 申请证书、模拟续期和正式续期 |
| `deploy/scripts/bundle-backup.sh` | 导出压缩备份、检查压缩文件并输出校验值 |

### 第 10 步：构建程序并做第一轮测试

在本地项目根目录执行过：

```powershell
./deploy/scripts/build-bundle.ps1 -Offline
```

- **作用：**先发现构建、依赖和业务问题，再交付服务器；`-Offline` 使用已具备的本地依赖缓存，不是让网站离线运行。
- **结果：**后端 65 个测试通过，前端 31 个测试及类型检查、生产构建通过；另外检查了 Compose 配置与脚本语法。
- **产物：**`release/study-20260928-150827.tar.gz`，37,226,033 字节。不包含个人数据库、真实密码或证书私钥。
- **本地运行验证：**隔离环境中验证三个服务健康、自签 HTTPS、登录、CSRF、未登录 401、管理接口 404。自签证书仅供测试，没有拿到生产使用。

### 第 11 步：确认必须迁移原数据，找到源数据库

- **做了什么：**明确已有计划和任务必须保留；启动本地 Windows `MySQL80`，核对 IDEA 中的 `408data` 连接。
- **作用：**避免误把“数据库工具里的连接名称”当作数据库名，也避免只上线一个空网站。
- **结果：**实际数据库名为 `kaoyan_study`。本地导出账号是 MySQL `root`；它与网站登录账号不同。
- **切换原则：**迁移期间避免继续往旧本地网站新增数据，以免本地与服务器两份数据分叉。

### 第 12 步：导出原数据库

对应导出入口：

```powershell
./deploy/scripts/export-local-database.ps1 -Database kaoyan_study -DbUser root
```

- **作用：**将本地账号、密码哈希、业务表和 Flyway 历史保存为 SQL 文件，供服务器恢复。
- **结果：**生成 `deploy/backups/kaoyan_study-20260928-191657.sql`，54,433 字节。
- **保密：**密码在本地交互输入，不发送到聊天。SQL 虽不是明文口令表，但包含账号哈希与个人数据，仍必须私密保存。

### 第 13 步：在隔离数据库试恢复并核对数据

- **做了什么：**将原 SQL 导入本地独立 MySQL 8.4 容器副本，再启动后端检查健康状态和迁移历史。
- **作用：**证明备份可以导入，降低把不可用备份带上生产的风险；测试不直接操作 Windows MySQL80 的原库。
- **数据结果：**30 个任务、1 个阶段目标、15 个周计划、113 条每日安排、0 条学习记录；用户明确确认当时只有计划/任务，0 条学习记录正常。
- **当时验证的不足：**只验证了健康检查、迁移及数量，没有在真实备份副本上完整验证会话写入，因此未提前发现后面的大小写问题。空库登录测试不能替代迁移后登录测试。
- **测试隔离：**测试副本曾新增虚拟账号，不能把它重新导出当作生产原始数据；上传的是未改动的原 SQL。

对话因额度暂停过，期间通过 `DEPLOYMENT-HANDOFF.md` 保存进度，恢复后从检查点继续，而不是重新执行初始化或清空数据。

## 四、上传并首次启动网站

### 第 14 步：建立服务器目录并上传文件

- **做了什么：**创建 `/srv/408doing` 和 `/srv/408doing/private`，通过 Workbench 上传程序包与 SQL。
- **作用：**程序和私有备份分开存放；备份不能放进 Nginx 的公开静态目录。
- **权限：**目录准备使用 `install -d -m 700`，限制其他普通系统用户访问。
- **结果：**程序包在 `/srv/408doing/`，原 SQL 在 `/srv/408doing/private/`。上传时曾出现界面速度为 0，最终上传完成；没有足够证据把该现象归因于某个具体网络故障。

### 第 15 步：核对上传文件 SHA256

在服务器执行过：

```bash
cd /srv/408doing
sha256sum study-20260928-150827.tar.gz
sha256sum private/kaoyan_study-20260928-191657.sql
```

- **作用：**将服务器文件的“指纹”与本地文件比较，确认传输没有损坏或选错文件。
- **结果：**两个值与本地一致，完整值见本文附录。
- **边界：**哈希一致说明两个文件相同，不等于程序没有漏洞，也不等于数据库一定能恢复。

### 第 16 步：解压程序并修复校验清单的换行问题

- **做了什么：**解压到 `/srv/408doing/study-20260928-150827`，校验包内文件。
- **问题：**早期 `SHA256SUMS` 在 Windows 生成 CRLF 换行，Linux 将末尾回车当成文件名的一部分，出现 `$'\r'`、找不到文件等错误。
- **处理：**不重新上传、不跳过校验，只对清单输入移除回车：

```bash
tr -d '\r' < SHA256SUMS | sha256sum -c -
```

- **结果：**包内 42 个文件校验通过。本地打包脚本同时改为生成 LF 换行，避免新包重复出现问题。

### 第 17 步：初始化私密配置

在解压目录运行过：

```bash
bash scripts/bundle-init.sh
docker compose -f compose.bundle.yaml config --quiet
```

- **作用：**把域名、数据库密码、预设账号信息与代码分开；`config --quiet` 只校验配置，不把展开后的敏感值打印出来。
- **结果：**生成权限为 600 的 `.env`，数据库密码随机生成，原网站用户名填写 `Sherryiing`。
- **关键规则：**初始化时输入的备用网站密码，只用于账号不存在时建号；恢复原库后，原账号密码不会被覆盖。
- **保护：**脚本拒绝覆盖已有 `.env`，不应在部署完成后反复运行。

### 第 18 步：将原 SQL 导入服务器空库

执行过：

```bash
bash scripts/bundle-import.sh /srv/408doing/private/kaoyan_study-20260928-191657.sql
```

- **作用：**先恢复数据，再正式启动后端，保留本地账号和业务内容。
- **保护：**脚本拒绝向非空库导入，也拒绝在后端运行时执行，防止误覆盖。
- **结果：**导入成功，V1–V12 迁移历史成功，任务 30、学习记录 0 与预期一致。

### 第 19 步：签发正式 HTTPS 证书

在 Nginx 首次启动前运行过：

```bash
bash scripts/bundle-cert.sh issue
```

- **作用：**通过 Let's Encrypt 验证域名控制权，给浏览器提供可信的加密连接。
- **流程：**提供联系邮箱、确认服务协议；初次使用 standalone 模式临时占用 80 端口，所以当时要求 Nginx 尚未启动。
- **结果：**`onlystudy.loc.cc` 证书成功签发；当时截图显示到期日为 2026-12-27。这是首次证书的到期日，不是永远固定的有效期。
- **存放：**容器内 `/etc/letsencrypt/live/study/`，通过命名卷持久保存；私钥不得公开。

### 第 20 步：启动服务，检查入口与接口

主要启动命令：

```bash
docker compose -f compose.bundle.yaml up -d --wait --wait-timeout 360
docker compose -f compose.bundle.yaml ps
```

- **作用：**在后台启动容器并等待健康检查；`healthy` 表示配置的检查通过，不代表所有业务路径都正常。
- **首次结果：**MySQL、后端、前端三个服务均健康；HTTPS 首页 200，HTTP 跳转 HTTPS 为 301，公网 `/actuator/health` 返回 404。
- **发现异常：**匿名 `/api/auth/me` 返回正常的未登录状态，但匿名 `/api/today` 返回 500，未达到预期的 401，故没有仅凭“网页能打开”就结束验收。

## 五、排查 500 并保留数据升级

### 第 21 步：收集日志，确认大小写问题

- **做了什么：**读取用户提供的后端日志，并对照原 SQL、Spring Session 配置与 V2 迁移。
- **关键错误：**`Table 'kaoyan_study.SPRING_SESSION' doesn't exist`。
- **原因：**Windows MySQL 导出的表名为 `spring_session`、`spring_session_attributes`；Linux MySQL 的 `lower_case_table_names=0` 区分表名大小写，而程序 SQL 查找大写 `SPRING_SESSION`。
- **作用：**区分“登录会话存储出错”和“任务数据丢失”，避免误删数据库或反复导入。
- **复现：**本地真实备份副本同样返回 500，业务任务数仍为 30。

### 第 22 步：新增 V13 迁移并完成本地回归

- **做了什么：**新增 `V13__normalize_session_table_case.sql`，不修改已经应用的 V2；在需要时原子重命名小写会话表，保留行数据与外键。
- **兼容：**全新 Linux 库及大小写不敏感的数据库不需要改名；大小写表同时存在时失败而不是覆盖或合并。
- **测试：**在 backend 目录运行 `mvnw.cmd -B -Dmaven.repo.local=../.m2repo -o verify`，后端 67 项测试全部通过。
- **真实副本验证：**修复后匿名 401；无 CSRF 登录 403；虚拟账号正常登录、会话读取、今日/任务查询 200；退出 204；退出后恢复 401。数据数量 30/1/15/113/0 保持不变。
- **测试边界：**此轮副本通过本机回环 HTTP 测试，临时覆盖测试 Cookie 配置；生产仍强制 Secure Cookie。前端未修改，本轮没有重跑前端测试。
- **产物：**`release/study-session-fix-20260929.jar`，40,435,538 字节。

### 第 23 步：上传修复文件，先备份再替换

- **做了什么：**修复 jar 上传到 `/srv/408doing/`，再次核对 SHA256 一致。
- **作用：**不直接覆盖运行中的程序；先保留数据库和旧程序，给故障分析及回退留下依据。

停止后端并备份的实际命令：

```bash
cd /srv/408doing/study-20260928-150827 &&
docker compose -f compose.bundle.yaml stop backend &&
bash scripts/bundle-backup.sh /srv/408doing/private/pre-session-fix
```

生成 `/srv/408doing/private/pre-session-fix/study-20260929-001449.sql.gz`。
停止期间网站业务暂时不可用，但没有删除数据。

### 第 24 步：保留旧 jar，安装修复版并验证线上接口

实际执行并成功完成：

```bash
cd /srv/408doing/study-20260928-150827 &&
test ! -e artifacts/app.pre-session-fix.jar &&
cp -p artifacts/app.jar artifacts/app.pre-session-fix.jar &&
install -m 644 /srv/408doing/study-session-fix-20260929.jar artifacts/app.jar &&
docker compose -f compose.bundle.yaml up -d --no-deps --force-recreate --wait --wait-timeout 360 backend &&
docker compose -f compose.bundle.yaml exec -T frontend nginx -t &&
docker compose -f compose.bundle.yaml exec -T frontend nginx -s reload &&
curl -i https://onlystudy.loc.cc/api/today
```

- **命令作用：**先确认旧程序备份名未占用；复制旧 jar；安装新 jar；重建后端以重新绑定程序文件；检查并重载 Nginx，让它重新解析可能变化的后端容器地址。
- **线上结果：**后端约 21.9 秒后 Healthy，Nginx 检查成功，匿名接口返回 `401 UNAUTHORIZED` 和“请先登录”，不再是 500。
- **Cookie：**线上响应带 Secure、HttpOnly、SameSite=Lax 属性。
- **注意：**这是一次性升级记录。备份文件已存在，不要原样反复执行；旧包的 SHA256SUMS 也不再描述更新后的 `app.jar`。

### 第 25 步：确认原账号登录与数据

- **做了什么：**用 `Sherryiing` 登录；最初页面提示“账号或口令不正确”，随后用户记起原网站密码并成功进入。
- **原因区分：**这次是认证信息未匹配，不是刚才的会话表 500；部署时的备用密码没有覆盖导入的旧密码。
- **结果：**没有重设口令，没有删除或重建账号；询问计划数据是否正常后，用户回复“ok”，按人工检查通过继续。
- **证据边界：**精确业务数量来自此前恢复与迁移检查；本次浏览器确认不是新的一次完整 SQL 数据审计。

## 六、续期、每日备份与电脑留底

### 第 26 步：模拟证书续期

执行过：

```bash
cd /srv/408doing/study-20260928-150827 &&
bash scripts/bundle-cert.sh check-renewal
```

- **作用：**用测试环境验证未来的续期流程，包括域名验证路径；不替换正式证书，不停止网站。
- **结果：**输出 `Congratulations, all simulated renewals succeeded`，Nginx 配置检查及 reload 成功。
- **区别：**初次签发使用 standalone；后续续期使用 webroot，由正在运行的 Nginx 提供验证文件。模拟成功不等于已经安装定时任务。

### 第 27 步：手动生成日常备份并检查 cron

执行过：

```bash
cd /srv/408doing/study-20260928-150827 &&
bash scripts/bundle-backup.sh /srv/408doing/private/daily-backups
```

随后检查：

```bash
systemctl is-active cron
crontab -l
```

- **作用：**先验证备份脚本可用，再检查服务器是否具备定时运行条件。
- **结果：**生成 `study-20260929-002555.sql.gz`；cron 为 `active`，最初显示 `no crontab for root`，意思只是 root 尚未配置任务。
- **备份方式：**通过 mysqldump 导出、gzip 压缩，检查压缩文件并计算 SHA256；不是复制运行中的 MySQL 数据文件。

### 第 28 步：编辑并保存定时任务

- **做了什么：**运行 `crontab -e`，用 nano 在文件末尾添加三行；`Ctrl + O`、回车保存，`Ctrl + X` 退出，再用 `crontab -l` 确认。
- **编辑细节：**`#` 开头是注释；每条 cron 命令保持一整行；nano 显示 `<` 是长行横向滚动标记，不代表内容丢失。
- **实际安装内容：**

```cron
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
17 3,15 * * * /usr/bin/flock -n /run/lock/study-renew.lock /bin/bash /srv/408doing/study-20260928-150827/scripts/bundle-cert.sh renew >> /var/log/study-renew.log 2>&1
35 4 * * * /usr/bin/flock -n /run/lock/study-backup.lock /bin/bash /srv/408doing/study-20260928-150827/scripts/bundle-backup.sh /srv/408doing/private/daily-backups >> /var/log/study-backup.log 2>&1
```

- **作用：**每天 03:17、15:17 检查证书，需要时才续期；每天 04:35 导出备份。按服务器时区执行，本次尚未单独核实时区。
- **参数：**cron 前五项依次是分、时、日、月、星期；`PATH` 指定查找命令的路径；`flock -n` 避免同一种任务重叠执行；`>> ... 2>&1` 将正常输出和错误追加到日志。
- **结果：**用户回传的 crontab 内容完整，任务已安装，不需要重启服务器。首次到点自动执行尚未观察，不能等同于已验证定时运行成功。
- **限制：**不会自动删除旧备份；升级版本目录后要同步检查这些绝对路径。

### 第 29 步：下载生产备份到电脑

- **做了什么：**从 Workbench 下载 `/srv/408doing/private/daily-backups/study-20260929-002555.sql.gz`。
- **保存位置：**`D:\Project_JAVA\408doing\个人资料\study-20260929-002555.sql.gz`。
- **作用：**服务器故障、磁盘损坏或实例误删时，服务器外仍保留一份数据副本。只把备份放在服务器另一目录，不算异机备份。
- **限制：**这是一次手动下载，不是每天自动同步到电脑；之后仍要定期下载新副本。

### 第 30 步：校验下载完整性与 Git 忽略状态

- **做了什么：**先在默认下载目录查找，未找到；用户提供“个人资料”位置后，只计算文件哈希并检查 Git 状态，没有读取个人记录内容。
- **结果：**文件 9,821 字节，SHA256 与服务器完全一致；`.gitignore` 中 `/个人资料/` 生效，`git ls-files` 确认该备份未跟踪。
- **作用：**防止下载损坏，以及把账号哈希和个人数据意外提交到仓库。
- **边界：**Git 忽略不是加密，也不能阻止人为强制添加；哈希校验不代替恢复演练。

## 七、遇到的问题与本次处理方式

| 现象 | 原因或判断 | 处理与结果 |
| --- | --- | --- |
| 担心 2 GB 不能运行 | 需要控制内存，不能仅看规格下结论 | 本地编译、容器限额、增加 Swap；实际启动与功能验证通过，未做高负载保证 |
| 不清楚域名和 DNS 的关系 | 名字使用权、解析服务和服务器是不同层 | 注册域名、设置 A 记录，再配置服务器入口 |
| Workbench 上传速度显示 0 | 无足够证据确定具体原因 | 最终上传完成，用哈希确认文件完整 |
| `SHA256SUMS` 报 `$'\r'` | Windows CRLF 换行兼容问题 | 移除清单输入中的回车后校验；后续构建固定 LF |
| 三个服务 Healthy 但接口 500 | 健康检查未覆盖会话表写入 | 查看真实错误日志，新增 V13 并验证迁移副本 |
| `SPRING_SESSION` 不存在 | Windows 导出小写表名，Linux 区分大小写 | 通过新迁移兼容，不清库、不重放 V2 |
| “账号或口令不正确” | 输入的登录信息未匹配，备用密码不覆盖旧密码 | 用户记起原密码，成功登录；没有重置 |
| `no crontab for root` | 尚未配置定时任务 | 编辑保存后用 `crontab -l` 确认 |
| nano 只看到长行尾部 | 横向滚动显示 | 保存退出后通过完整输出核对 |
| 默认下载目录找不到备份 | 实际保存到项目“个人资料” | 按指定位置找到并核验，与服务器一致 |

## 八、重要产物、备份与校验值

下面只记录文件元信息，不记录文件里的个人数据。服务器路径均基于 `/srv/408doing`。

### 1. 原始程序包

- 文件：`study-20260928-150827.tar.gz`。
- 本地位置：项目 `release/`；服务器位置：`/srv/408doing/`。
- SHA256：`28f0bbb43ca22fd3364d0817640f0e5235701373ed296b9df4f44102524dcc51`。

### 2. 原始本地数据库备份

- 文件：`kaoyan_study-20260928-191657.sql`。
- 本地位置：项目 `deploy/backups/`；服务器位置：`/srv/408doing/private/`。
- SHA256：`ff3013df9bc493217c14919cdf304fa01d42061c22b69794814a64daf797faea`。

### 3. 会话修复后端程序

- 文件：`study-session-fix-20260929.jar`。
- 本地位置：项目 `release/`；服务器上传位置：`/srv/408doing/`。
- 当前运行副本：版本目录的 `artifacts/app.jar`；旧程序：`artifacts/app.pre-session-fix.jar`。
- SHA256：`77b69faa2e74f773dde80f79e55ffeda2e024cbcb87ab53ae82ad61ee4933176`。

### 4. 修复前生产备份

- 服务器：`/srv/408doing/private/pre-session-fix/study-20260929-001449.sql.gz`。
- SHA256：`9a749ed0ea802ba5970004758c579ac689c5b037824fd15061031465c1aa03b6`。

### 5. 已下载到电脑的日常生产备份

- 服务器：`/srv/408doing/private/daily-backups/study-20260929-002555.sql.gz`。
- 本地：`D:\Project_JAVA\408doing\个人资料\study-20260929-002555.sql.gz`。
- SHA256：`b0b7af8c65f9ede9fd06c8434b7c8e4feea257148a34caf6a3b7fbf65ffc90be`。

## 九、完成状态与后续维护

### 已完成

- [x] 服务器、域名解析与网站端口准备。
- [x] 程序构建、上传、文件校验、容器部署。
- [x] 本地原始 SQL 的隔离恢复验证与生产导入。
- [x] 会话表大小写问题修复，线上匿名接口从 500 恢复 401。
- [x] 用户使用原账号成功登录，计划数据人工确认。
- [x] HTTPS 正式签发与续期模拟演练。
- [x] 生产库手动压缩备份，续期和备份 cron 已安装。
- [x] 一份生产备份下载到电脑，哈希与 Git 忽略检查通过。

### 尚待验证或安排

- [ ] 检查服务器时区，并观察定时续期与备份首次实际执行的日志。
- [ ] 对新下载的这份生产 `.sql.gz` 做隔离恢复演练，而不只验证哈希。
- [ ] 为 `/var/log/study-renew.log`、`/var/log/study-backup.log` 配置日志轮转。Docker 容器日志已有轮转，但这两个 cron 输出文件是另一回事。
- [ ] 定期检查磁盘与备份数量，制定保留策略；当前不自动清理备份。
- [ ] 持续保留新的异机备份；电脑副本也需要妥善保护，不要分享公开链接。
- [ ] 手机与电脑跨设备写入、生产退出清缓存及离线只读等体验，还需按完整验收清单持续确认，不能只凭登录成功一概宣称通过。
- [ ] 留意 ECS 租期、免费域名规则与证书状态；证书自动续期不会替你续费服务器或保证免费域名永久可用。

网站主体部署已经完成，可以正常使用；这些后续项目属于运维和更完整的验收，不等于网站尚未部署。

## 十、今后使用和排查时记住的事

1. 日常直接访问 https://onlystudy.loc.cc/ ，无需启动本地前后端，电脑关机不影响服务器运行。
2. 正式数据以服务器为准。本地数据库是另一份副本，不会自动与线上双向同步。
3. 密码、`.env`、证书私钥和 SQL 备份不要发到聊天或提交 Git。
4. 不要运行 `docker compose down -v` 删除数据卷；不要向已有数据的库重跑初始化导入。
5. 变更前先备份；回退程序不等于回退数据库，涉及迁移时必须单独评估。
6. 本次使用 `compose.bundle.yaml`，不要与原镜像发布流程混用。

以下是后续可用的只读检查命令，不代表整理本文时又执行了一遍：

```bash
cd /srv/408doing/study-20260928-150827
docker compose -f compose.bundle.yaml ps
docker compose -f compose.bundle.yaml logs --since 10m --tail 100 backend
docker stats --no-stream
free -h
df -h
date '+%F %T %Z %z'
crontab -l
tail -n 50 /var/log/study-renew.log
tail -n 50 /var/log/study-backup.log
```

首次定时运行前日志文件可能还不存在。发送日志给他人前应检查并遮盖敏感内容。

相关文档： [手动部署操作说明](manual-deploy.md)、[逐次部署交接](DEPLOYMENT-HANDOFF.md)、[测试验收记录](../testing/acceptance.md)。
