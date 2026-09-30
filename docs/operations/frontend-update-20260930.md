# 2026-09-30 今日页面前端更新

## 变更与部署方式

今日页面删除顶部标题、说明文字以及预算/额度缺失提示框，保留右侧日期选择和刷新操作。今日概览、计划任务、尚未记录条目、复习任务和快速记录保持原样。

远程网站为 `https://onlystudy.loc.cc/`。根据用户提供的首次部署记录及此次 Workbench 终端核对，部署目录为 `/srv/408doing/study-20260928-150827`，使用 `compose.bundle.yaml`。前端将 `artifacts/frontend` 挂载到 Nginx 的 `/usr/share/nginx/html`。此仓库副本没有该 bundle 配置及脚本，不能直接用仓库的 `deploy/compose.yaml` 替换现有服务器配置。

此次仅上传并替换前端静态文件，没有替换后端 jar、运行迁移或改动数据库、账号、证书、定时任务及容器配置。更新时保留旧的带哈希资源文件，以支持尚未更新缓存的浏览器。入口文件通过同目录临时文件及 `mv` 替换，发布检查失败时恢复旧入口文件；没有重新创建容器。

## 发布包与备份

- 上传包：`/srv/408doing/study-frontend-today-20260930.tar.gz`，771510 字节，含 34 个静态文件及 `SHA256SUMS`。
- 包 SHA256：`cd2ea8909bc9b7db71993ec447f4a512df39734e3e8b4134c0060b0b42776d51`。
- 校验、解压与旧版备份目录：`/srv/408doing/private/frontend-update-20260930`。
- 完整旧前端备份：上述目录下的 `previous-frontend/`。
- 旧入口 HTML SHA256：`73a1d02e0113319c544d232b2b5cf49e8a7329a4d556d8467371fc230f495d3c`。
- 新入口 HTML SHA256：`4d2f328fb78fc4fece76d79705ce711c9216241fd2ca9d68992811cf1f6a5a3b`。

## 实际执行的验证

本地在 `frontend/` 执行：

```bash
./node_modules/.bin/vue-tsc --noEmit
./node_modules/.bin/vite build
./node_modules/.bin/vitest run
```

类型检查和生产构建通过，5 个测试文件、31 项测试通过。此次未修改后端，未重跑后端或数据库测试。还在隔离临时目录用改动前的前端代码重新构建，其主脚本与线上旧主脚本逐字节一致。

服务器通过 Workbench 实际执行并确认：

```bash
# 在现有部署目录执行；不输出展开的敏感配置
docker compose -f compose.bundle.yaml ps
docker compose -f compose.bundle.yaml exec -T frontend nginx -t < /dev/null

# 在解压目录执行
sha256sum --quiet -c SHA256SUMS
```

上传包、旧入口和包内 34 个文件全部校验成功；替换后的全部新文件再次按清单校验成功。通过 `curl -fsS --max-time 30 https://onlystudy.loc.cc/` 下载的 HTTPS 入口与新入口经 `cmp` 核对一致，三个容器仍为 healthy。命令在此处文档化，不代表重新执行部署。

浏览器初次刷新继续显示旧缓存，并出现“有新版本可用”。点击“立即更新”后，登录状态正常、今日页面读取正常，指定部分已移除，其余概览及任务内容保持原样。

## 如需回退本次页面修改

以下命令仅在明确决定回退时执行，本次发布没有执行回退。先确认备份仍然存在，路径仍指向该部署。

```bash
cd /srv/408doing/study-20260928-150827
bash <<'ROLLBACK_FRONTEND'
set -euo pipefail
backup_front=/srv/408doing/private/frontend-update-20260930/previous-frontend
live_front=artifacts/frontend
test -s "$backup_front/index.html"
cp -a "$backup_front/assets/." "$live_front/assets/"
for name in icon.svg manifest.webmanifest workbox-2fbc6a65.js index.html sw.js; do
  install -m 644 "$backup_front/$name" "$live_front/.$name.rollback"
  mv -f "$live_front/.$name.rollback" "$live_front/$name"
done
cmp "$backup_front/index.html" "$live_front/index.html"
docker compose -f compose.bundle.yaml exec -T frontend nginx -t < /dev/null
ROLLBACK_FRONTEND
```

回退后重新检查网站，在浏览器的版本提示中更新缓存。不要删除数据卷、重新导入数据库或用旧后端 jar 覆盖当前后端。

## Git 状态与两台电脑同步

此次操作时 `main` 与 `develop` 都指向 `e198f54`；页面改动及本文保存在 `develop` 工作区，尚未提交或推送。部署静态文件不会自动更新 Git。以后从另一台电脑发布前，应先提交并同步这次代码，避免用旧工作区重新构建而覆盖线上页面。
