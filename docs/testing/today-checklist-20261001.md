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

## 生产发布与未验证项

本次只完成代码与本地验证，未发布新版今日清单。线上只读读取 V13 迁移，仓库恢复文件与线上应用 SHA256 一致。

本次没有执行 Testcontainers / MySQL 8.4，且本机数据库表名不区分大小写。生产为 Linux MySQL：发布前需要隔离验证实际 V13 库升级 V14，以及现有 session 表与应用启动。不能把本机 9.7 的通过当成生产 8.4 的验证结论。

参见 [今日清单规则与兼容性](../business/today-checklist.md)。V14 已产生部分复习记录后，旧后端对空掌握反馈不兼容；禁止只替换旧 jar 而沿用新的数据进行盲目回退。
