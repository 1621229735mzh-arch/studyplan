# 暗紫档案室主题发布（2026-10-18）

后续用户指出本版与确认稿的还原差距，已于 21:30:52 CST 发布[还原校正版](archive-theme-refinement-20261018.md)。下文保留初版发布记录，不能作为校正版的当前产物哈希。

2026-10-18 18:35:32 CST 已在 https://onlystudy.loc.cc/ 正式发布。随后在原 Chrome 登录会话中点击 PWA“立即更新”，正式桌面及手机页面验收通过。只更新前端静态文件，后端、数据库迁移及业务规则沿用原版本。

## 产物与范围

- 来源：`develop` 的 `91afc78` 加本轮未提交的主题修改；本轮未执行 Git 提交、推送或合并。
- 发布包：`release/archive-theme-20261018.tar.gz`，987,184 字节，SHA256 `07c7d747a9703c1585a0e40cb85c1343f72ccad3294617bd58f57faa018bb8fb`。
- 新入口 SHA256：`70d98760a0fc6a39bc2014650826f1c4d2802f83e228a49289d251dc4b0a8cfd`。
- 旧入口 SHA256：`87a26e462fa2f01b8e8e8331526a0b4f3be8084661cc1fb4b281855104a83008`。
- 沿用后端 JAR SHA256：`6d202ae4e118ea5af6f2eb398665f3a7c41f7aeb7238bfe4e40928419e96f369`；Flyway 仍为 `22:1`。
- 本轮前端 14 个测试文件、116 项测试通过，类型检查及生产构建通过；部署复用这份构建，没有改动业务代码或重跑后端测试。设计、素材及本地响应式检查见[主题说明](../development/archive-theme.md)。

## 保护与切换

实际部署目录 `/srv/408doing/study-20260928-150827`、配置 `compose.bundle.yaml` 均已实时核对。直接 SSH 认证失败；原 Workbench 会话断开后，通过既有免密方式恢复连接，未重启服务器。上传至 `/root/archive-theme-20261018.tar.gz`，服务器外部 SHA256 和包内 `SHA256SUMS` 均通过。

发布脚本位于 `/root/archive-theme-20261018/publish.py`，状态目录 `/srv/408doing/private/archive-theme-20261018`。`prepare` 核对三个服务 healthy、V22、旧入口和后端哈希，并确认当前 31 张业务表完整行哈希与上一轮已验证恢复的基线一致（`/srv/408doing/private/completed-problems-20261018/publish-before.json`，恢复凭据为同目录 `RESTORED`）。因此本次前端发布沿用该恢复基线，不新建测试库或候选后端。

准备阶段另保存新数据库备份并完成 gzip 读取与 SHA256 检查，完整旧前端保存在 `previous-frontend/`；发布前业务行校验保存在 `before.json`。`publish` 先执行 Nginx 配置检查，追加新资源，最后原子替换 index 和 Service Worker，保留所有旧 hash 资源。未重建或重启任何服务，未执行数据库写入。

新备份为 `/srv/408doing/private/archive-theme-20261018/preflight-backup/study-20261018-183422.sql.gz`，SHA256 `e9bb7db7e4b6f80e3a05ef048dc95a4c8186116f47f812827a3bbe3ff54edc8f`，准备完成时间为 18:34:35 CST。

发布成功输出 `PUBLISHED_ARCHIVE_THEME_FRONTEND_ONLY_ALL_ROWS_PRESERVED`，保存 `PUBLISHED` 及 `after.json`。31 张业务表完整行哈希与发布前一致，原有 11 条学习记录保持。中断后须先检查标记和线上入口，不重复 prepare 或 publish。

## 公网和真实页面验收

`python release/archive-theme-20261018/public-check.py` 退出 0：全部 40 个静态文件与包内文件逐字节一致（含两张 WebP），匿名 `/api/today` 与 `/api/progress/catalog` 返回 401，`/actuator` 和 `/actuator/health` 返回 404。服务器脚本也核对公网文件、后端哈希、V22 及服务健康。

原登录会话保持，PWA 显示“有新版本可用”后点击“立即更新”。正式 1536×960 桌面页面已显示紫色主题、人物与窗边气球，真实统计仍为已完成 1 项／进行中 0 项／还剩 4 项；数学只读预计剩余保留。390×844 手机页面显示插画标题与底部五个入口，无页面横向溢出。“完成任务”弹窗可见且不要求计时，取消后通过“更多”进入设置，菜单自动关闭，再返回今日；没有保存学习记录或设置。

浏览器验收后再次执行 `python3 publish.py audit`，输出 `LIVE_ARCHIVE_THEME_PUBLIC_FILES_HEALTH_AND_BUSINESS_ROWS_VERIFIED`，全部文件、后端哈希、V22、服务健康与 31 张业务表一致性再次通过。

证据保存在忽略目录 `release/archive-theme-20261018/`：`production-published.png`、`production-audit.png`、`production-desktop.png`、`production-mobile.png`、`production-mobile-dialog.png`、`public-check-result.json`。正式站截图使用真实现有数据，区别于上一轮 `release/theme-preview/` 的本地演示截图。浏览器尺寸覆盖已恢复。

如需回退，仅在核对实时入口和本次状态后，从 `previous-frontend/` 恢复旧入口及原有同名外壳文件；保留后续数据和已发布 hash 资源，不恢复数据库或替换后端。不要删除持久卷。
