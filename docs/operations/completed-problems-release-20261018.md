# 进度详情完成题号展示发布（2026-10-18）

已完成：2026-10-18 11:24:08 CST正式发布，11:26正式PWA立即更新后真实页面验收通过。完成量下方显示31、32、49、128、283，仍5/31题。31张业务表完整哈希及原11条学习记录保持。本次不修改记录或目录，不重复前一轮题号转换，不新增数据库迁移，仍V22。

## 范围与产物

develop现有未提交修改完整保留。当前实现只读汇总已归属课程的自填题号完成记录，返回completedProblemNumbers，课程详情完成量下仅显示数字。尝试排除、去重及数字升序，修改、删除或归属调整后重算，明细不进入离线概览。

本地CatalogIntegrationTest真实MySQL8.0.45共14项、前端retrospective共22项与前后端构建通过，本机测试实例已关闭。详见[验证记录](../testing/leetcode-number-20261017.md)。

- 完整包：release/completed-problems-release-20261018.tar.gz，37125424字节，SHA256 8000610dcae11afadf64ea5ed43e2212bbcd0ae6095f61e188dd071e2f96f400。
- 新JAR：6d202ae4e118ea5af6f2eb398665f3a7c41f7aeb7238bfe4e40928419e96f369。
- 新index：87a26e462fa2f01b8e8e8331526a0b4f3be8084661cc1fb4b281855104a83008。
- 发布前JAR：24ef15ca076bd7db729901d76488d1fae4a176f62e4b66bd51bf5c22c761d16c。
- 发布前index：e580c8d54aeeb10beb88b05318adfef02d0806011b4692d3f4244150844f24fe。

## 上传与保护

Workbench上传控件再次报No node found for given backend id；未调用原生UI旁路。改用现有终端分段传输851560字节差异包，SHA256 1c46c3b4ca9d69008631921441cca38e99f3b8a6c9b3019e4272650505015d58。复用已核验旧JAR的原始ZIP段，完整还原新JAR；本机重建逐字节比较一致，服务器重建SHA256与完整包内清单一致，全部文件通过sha256sum -c。没有修改旧JAR或正式静态资源。

正式部署目录仍/srv/408doing/study-20260928-150827、compose.bundle.yaml。实时核对V22、11条原记录、SELECT_BY_NUMBER及三个服务healthy；可用内存442984KB。服务器新包目录/root/completed-problems-release-20261018，状态目录/srv/408doing/private/completed-problems-20261018，隔离库completed_problems_verify_20261018、候选completed-problems-verify-20261018。

发布阶段prepare、candidate、publish均有标记与哈希保护；恢复工作先查状态，不盲目重复执行。候选复用现有MySQL的隔离库和受限账号，内存256MiB、内存加交换上限320MiB、0.75CPU、不公开端口；要求至少320MiB可用内存。验收后停止候选并锁定测试账号。

## 恢复与候选结果

`python3 release.py prepare`退出0，输出RESTORED_ALL_BUSINESS_ROWS_VERIFIED 31 11。完整31张业务表及11条学习记录从正式库备份并恢复到隔离库后逐表核对一致，仍V22。

`python3 release.py candidate`退出0，输出CANDIDATE_NUMBER_LIST_31_32_49_128_283_VERIFIED，返回numbers=[31,32,49,128,283]、completed=5、total=31。未登录401、缺少CSRF写请求403、登录、只读目录查询及注销均通过；候选除测试账号初始化外业务行保持，正式库完整哈希不变。

恢复验证备份：`/srv/408doing/private/completed-problems-20261018/preflight-backup/study-20261018-111744.sql.gz`，SHA256 `f29c3a0f3b37ffe6a0abd378df83bcb8b5820e8da96f895c76f2804f9887f864`。

## 正式发布与页面验收

`python3 release.py publish`退出0，保存PUBLISHED，时间2026-10-18 11:24:08 CST。后端停止写入后完成最终备份、原子替换JAR并force-recreate，健康检查通过；仍V22且31张业务表逐行哈希不变。保留旧hash前端资源，Nginx检查与重载成功；HTTPS入口与发布包一致，未登录目录API返回401。三个正式容器healthy，未写入正式测试记录。

最终备份：`/srv/408doing/private/completed-problems-20261018/final-backup/study-20261018-112254.sql.gz`，SHA256 `8a09f7435a0461c38c6a8a616fd58c349efdc95c0b67eaa68feeeb3655714a76`。旧JAR及前端保留于状态目录previous-app.jar和previous-frontend。本次无表结构或业务数据变更；回退仍须先核对当前数据和产物，不能覆盖后续学习记录。

真实网站刷新后出现“有新版本可用”，点击“立即更新”。课程详情实际显示数字31、32、49、128、283，完成量5/31题；图示位置无题名、日期或链接。原11条记录与31张业务表保留校验通过。候选已停止，测试账号account_locked=Y。截图：`release/leetcode-number/production-completed-numbers-20261018.png`。本轮未运行全后端或Linux Docker会话专测，未提交、推送或合并Git；既有未提交修改保留。

用户随后要求从中断处继续：重新检查develop代码、git diff和执行状态，原14个已跟踪修改文件及两份未跟踪发布文档均保留，diff --check通过。只读复核线上JAR/index与PUBLISHED哈希一致、三个服务健康、V22、候选停止、测试账号锁定；31张业务表与publish-before.json完整一致，输出RESUMED_AUDIT_PUBLISHED_HEALTHY_ALL_31_TABLES_UNCHANGED。实时刷新课程页仍为31、32、49、128、283和5/31题；没有重复发布或写入记录，本次部署无剩余步骤。
