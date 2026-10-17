# 英语背词与复习进度发布（2026-10-17）

当前阶段：已于2026-10-17 12:47:20 CST正式发布，晚间恢复浏览器控制后读取发布成功标记、备份日志与当前服务状态，并完成线上英语进度刷新及截图验收。禁止重复发布、任务转换或导入。

用户要求将复习和原“每日50”背词加入英语进度。本次增加原数量任务的只读进度统计和展示，保留已经取消固定目标的规则，无表结构迁移、目录导入、任务转换或生产学习记录写入。

## 本地验证与产物

前端104项、类型检查、Vite/PWA构建通过；后端相关26项在真实MySQL8.0.45隔离库通过并打包。本轮没有重跑全套后端或Linux Docker专测，见 [验证记录](../testing/english-progress-20261017.md)。

- 本机包：`release/english-progress-release-20261017.tar.gz`，37107620字节。
- 包SHA256：`48088736c84703cebb95787d947f463f4da361303f5d48a40c86c4b573317bf9`。
- JAR SHA256：`1262155427fd2fe63f951c2f384af70c9384d784d341e0f6474b024ba291f762`。
- index SHA256：`b180db642d437d4662634edbeaab08145e6a2e8cf516b7f77ea5392b3c1b0ad4`。

Chrome文件选择器正常上传到服务器 `/english-progress-release-20261017.tar.gz`，上传成功标记及终端SHA256均已核对，解包到 `/root/english-progress-release-20261017`。不要重复上传或重新转换原任务。

## 实际环境与阶段结果

服务器仍为8.217.241.16，实际部署目录 `/srv/408doing/study-20260928-150827`，沿用compose.bundle.yaml。预检三个服务healthy、可用内存约466MB，Flyway V21，原任务510／511已是COUNT_STUDY／COUNT_REVIEW，发布前JAR与index为上一轮已验证值。

状态目录 `/srv/408doing/private/english-progress-20261017`，隔离恢复库 `english_progress_verify_20261017`，候选容器 `english-progress-verify-20261017`。原先Workbench空闲连接断开，已通过既有免密登录恢复；没有重启主机或更改连接权限。

`python3 release.py prepare > prepare.log 2>&1` 已通过：备份 `/srv/408doing/private/english-progress-20261017/preflight-backup/study-20261017-123632.sql.gz`，SHA256 `2c842685dcb9b25ad5a080c9a1c0c3986a0c8ee0eaee80bd14f219e64b0c2fcb`；在既有MySQL8.4恢复，31张业务表行哈希与7条记录一致，RESTORED已生成。

候选使用独立库与受限账号，320MB内存、0.75CPU、128进程、2数据库连接，不公开端口；测试数量只写隔离库。CANDIDATE-VERIFIED已实际读取，正式发布前进行了最终备份和逐表保护校验。

## 候选验证与暂停接续

首次候选脚本错误地直接读取历史复习JSON中省略的null数量字段，报KeyError；产品统计没有失败，正式数据未变化。旁路resume_candidate.py只修正验收取值方式，保留原release.py及包manifest。恢复前验证正式库仍与基线一致、候选停止、原隔离库记录未改；之后候选验收通过，实际读取到CANDIDATE-VERIFIED及成功输出 `MYSQL84_PROGRESS_COUNTS_EDITS_DELETES_UNKNOWN_HISTORY_NO_DOUBLE_COUNT_ALL_ORIGINAL_ROWS_VERIFIED`。新词任务510为100词／2天／2次，复习511为数量未知／2天／2次，2条历史未知。分次新增、编辑删除、统计回算、复习不混入新学与课程、原业务行保留均通过；候选已停止、测试账号已锁定，正式三个服务当时healthy。

随后执行 `python3 release.py publish > publish.log 2>&1`，用户暂停时尚未读取结果。晚间继续只读取原日志和PUBLISHED，没有重复发布。

最后前端 `pnpm --dir frontend typecheck` 以及retrospective定向20项也已通过；与发布包相比仅TS可选类型和测试fixture变化，无运行时产品代码变化。README／PROJECT_MEMORY与正式验收截图已同步。未提交、推送或合并代码。

## 晚间连接恢复与最终结果

原浏览器标签关闭后，从既有云控制台和免密连接恢复Workbench。曾出现Debugger unattached，用户重新连接扩展后恢复。浏览器ID会随工具会话变化，必须实时核对。较慢的终端调用曾在整批执行时超时，截图确认只读命令仅粘贴后，再单独按Enter执行；没有重复发布或新增生产学习记录，没有重启主机或修改连接权限。

实际读取 `/root/english-progress-release-20261017/publish.log` 和状态目录的PUBLISHED：

- 成功输出 `PUBLISHED_ENGLISH_PROGRESS_ALL_BUSINESS_ROWS_PRESERVED`，时间2026-10-17 12:47:20 CST。
- JAR与index SHA256与本机包一致；Flyway仍为V21，没有新增迁移。
- 发布脚本在停后端写入后备份，替换JAR并强制重建后端，保留旧hash静态资源、更新入口并重载Nginx；31张业务表逐表行哈希一致，原7条学习记录保留。
- 最终备份：`/srv/408doing/private/english-progress-20261017/final-backup/study-20261017-124607.sql.gz`。
- 最终备份SHA256：`5356c96cc82ac0114589d26d9680baab07d22a40e5f17bdb64ecf816d82813c6`，备份日志与发布日志一致。
- 晚间读取docker ps：正式backend／frontend／mysql均healthy，候选不在运行列表中。

正式网站 `/progress/ENGLISH` 在已登录状态下点击“刷新进度”，最后同步更新至2026-10-17 19:28，无离线或接口错误。新词显示累计100词、2天／2次；复习显示数量未记录、2天／2次、2条历史未知，学科累计仍为100词。页面已加载新版卡片，本轮没有出现“立即更新”提示；未在正式库写测试记录。未进行手机视口或本轮断网验收。

截图：`release/english-progress/production-english-progress-20261017.jpg`；终端发布证据：`release/english-progress/publish-result-20261017.jpg`。原JAR与前端恢复副本留在状态目录previous-app.jar和previous-frontend。无新增迁移的代码回退仍需先核对发布后的数据；不得直接恢复旧数据库覆盖后续学习记录。
