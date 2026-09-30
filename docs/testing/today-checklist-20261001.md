# 今日清单验证（2026-10-01）

## 已执行

分支 `develop`。JDK 21.0.11，项目 Maven Wrapper 3.9.16。本机未安装 Docker CLI；使用独立临时 MySQL 9.7.0 实例，仅绑定 127.0.0.1:13316，库 `today_checklist_test`。未连接生产数据库。

首次初始化 MySQL 在沙箱内失败；改为经执行审批的本机进程后初始化成功。Maven/依赖缓存在临时目录，不改变用户已有 Maven 配置。

在 `backend/` 执行（测试库变量预先配置）：

```bash
MAVEN_USER_HOME=/private/tmp/408doing-maven bash mvnw -q \
  -Dmaven.repo.local=/private/tmp/408doing-maven/repository verify
```

结果：72 项后端测试通过，0 失败、0 错误，jar 打包成功。Flyway 在空隔离库完整应用 V1–V14，并在后续启动校验通过。新增 `TodayChecklistIntegrationTest` 的 7 项覆盖：

- 30 → 50 → 100 只写增量，重复比例仅记时长，完成后保留用时。
- 同令牌重试不重复保存。
- 记录编辑与计划调整使旧修订号失效，删除重新计算。
- 两个设备同时提交同一旧快照，只成功一个。
- 同日多个来源只展示一条学习任务，缺失估算明确标注。
- 小计划量的百分比差量不被两位小数舍掉。
- 复习部分完成不提前反馈，100% 才推导日期；历史及用时保留，不增加首次学习量。

时间估算显示精度和代码整理后再次执行：

```bash
MAVEN_USER_HOME=/private/tmp/408doing-maven bash mvnw -q \
  -Dmaven.repo.local=/private/tmp/408doing-maven/repository \
  -Dtest=TodayChecklistIntegrationTest verify
```

结果：新增 7 项再次通过，重新打包成功。其余 65 项的全量通过记录来自本轮先前的 `verify`，未声称其在之后每次整理均重新执行。

在 `frontend/` 执行：

```bash
./node_modules/.bin/vue-tsc --noEmit
./node_modules/.bin/vitest run
./node_modules/.bin/vite build
```

结果：类型检查通过；38 项测试通过；生产构建与 PWA 应用外壳生成成功。新增 7 项界面测试覆盖服务端总计、保存失败保留输入和令牌、完成必须填用时、复习完成必须反馈、部分复习、离线禁止写入、旧快照兼容。

另在独立 `today_checklist_preview` 示例库启动真实后端与 Vite，浏览器验证：

- 已完成英语任务默认折叠。
- 学习 30% → 50%、本次 15 分钟，刷新后得到实际 +15、剩余 -20 分钟。
- 复习完成并反馈掌握，清单转入已完成，实际 +20、剩余 -25 分钟。
- 390px 手机与 1280px 桌面布局；手机无水平溢出，时间字号收紧。

临时日志位于 `/private/tmp/408doing-backend-verify.log`、`/private/tmp/408doing-today-final.log`、`/private/tmp/408doing-frontend-final-build.log`；这些文件不纳入仓库，也不包含生产账号配置。预览使用示例数据，不是线上学习数据。

## 服务器验证与生产发布

2026-10-01（北京时间）已手动发布 `develop` 提交 `2a1644a` 至 https://onlystudy.loc.cc/ 。V13 文件与线上原文件 SHA256 一致，未修改已应用迁移。

服务器使用现有 Linux MySQL 8.4.11，在独立 `today_upgrade_check_r2` 库依次运行旧、新应用：旧应用 V13 成功，新应用 V14 成功，均通过 `wget` 健康检查。测试应用逐个启动，限制内存 384MiB、CPU 1，不发布端口。Python urllib 验证 CSRF、登录、持久化 JDBC 会话、今日接口及四个学科分区，脚本返回 `UPGRADE_AND_SESSION_VERIFIED`。没有执行 Testcontainers，也未在 MySQL 8.4 重跑全套 72 项测试。

生产数据库完整备份另恢复至 `today_recovery_check_r2`，确认 19 张表及 V13 成功；冻结生产后端写入后又做切换时备份。实际发布执行旧后端停止、jar 原子替换、后端强制重建、V14 升级、前端文件逐项 hash 校验、Nginx 配置检查与重载。HTTPS 页面与新入口文件一致，匿名身份接口可读，未登录今日接口为 401，三个生产容器均 healthy，脚本返回 `PUBLISH_DONE`。

浏览器在真实网址通过“立即更新”切换 PWA，确认四科清单、原有两项数学任务、顶部和分区时间汇总、“完成 / 剩余”按钮。只读打开“剩余”弹窗确认累计百分比和本次用时字段后取消，没有写入示例学习数据。线上没有当前复习任务，复习写入交互的验证来自本机示例库及自动测试。

发布详情见 [发布记录](../operations/today-checklist-release-20261001.md)。

参见 [今日清单规则与兼容性](../business/today-checklist.md)。V14 已产生部分复习记录后，旧后端对空掌握反馈不兼容；禁止只替换旧 jar 而沿用新的数据进行盲目回退。
