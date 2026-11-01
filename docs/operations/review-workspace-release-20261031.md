# 复习框架发布记录（2026-10-31）

## 发布范围

复习工作区沿用计划的整体／月／周／日布局，今日继续从同一复习项读取对应学科任务，记录日期树追加复习痕迹，进度方向页追加独立复习概览。不录入个人复习内容，不新增迁移，数据库维持 V23，启动命令不变。

复习导入 v1 仅为[规范及示例](../business/review-import-guide.md)，本次没有导入按钮、导入 API、完成来源导出或严格完成来源校验。现有加入入口仍校验学习证据；不能将本次框架发布理解为这些后续能力已经实现。

## 隔离构建

发布目录为忽略目录 `release/review-release-20261030/`。基线 `7731b58c5320375d68339b1c50b76d53cc9738c0`，`scope.json` 限定 26 个复习相关源码／测试文件，451 个范围外文件保持基线。并行通用学习记录安全修复、安卓及其他工作区改动没有纳入发布包。没有提交、推送或合并 Git。

隔离源码上真实 MySQL 8.0.45 的相关后端 45 项测试及 Maven verify 打包通过；前端 17 个文件、148 项测试，vue-tsc、Vite 和 PWA 构建通过。完整工作区早先为 150 项；本次隔离发布以 148 项为准。日志为 `backend-tests.log`、`frontend-tests.log`、`frontend-build.log`。未重跑全后端或本地 Docker／Testcontainers 专测。

| 产物 | SHA256 |
| --- | --- |
| 传输包（871986 字节） | b0be973c4dbf617bd72ac0536b23a021a827c6fa728f0e301fb9e177b2f6eb6a |
| 新 JAR | d60494a16cd844687d1a7eda0ac68985f677fff3e71e60328b068d1c77e4e136 |
| 新 index | 2825f35c93af73584a8c2acadf1bd07ce0a4fe8dbaf8fb0b9701c723ebb7940b |
| 原 JAR | f9226032514c75fc5b654b910aa1348e77e6388c91f539fdda41139075c650d3 |
| 原 index | 38ae4ee35eb9291dc8fcb2f416207236b07ea566d6b7e4f6e52a9b97c0c685cf |

## 服务器准备与断点

沿用 `/srv/408doing/study-20260928-150827/compose.bundle.yaml`，上传包校验、完整产物还原与全部哈希核验通过。远程包在 `/root/review-workspace-release-20261030/`，私有发布状态在 `/srv/408doing/private/review-workspace-20261030/`。

`release.py prepare` 已完成备份、gzip 和 SHA 校验，并恢复至现有 MySQL 8.4 的独立库 `review_workspace_verify_20261030`：31 张业务表及 24 条原学习记录逐表一致。用户随后暂停，正式版本未切换。31 日恢复时只读核对 `RESTORED=true`、其余发布标记不存在，正式三个服务健康、V23、原产物哈希匹配，恢复库仍与准备快照一致；没有重复 prepare。

`release.py candidate` 首次通过，退出码 0。候选限制 256 MiB 内存、320 MiB 内存加交换、0.75 CPU、128 进程且无公开端口，临时停止正式后端以腾出内存，由 finally 恢复原后端及 Nginx，输出 `ORIGINAL_BACKEND_RESTORED_HEALTHY`。只在恢复库测试实际已完成的内容；不写正式演示记录。

候选验证工作区与今日读取同一事实、40% 部分进度与 100% 掌握反馈、归档树复习痕迹、独立统计、过期版本拒绝、相同请求重试、移除日期保留历史。原课程／累计量等响应不变，恢复库原记录和正式 31 张业务表快照保持。

已执行的远程命令（工作目录 `/root/review-workspace-release-20261030/`）：

```bash
python3 release.py prepare </dev/null >/root/review-prepare.log 2>&1
python3 release.py candidate </dev/null >/root/review-candidate.log 2>&1
python3 release.py publish </dev/null >/root/review-publish.log 2>&1
python3 release.py verify </dev/null >/root/review-verify.log 2>&1
```

prepare 在暂停前完成，恢复后未重复；candidate、publish、verify 均退出 0。

## 正式发布及数据保护

正式切换于 **2026-10-31 09:25:24 CST** 完成。最终备份 SHA256 为 `f042641881cccdcae00ef8ad1dc2233b3e9168fead239ae890358a5979eea413`。停后端后保存最终快照及备份，原子替换 JAR、force-recreate 后端，健康后更新前端并保留旧哈希资源，Nginx 配置检查与重载通过；三个服务健康，版本仍 `23:1`。

`PUBLISHED` 确认 31 张业务表、24 条原学习记录完整保留。`VERIFIED` 再次核对业务快照与正式 JAR，候选已停止且测试账号已锁定。没有数据库迁移、正式测试学习／复习记录、第二个 MySQL 或整机重启。

原 JAR 与前端在状态目录的 `previous-app.jar`、`previous-frontend/`；数据库备份在 `preflight-backup/` 与 `final-backup/`。回退前先核对发布后的用户写入与兼容性；不要用旧库覆盖后续记录。接续先看 `PUBLISHED`、`VERIFIED` 标记，不得重复 prepare／candidate／publish。

## 公网验收

本机执行 `python release/review-release-20261030/public-check.py`，首次因沙箱 DNS 限制失败；允许联网后相同脚本通过。44 个静态文件逐字节匹配发布包，入口哈希与表中一致；匿名 `/api/review/workspace`、`/api/today`、`/api/progress/catalog` 返回 401，`/actuator/env` 返回 404。结果在 `public-check-result.json`。

正式 Chrome 已点击 PWA“立即更新”。整体真实显示 0 项复习内容，月／周／日及日历进入当天可用；桌面周历七列，添加安排弹窗没有内容时禁止确认，没有录入演示内容。今日仍显示原0／0／5项，数学方向显示独立复习概览空状态及复习计划入口，记录页原日期树返回10天／24次学习。真实复习痕迹的有数据交互在恢复库候选验证，正式库保持没有复习内容，不以演示记录代替验收。

本轮正式网站手机尺寸验收未完成：浏览器 viewport 工具设置390×844后实际 DOM 仍为桌面宽度，重设及新页面均未生效；访问原生 Chrome 设备工具的批准等待超时，没有完成设备模拟。没有将桌面截图记作手机通过。30日本地390×844响应式检查仍见原测试记录，但不替代本轮正式手机验收。

证据在忽略目录 `release/review-release-20261030/`：`production-candidate.jpg`、`production-published.jpg`、`production-verified.jpg`、`production-review-overall.jpg`、`production-review-week.jpg`、`production-review-progress.jpg`、`production-records.jpg`、`public-check-result.json`。服务器日志为 `/root/review-{prepare,candidate,publish,verify}.log`。全部正式写入仅由正常发布流程完成，没有录入演示学习／复习内容。
