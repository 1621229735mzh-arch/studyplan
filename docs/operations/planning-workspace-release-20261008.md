# 计划页发布记录（2026-10-08）

北京时间 21:45:31 手动发布完成，21:49 完成公网与真实浏览器检查。正式入口：https://onlystudy.loc.cc/plan 。默认整体视图，支持月、周、日及外部方案导入；内置 AI 仍封锁，原有学习数据保留。

## 版本与产物

- 发布时使用本地 develop 未提交工作区。后端 92 项、前端 49 项测试和构建及本地浏览器验收已完成，详见 [本地验收记录](../testing/planning-workspace-20261008.md)。发布当天未提交或推送；2026-10-09 按用户要求归档本轮计划页代码与文档到 develop，原部署改动和 stash 保留。
- 生产目录 `/srv/408doing/study-20260928-150827`，沿用 compose.bundle.yaml、原环境配置、固定镜像、证书与持久卷。
- 发布包 `/srv/planning-20261008-211313.tar.gz`，解压至 `/srv/planning-20261008-211313`。归档及包内 SHA256SUMS 校验成功。
- 归档长度 37,294,967 字节，SHA256：`7a1498ea0e96532431f6a79a0c56da9a6a47d1fd062e22c4f808cd806ccd6eab`。
- jar SHA256：`74171a8a3eb67865e9a43b12f7130e7cc86e92a632f8d59ea1182f88c0aeb8ae`。
- 前端入口 SHA256：`2327d01b071208f59c7678c3c1ed73bbf5eaaac35f75d5771015bc0b3034731c`；公网逐字节匹配。
- 验证工具包 `/srv/planning-deploy-tools-20261008.tar.gz`，SHA256：`0e112f083c8098ffa452c2ee8a410252cd040396d1d3b3c1a168aeb00d70d9a7`。

SSH 在认证前关闭，实际经阿里云 Workbench 发布。用户开启 Chrome 扩展“允许访问文件网址”后上传成功。

## 数据保护与升级验收

私有发布目录：`/srv/408doing/private/planning-20261008-211313`。

- 旧版本保留为 previous-app.jar、previous-frontend/、previous-nginx.template。
- 预备备份 study-20261008-212121.sql.gz，gzip 校验成功；SHA256：`46f46f8fc8dc0cb64783df49cdb13357291b9c529a59edb291b9a3275e677bb0`。
- 恢复至生产现有 MySQL 8.4.11 中的独立库 planning_upgrade_20261008，V14 成功，16 张业务表与生产库记录数量一致。没有另开 MySQL 容器。
- 候选 Java 容器限制 384MiB 内存、448MiB 含交换、1 CPU、128 pids，无公开端口。V14 → V15 成功，迁移前后业务表数量一致。
- 初始化环境口令不能登录恢复库中的现有账号，改在隔离库创建临时验收账号，完成 CSRF、登录、JDBC 会话、workspace、今日与退出检查。生产账号未修改。输出 AUTH_CSRF_SESSION_WORKSPACE_TODAY_LOGOUT_VERIFIED 和 MYSQL84_V14_TO_V15_AND_API_VERIFIED。
- 验证后停止 planning-upgrade-20261008 容器，锁定隔离数据库用户，停用隔离应用账号。隔离库保留作证据，其中比原库多一条已停用测试账号，不属于生产数据变更。
- 正式切换前停止后端并再次备份：study-20261008-214450.sql.gz，gzip/hash 成功，SHA256：`5b5c31748a52359efee94869b63742625ecda2eba77acb9aaf183271335d6581`。
- publish-before-counts.json 和 publish-after-counts.json 核对 16 张生产业务表数量一致。

## 发布与线上检查

1. 候选 Nginx 模板经 envsubst 渲染和 nginx -t 校验。
2. 原子替换 artifacts/app.jar，执行 `docker compose -f compose.bundle.yaml up -d --no-deps --force-recreate backend`；健康检查通过，生产 V15 成功。
3. 保留旧 hash assets，逐文件原子更新并校验前端；更新 Nginx 模板并强制重建 frontend，重新渲染配置。
4. nginx -t 成功，实际配置包含导入路径的 5m 请求体和 180s 超时。三个生产容器均 healthy，仅前端映射 80/443，MySQL 未公开映射端口。
5. 发布脚本输出 PUBLISH_DONE BUSINESS_COUNTS_PRESERVED。`python release/planning-public-check.py` 公网检查成功：匿名 workspace 401、无 CSRF 登录请求 403、/actuator 和 /actuator/health 404；HTTPS 入口、PlanView 及入口 JS 均与发布包逐字节一致。
6. 用户原账号登录成功，发布后会话保持。Chrome 经 PWA“立即更新”切换后，整体显示 2026 年 9 月至 2027 年 1 月路线，月/周/日正常，10 月 8 日原有 12 题安排正常。导入窗口、目录加载及今日页面正常，浏览器未报告 error 日志。仅只读检查，未写演示记录或导入演示方案。

服务器私有目录保留 planning_server_verify.py、resume_verify.py、planning_server_publish.py，以及 package-verify.log、candidate-check.log、resume-check.log、publish.log、final-backup-verification.txt、PUBLISHED。初次候选登录失败和恢复脚本换行错误留有日志，后续修正并通过验收。

本机证据在 release/planning-verify-20261008/：production-public-check.log，以及 production-overall/month/week/day/server-check.jpg。旧页面缓存可通过“立即更新”切换。

## 验证边界与回退

生产 MySQL 8.4.11 的恢复、升级、会话与接口检查已执行，不等同于该版本的全量业务测试。Docker 专用 SessionTableCaseMigrationTest 的 2 项仍未在本机执行；本次使用固定镜像挂载已构建产物，没有重建 Docker 镜像。

V15 已应用，不得改写。回退前需停止写入、备份现状并确认新版数据兼容性；不能盲换旧 jar，也不能擅自恢复旧数据库而丢弃发布后产生的数据。
