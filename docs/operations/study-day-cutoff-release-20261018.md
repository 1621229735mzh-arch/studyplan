# 学习日次日早上9点截止发布（2026-10-18）

已于 **2026-10-18 23:30:10 CST** 发布到正式网站 `https://onlystudy.loc.cc/`。用户暂停时发布命令已发出；2026-10-19继续后先检查PUBLISHED、日志、产物及健康，确认命令已经成功完成，没有重复发布。19日约20:00完成PWA、真实页面和最终数据保留验收。

## 范围与验证

北京时间每天09:00切换学习日，09:00整点起归新一天。19日06:00默认归18日；今日、学习／复习默认日期、计划默认日期、回看和统计期间使用同一规则。手选补记日期不再偏移，历史记录不批量改写，真实时间戳与预览过期不变，无新增迁移，仍V22。

发布源码以develop的`4fbea505ea13059c96d34d7b32f85cf0e6a55174`为基线，只叠加本次截止规则。独立源码位于忽略目录`release/study-day-deploy-20261018/source`，`scope.json`记录清单；原工作区学习记录安全修复、Mapper／精度DTO及TaskPanel修改未进入发布包，也未被覆盖。用户随后要求提交推送，Git交付范围限定为截止规则及验证发布文档，安全修复保留工作区；提交和远程状态以实际Git核对为准。

独立发布源码的前端16文件130项测试、类型检查及PWA构建通过；后端44项真实MySQL8.0.45相关测试及打包通过，使用全新隔离库study_day_release_test，V1–V22迁移成功。不是工作区包含安全修复时的132项前端结果。实际命令与本地边界见[验证记录](../testing/study-day-cutoff-20261018.md)，发布构建日志在忽略目录`release/frontend-scoped-tests.log`、`release/frontend-scoped-build.log`和`release/study-day-deploy-20261018/backend-scoped-tests.log`。

## 产物与上传

- 完整包：`release/study-day-deploy-20261018/study-day-release-20261018.tar.gz`，37,789,568字节，SHA256 `1aea4e6033f51d729b42c73ec163f6d9d8e7aa650530641ef745c74b0a4132c5`。
- 上传差异包：`study-day-transfer-20261018.tar.gz`，836,526字节，SHA256 `5d87e812cf1001374f9567fa7618da4c4d14ef5f5996ac815a2eeb35c0d47cf0`。
- 新JAR：`e303807fa2eb2ed52919696125ec68305a42e77fb071e6b956487f97106df23d`。
- 新index：`9cb0bcbc7f0c81beb568c6f931f0645fa0bbec74eaba0ac5f7eead7be20eefdf`。
- 发布前JAR：`6d202ae4e118ea5af6f2eb398665f3a7c41f7aeb7238bfe4e40928419e96f369`；发布前index：`bfac2fae8e120e2a9bda0ddd066dd2a6e318028ef629d673f2a4ae962db0f40a`。

通过现有阿里云Workbench正式上传控件成功上传；服务器先验证差异包SHA256，再复用已核验旧JAR的相同ZIP段与19个未变化静态文件，恢复完整包。全部文件通过SHA256SUMS，输出HYDRATED_FULL_PACKAGE_ALL_HASHES_VERIFIED。服务器候选目录`/root/study-day-release-20261018`，正式部署仍`/srv/408doing/study-20260928-150827`、compose.bundle.yaml。

## 备份恢复和候选

状态目录`/srv/408doing/private/study-day-20261018`。prepare保留previous-app.jar和previous-frontend，备份并恢复到现有正式MySQL8.4中的隔离库study_day_verify_20261018；31张业务表及原11条记录逐表哈希一致。没有额外启动MySQL实例。

- 恢复验证备份：`preflight-backup/study-20261018-231909.sql.gz`，SHA256 `fa551d9f83904753f6d89b1558c51bbdf89b2255c22272f70a65d0130d31f94c`。
- 切换前最终备份：`final-backup/study-20261018-232855.sql.gz`，SHA256 `42b5fff66e995b50a559f2f5c4a6ebb6fe45ee72c7b08ec6e8e00bd432558795`。

候选study-day-verify-20261018使用256MiB内存、320MiB内存加交换上限、0.75CPU，不公开端口。首次应用已健康启动，但在其中追加启动日期探针的docker步骤失败；日志仅确认该步骤退出1，未把原因猜测为已证实故障。探针改在独立128MiB受限、无网络容器执行后，实际发布JAR中提取的StudyDay.class通过8个精确边界场景。使用原隔离库与既有候选恢复验证，没有重复prepare或恢复覆盖。

候选确认匿名401、缺CSRF写请求403、登录／注销、默认今日／统计／复习日期及显式历史日期正确。课程完成题号仍31、32、49、128、283和5/31题。候选不提交学习测试记录；除候选测试账号初始化外，原业务数据保持。CANDIDATE-VERIFIED记录8个边界、JAR哈希及接口结果。结束后候选停止，受限测试数据库账号锁定。

## 正式切换和恢复验收

publish先停止应用写入并完成最终备份，原子替换JAR、force-recreate后端，健康后检查V22及全部业务行保持。前端先写资源再替换index／Service Worker，保留旧hash资源；Nginx检查和重载通过。PUBLISHED保存23:30:10发布结果，三个服务healthy，未写正式测试记录。

19日继续时线上产物仍与包一致。`python release/study-day-deploy-20261018/public-check.py`验证41个正式静态文件逐字节匹配；匿名今日／课程目录401，管理端点404。原浏览器登录已过期，由用户登录；点击PWA“立即更新”后，真实页面显示“学习日2026-10-19 · 次日早上9点截止（北京时间）”，今日真实统计0／0／5。补记默认19日，手选18日后仍显示18日，随后刷新丢弃空表单，未保存测试记录。

最初恢复审计直接要求当前全表快照与昨晚一致，因study_record已从11增至14而停止。只读诊断确认仅该表有3条发布后新增记录（ID12–14），其余30张业务表完全一致。修正审计为保留新数据、逐字段核对原记录：原ID1–11完整行哈希与发布基线一致；3条新增记录的创建时间均晚于发布。ID14于19日00:02:55创建，record_date为18日，符合凌晨归前一天的规则。未回滚、重写或清理这些学习记录。

最终audit.py输出LIVE_STUDY_DAY_9AM_ALL_FILES_HEALTH_AND_31_BUSINESS_TABLES_VERIFIED，保存FINAL-VERIFIED：41项静态文件、V22、三个服务健康、原11条记录及新增3条保留，其余30张业务表不变；候选停止且测试账号锁定。本轮没有重复发布或重启。

证据位于忽略目录`release/study-day-deploy-20261018/`：production-today-20261019.jpg、production-audit-20261019.jpg、public-check-result.json。没有重跑全套后端、独立Docker会话迁移专测；生产运行、MySQL8.4恢复、候选与真实页面已验证。

恢复工作先检查PUBLISHED和FINAL-VERIFIED，不重复prepare／candidate／publish。回退前需核对实时产物和记录，旧产物保存在状态目录；本次无需数据库回退，禁止用旧备份覆盖发布后的学习记录。
