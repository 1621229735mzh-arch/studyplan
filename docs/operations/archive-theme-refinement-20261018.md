# 暗紫主题还原校正发布（2026-10-18）

用户指出[初版](archive-theme-release-20261018.md)与确认稿有明显差距，本轮按原稿校正材质、三栏比例、文字、图标和按钮。暂停前的本地修改已保留，用户要求继续后完成验收，于 **21:30:52 CST** 仅更新前端上线。实际部署路径仍为 `/srv/408doing/study-20260928-150827`，使用 `compose.bundle.yaml`；没有重启服务、改后端或写数据库。

## 产物和验证

- 完整本地包：`release/archive-theme-refine-20261018.tar.gz`，1,155,311 字节，SHA256 `494a5673453e1485ce188b9f10185921acac52028cf04aee8657db3fad60a737`。
- 传输差异包：`release/archive-theme-refine-transfer-20261018.tar.gz`，640,822 字节，SHA256 `728eb6cc4ad96e3b7c4fd426b457c09836d4547c822fb5599caa36296ee2435d`。
- 新入口：`bfac2fae8e120e2a9bda0ddd066dd2a6e318028ef629d673f2a4ae962db0f40a`；旧入口：`70d98760a0fc6a39bc2014650826f1c4d2802f83e228a49289d251dc4b0a8cfd`。
- 后端保持 `6d202ae4e118ea5af6f2eb398665f3a7c41f7aeb7238bfe4e40928419e96f369`，Flyway 仍 `22:1`。
- 前端 14 个测试文件、116 项测试通过；最终生产构建含类型检查通过，PWA 外壳预缓存 41 项（约 2281 KiB）。后端与数据库没有改动，本轮没有重跑后端测试。

新增 `archive-patina.webp` 为 166,540 字节，来自内置 imagegen 按确认稿提取的独立装饰纹理。素材提示词与实现说明见[主题说明](../development/archive-theme.md)。不把设计截图作为真实网页，也没有将演示任务写入生产。

## 上传与保护

Workbench 上传控件长时间等待后报 `Session has expired, please login again.`，没有认定上传成功，也未反复重试同一控件。恢复既有免密终端连接后，通过正常终端输入分 20 段传输差异包；每段 SHA256 和完整差异包 SHA256 均通过。`hydrate.py` 逐文件核对旧包 SHA256 后复用 20 个未变化文件，再通过完整 `SHA256SUMS`，还原的所有运行文件与完整本地包一致。

服务器程序目录 `/root/archive-theme-refine-20261018`，状态目录 `/srv/408doing/private/archive-theme-refine-20261018`。`prepare` 核对健康、V22、后端及旧入口哈希；31 张业务表完整行哈希与上一轮 `archive-theme-20261018/before.json` 一致，沿用其已验证恢复的基线。旧前端保存在 `previous-frontend/`，新备份与 gzip／哈希检查通过，保护结果保存在 `PREPARED` 与 `before.json`。

备份为 `/srv/408doing/private/archive-theme-refine-20261018/preflight-backup/study-20261018-212921.sql.gz`，SHA256 `f54a1d8c65e791b6247a32f7314e236f6637afac1d8b2360cfad01922a7cb611`。准备完成时间为 21:29:34 CST。

`publish` 输出 `PUBLISHED_ARCHIVE_THEME_REFINED_ALL_ROWS_PRESERVED`，保存 `PUBLISHED`、`after.json`；新增资源先写入，最后原子替换 index／Service Worker，保留旧 hash 资源。31 张业务表完整哈希不变，原 11 条学习记录不变。中断后先检查标记，不重复 prepare／publish。

## 公网和页面

`python release/archive-theme-refine-20261018/public-check.py` 退出 0：41 个正式静态文件逐字节匹配本地构建，匿名今日／课程目录接口 401，管理端点 404。服务器同时验证所有公网文件、后端哈希、服务健康及业务行。

原 Chrome 登录会话中刷新正式站，出现 PWA“立即更新”后确认更新。正式页面在 1536×1024 下为约 224／821／481px 三栏，在 2559×1258 下为约 374／1374／801px，没有原先的内容限宽空隙。纹理确实来自正式 HTTPS 的 `archive-patina` 资源，标题、科目图标与按钮已增大；真实统计仍 1／0／4，书名、说明和长标题均保留。

390×844 手机页面无横向溢出，保留底部导航；真实数学完成弹窗可打开并取消，没有保存学习记录或设置。本地另检查 768px 平板、320px 窄屏及日期选择器。浏览器尺寸覆盖已恢复。

页面验收后执行 `python3 publish.py audit` 再次通过，输出 `LIVE_ARCHIVE_THEME_PUBLIC_FILES_HEALTH_AND_BUSINESS_ROWS_VERIFIED`，确认全部公网文件、健康、后端及 31 张业务表保持。

正式截图与公网结果保存在忽略目录 `release/archive-theme-refine-20261018/`：`production-desktop.png`、`production-wide.png`、`production-mobile.png`、`production-published.png`、`production-audit.png`、`public-check-result.json`。截图使用真实现有任务，区分于 `release/theme-preview/` 的本地演示。

发布验收时代码位于 develop 未提交工作区。后续用户要求提交，主题源码、素材、相关前端回归与两轮发布文档纳入独立 Git 交付；并行学习记录安全修复保留在工作区，不属于本次提交或本次生产部署。当前 Git 状态以实际提交记录为准，不合并 main。若需回退，核对实时入口后恢复本状态目录的旧前端外壳文件，保留后续学习数据和 hash 资源，不回滚数据库或替换后端。
