# LeetCode每日多题发布（2026-10-19）

已于 **2026-10-19 21:16:56 CST** 正式发布到 https://onlystudy.loc.cc/，PWA更新后的真实页面验收通过。

## 范围与本地验证

取消SELECT_BY_NUMBER每天最多一题；今日页批量填写本次完成题号，同一天可继续追加，无需填写数量或用时。重复已完成题拦截，批量原子保存、令牌重试、修改／删除重算保留。原计划日期、参考总量和北京时间09:00学习日保留，无新增数据库迁移，仍V22。

以develop的0c9cfcef5ff74ca86cd9ab3f509add9730b84468为基线，独立源码位于忽略目录release/leetcode-multi-deploy-20261019/source，scope.json记录16个限定文件。现有通用学习记录安全修复、精度DTO、RecordForm与TaskPanel修改未纳入本次发布包，工作区保留。新增包含软删除令牌的Mapper查询只供批量重试使用；批量完整重试校验沿用安全校验逻辑，原单条创建的重试路径不改。

实际发布源码在MySQL8.0.45隔离库通过62项后端测试与Maven打包；前端16文件133项测试、类型检查、Vite及PWA构建通过。源码清单外的所有HEAD归档文件逐字节一致。本机未运行Docker／Testcontainers专测。

## 产物与上传

- 完整包：release/leetcode-multi-deploy-20261019/leetcode-multi-release-20261019.tar.gz，SHA256 `5f41f6d61d602cd54d352a113297a1935ec8be2f4b0d0d4495a8780d30a53109`。
- 上传差异包：leetcode-multi-transfer-20261019.tar.gz，840118字节，SHA256 `ab95e94769dbc1f898afa978ca82f607533b733810db599cf537209228440e86`。
- 新JAR：`c435211cb9b281e2ac18c19e05ebc69c9899a5d5fa2b0e0def31fd2b11c65656`。
- 新index：`22f15a0895fc8c2fd74b1c6d46a09fc37236bd4128e903eaf6d92c3dc2a9669e`。
- 发布前JAR：`e303807fa2eb2ed52919696125ec68305a42e77fb071e6b956487f97106df23d`；发布前index：`9cb0bcbc7f0c81beb568c6f931f0645fa0bbec74eaba0ac5f7eead7be20eefdf`。

通过原阿里云Workbench上传。原文件会话过期后刷新并恢复已有免密连接；隐藏input点击未触发文件选择器，改用可见上传按钮后成功。服务器上传包哈希一致，hydrate复用已核验旧JAR的相同ZIP段及18个相同前端文件，完整45项文件通过SHA256SUMS，输出HYDRATED_FULL_PACKAGE_ALL_HASHES_VERIFIED。

正式路径仍为/srv/408doing/study-20260928-150827，compose.bundle.yaml；新候选包目录/root/leetcode-multi-release-20261019，状态目录/srv/408doing/private/leetcode-multi-20261019。

## 备份恢复

prepare输出RESTORED_ALL_BUSINESS_ROWS_VERIFIED 31 14。备份恢复到现有MySQL8.4中的独立库leetcode_multi_verify_20261019，31张业务表和14条原学习记录逐表哈希一致；未额外启动数据库实例。

## 候选验证

候选leetcode-multi-verify-20261019不公开端口，限制256MiB内存、320MiB内存加交换、0.75CPU与128个进程，复用现有MySQL8.4的独立恢复库。candidate输出CANDIDATE_MULTI_PROBLEM_BATCH_APPEND_RETRY_DELETE_VERIFIED：一次保存两题，同日追加一题，重试返回原记录，缩短列表重放拒绝，重复题整批回滚，删除后三题完成量恢复，撤销令牌重放拒绝；登录、CSRF与学习日查询通过。课程仍为原数字31、32、49、128、283和5/31题。

技术验证记录仅写隔离候选库，原记录逐字段校验不变，正式31张业务表快照不变。验证后候选停止、隔离测试账号锁定。
## 正式切换与最终验收

publish停止应用写入并完成最终备份，原子替换JAR后force-recreate后端，健康检查通过；V22与31张业务表逐表哈希一致。前端先写资源再替换入口和Service Worker，保留旧hash资源；Nginx配置检查及重载通过。PUBLISHED时间2026-10-19 21:16:56 CST，三个正式服务healthy，未重启整台服务器、未写正式测试记录。

- 恢复验证备份：`/srv/408doing/private/leetcode-multi-20261019/preflight-backup/study-20261019-211042.sql.gz`，SHA256 `20415f790d3c27e10d2b8dd5088d3689851e10cffa0cd39316990c042f0c9705`。
- 最终备份：`/srv/408doing/private/leetcode-multi-20261019/final-backup/study-20261019-211542.sql.gz`，SHA256 `f9c871a86b571aa95e72381643fbeb02c3db2621a655c86c2324a04b726c86aa`。
- 旧JAR及前端保存在状态目录previous-app.jar与previous-frontend。无需数据库回退，禁止用旧备份覆盖后续新学习记录。

`python release/leetcode-multi-deploy-20261019/public-check.py`验证公网41项静态文件逐字节匹配发布包，匿名今日和课程目录返回401，管理端点/actuator/env返回404。`audit.py`输出LIVE_MULTI_PROBLEMS_ALL_FILES_HEALTH_AND_BUSINESS_ROWS_VERIFIED，FINAL-VERIFIED确认全部文件、V22、三个服务健康、31张业务表和14条原记录保持；候选停止、测试账号锁定。

真实Chrome正式网站点击PWA“立即更新”后，今日页显示“自选题号 · 每天不限题数 · 可分次填写”及“填写完成题号”。打开弹窗为多行完成题号输入框，无数量／用时输入。手选10月18日，已有完成记录显示“今日已完成1题”且“继续填写”仍直接可见，弹窗没有回填旧题；关闭空表单未提交。课程详情仍显示31、32、49、128、283和5/31题。结束后返回19日今日页。

证据位于忽略目录release/leetcode-multi-deploy-20261019：production-multiple-input-20261019.jpg、production-continue-input-20261019.jpg、production-completed-numbers-20261019.jpg及public-check-result.json。本次Git交付纳入每日多题源码、测试与验证发布文档；不合并main，原并行修改保留。接续时先查PUBLISHED和FINAL-VERIFIED，不重复prepare／candidate／publish。
