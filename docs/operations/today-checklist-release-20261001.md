# 今日清单发布记录（2026-10-01）

北京时间约 02:00 手动发布完成，正式网址 https://onlystudy.loc.cc/ 。代码为 `develop` 提交 `2a1644a`，未合并 `main`、未推送；原有学习数据保留。

## 发布产物与部署方式

- 实际目录：`/srv/408doing/study-20260928-150827`，沿用 `compose.bundle.yaml`、环境配置、镜像、证书与卷。
- 发布资料：`/srv/408doing/private/today-checklist-20261001-2a1644a`。
- 上传包：`/srv/408doing/study-today-2a1644a.tar.gz`，37229783 字节，SHA256 `07b1b57a590e3f8207e9ffb8f6769061a371021d20f4cdb4ee076741fbcf3413`。
- 新 jar SHA256：`2afedcd27c74728ae31f5478ed8613ebafb46c570d14d1e1b7ec90be9d2a8472`。
- 新前端入口 SHA256：`af98701016d140fa20d7fe81204109d08b6243e4376602254579edd33e3bbb6d`。

后端绑定 `artifacts/app.jar`，前端绑定 `artifacts/frontend`。替换 jar 后执行 `docker compose -f compose.bundle.yaml up -d --no-deps --force-recreate backend`，确保加载新文件 inode。保留旧 hash 前端资源，原子替换入口文件；执行 `nginx -t` 和 `nginx -s reload`，重新解析后端地址。

## 备份与实际验证

私有发布目录保留 `previous-app.jar`、`previous-frontend/`、`database-before.sql.gz`、`BACKUP-SHA256SUMS`，gzip 及 SHA256 校验通过。完整数据库备份恢复至独立库 `today_recovery_check_r2`，检查 19 张表和 V13 成功。

使用生产现有 MySQL 8.4.11 的隔离库 `today_upgrade_check_r2`，旧应用 V13 → 新应用 V14 验证成功。独立测试账号仅有该测试库权限，凭据留服务器私有目录，不进仓库。应用逐个运行，384MiB 内存、448MiB 含交换、CPU 1、128 pids，无公开端口；结束后测试应用停止。健康检查使用镜像已有 `wget`。Python urllib 验证 CSRF、登录、JDBC 会话及今日四科数据，输出 `UPGRADE_AND_SESSION_VERIFIED`、`LOW_RESOURCE_CHECK_DONE`。这不是 Testcontainers 或 MySQL 8.4 全套业务测试。

正式切换前停止后端，额外生成 `database-at-publish.sql.gz` 与 `FINAL-BACKUP-SHA256SUMS`。包及备份校验后执行发布：

1. 原子替换 jar，强制重建后端，健康检查为 UP，生产 V14 成功。
2. 前端逐文件 hash 校验通过，Nginx 配置检查与重载成功。
3. `curl https://onlystudy.loc.cc/` 内容与新入口一致，匿名 `/api/auth/me` 正常，未登录 `/api/today` 返回 401。
4. 三个生产容器均 healthy，脚本输出 `PUBLISH_DONE`；`DEPLOYED-COMMIT` 记录 `2a1644a`。
5. Chrome 实际线上页面通过 PWA“立即更新”切换成功：四科学习清单、原有两项数学任务、“完成 / 剩余”、时间汇总正常。两项旧任务未填估算，显示待补充。只读查看剩余弹窗并取消，没有写入演示记录。

执行脚本本机源文件为 `/private/tmp/408doing-backup-today.sh`、`/private/tmp/408doing-resume-limited-verify.sh`、`/private/tmp/408doing-publish-today.sh`；通过 Workbench 终端执行。验证日志在私有发布目录 `resume-console.log`。构建及测试命令见 [验证记录](../testing/today-checklist-20261001.md)。

## 发布中断与恢复经验

最初在小规格服务器额外运行未限资源的隔离 MySQL 和旧应用，随后 Workbench/VNC 登录无法执行，云助手未运行，监控报告系统盘带宽达到规格上限。用户明确允许正常重启并完成安全验证后，正常重启恢复；没有强制重启。停止最初测试容器后生产恢复健康。

重启后确认测试网络 `172.20.0.0/16` 与 VPC `172.19.128.0/20` 不重叠，排除先前猜测的网段冲突。未发现内核 OOM kill，资源/IO 压力是可能原因，未证实唯一根因。后续改用现有 MySQL 的隔离 schema，避免再启动第二个数据库实例。第一轮受限检查因镜像没有 curl 错误判定启动失败，修正为 wget 后成功。

初始 `today-upgrade-mysql`、`today-upgrade-old` 及受限测试应用已停止，均不应重启。保留测试库与恢复资料供排查；不公开数据库、测试应用或管理端点。

## 回退边界

V14 产生部分复习记录后，旧后端无法理解空掌握反馈。禁止仅替换旧 jar 并继续使用新版产生的数据。回退必须先停止写入、备份现状，决定恢复切换时数据库还是迁移新数据，再同步恢复兼容的后端/前端；恢复旧数据库会丢失切换后的记录，需要明确用户授权。`main` 可追溯代码，但不是数据库回退方案。
