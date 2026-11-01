# 外部规划上下文导出发布（2026-11-01）

本次用户明确要求部署。仅交付外部规划上下文导出，不包含并行通用学习记录安全修改或安卓代码，无Git提交／推送／合并。2026-11-01 23:17:18 CST正式发布，仍V24；数据保护、公网及正式PWA桌面／手机尺寸与真实下载验收通过。

## 限定源码与产物

基线为17:29:25 CST已上线的今日逐项复习版本：`release/review-queue-deploy-20261101/source`。仅覆盖15个功能、测试与文档文件，492个范围外源码文件哈希保持。独立源码位于忽略目录`release/context-export-deploy-20261101/source`，`scope.json`保存具体范围。后端62项真实MySQL8.0.45回归及打包、前端19个文件163项测试与类型／Vite／PWA构建通过；工作区此前165项含未纳入的并行测试。

JAR只有15个新增导出相关class条目变化，全部位于原plan模块；原业务class、依赖、配置与V1—V24迁移逐字节保持，无新迁移。前端仅新增导出入口和弹窗，PWA仍44个外壳资源，不扩大个人缓存范围。

| 产物 | SHA256 |
|---|---|
| 差分传输包232798字节 | e0631cd26f326203f570da8a17811b982fdba76489b14354fb124d7ce4db4181 |
| 新JAR | 73cc55e4e1e9c289ed3d93e9e0d94a800d3698d102c5e0c8f8c75594caaf9478 |
| 新index | 8ae0dd5dcbf8ac19aa4c29917f79efc9de1e65e2a317ab29436d0b27885389b4 |
| 原JAR | bd992dd1fd41a38b92a298ebc153f07b0de951ee9dbf184d48b6bc2c545bd868 |
| 原index | 2c3b21e1bb5bd7f5785a90e34aa6acaeb30e88fe50a31341e7233ce3b495073e |

## 已核对环境与恢复点

正式网址`https://onlystudy.loc.cc`，部署目录`/srv/408doing/study-20260928-150827`，`compose.bundle.yaml`。预检确认三个服务健康、V24、25条学习记录、1个复习项和0条复习反馈，旧产物哈希与基线一致。

通过原Workbench文件管理上传差分包到`/root/context-export-transfer.tar.gz`，服务端完整SHA256匹配；在`/root/context-export-release-20261101`重建全部产物，SHA256SUMS通过，程序JAR为644。私有状态位于`/srv/408doing/private/context-export-20261101`。

预检备份gzip与SHA256通过：`133902ae2e9c7fc08f8dabf345ecfa7b1bd72f23b116e6cf268a527abdf82174`。恢复到现有MySQL8.4的独立库`context_export_verify_20261101`，33张业务表逐表哈希与25条原学习记录一致，prepare退出0。没有额外启动生产MySQL实例。

候选使用独立库账号`context_export_1101`、容器`context-export-verify-20261101`，不发布端口，限制256MiB内存／320MiB交换上限／0.75CPU／128进程，Java堆128MiB。先停止正式后端再启动候选，验证后停止候选、锁定测试账号、恢复原正式后端健康并重载Nginx。本机一次性MySQL13334在独立包测试结束后核对数据目录并正常关闭。

## MySQL8.4候选验证

`python3 release.py candidate`退出0，`CONTEXT_CANDIDATE_VERIFIED`及`ORIGINAL_BACKEND_RESTORED_HEALTHY`均确认。恢复数据导出3门课程、255个任务、25条有效学习记录，JSON606775字节、Markdown284444字节。候选使用的真实MySQL8.4迁移版本仍V24，所有迁移校验值保持。

验证匿名导出401、认证后可导出、重复读取指纹稳定、导出学习记录ID与原记录API完全一致、课程完整响应与原目录API完全一致、参考计划与原工作区API完全一致、近期30学习日范围与摘要正确，递归检查无账号密码、会话、提交令牌或请求摘要。导出前后独立库全部业务表保持，候选仅新增1个合成登录账号；未新增学习或复习反馈。正式库全部33张表在候选前后保持。候选无OOM，结束后已停止、测试账号已锁定，原正式后端恢复健康并重载Nginx。

## 正式发布验收

`python3 release.py publish`退出0，23:17:18.187784+08:00写入PUBLISHED。切换前final-backup通过gzip与SHA256校验：`f54f52e04e92f875442b9138d744337336a12cd3889b7362d01e972adee85c04`。强制重建后端使绑定挂载读取新JAR，复制前端时保留旧hash资源，随后Nginx检查／重载通过。Flyway仍V24，原V1—V24校验值保持；33张业务表逐表哈希与切换前一致，25条原学习记录、1个复习项、0条复习反馈及原教材策略保留。

`python3 release.py verify`退出0，确认三个正式服务健康、全部业务表保留、候选停止、独立测试账号锁定。联网执行本地`public-check.py`退出0，44个正式静态文件与限定产物逐字节匹配；导出、今日、课程与复习匿名API均401，`/actuator/env`为404。

正式Chrome沿用已有用户会话，点击PWA“立即更新”，计划页出现“导出规划上下文”。窗口正确显示3门课程、255个任务、25条学习记录、0条复习记录；最近30学习日为2026-10-03至2026-11-01，11个学习日、25次记录、0次完整复习，当前无真实薄弱反馈和OPEN备忘录。没有写入正式演示记录或反馈。

正式Markdown和JSON下载均实际落盘到用户Downloads，并复制至忽略的本轮发布目录保留验收。浏览器自动化的Blob下载事件等待超时，但文件已成功保存；通过文件系统核对实际文件，不将事件超时误判为产品下载失败。JSON664245字节，Markdown284444字节，二者稳定ID、真实摘要、数据版本及导出时点一致。Java时间文本与JSON序列化可能有等价的小数秒尾零差异，按真实时间值核对，不按格式化字符串强求逐字一致。版本指纹为`fb80b4ba7ec0d7647337292219a31dc0107a1f40d85a5d24bac02a64065ade37`，本次导出时点为23:19:47.543573010+08:00；重新打开后导出时刻变化，事实不变，指纹保持。

正式390×844视口中，页面宽380，导出入口占独立一行，两个原操作按钮并排；导出弹窗长标题／时间戳／数据版本均换行，3个下载／刷新按钮可见且可操作，无横向溢出。已恢复默认桌面视口，并将正式计划页导出窗口留给用户。安卓实体设备保存未在本轮验证，不将浏览器手机尺寸验收当作设备验收。

截图及验收文件均位于忽略目录`release/context-export-deploy-20261101`：`production-export-desktop-final.jpg`、`production-export-mobile-entry.jpg`、`production-export-mobile.jpg`、`production-download-result.json`、`public-check-result.json`。个人导出文件不提交源码仓库。

## 回退边界

本轮无新迁移，保持V24。切换失败时恢复本次保存的原V24 JAR／前端，强制重建后端并重载Nginx，不恢复旧数据库覆盖新记录，不降到V23。保留旧hash资源供PWA缓存使用，正式页面需更新后核对。
