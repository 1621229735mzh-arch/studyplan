# 计划工作区验收（2026-10-08）

本轮在 `develop` 实现默认整体、月、周、日规划及外部 JSON 方案导入。手动按日期安排，周月汇总；今日负责执行和学习记录。以下记录本地验收；随后已于 2026-10-08 发布生产，服务器验证见 [发布记录](../operations/planning-workspace-release-20261008.md)。此前部署相关工作区变更及备份 stash 保留。

## 自动验证

| 验证 | 实际结果 |
| --- | --- |
| 独立 MySQL 8.0.45 空库 Flyway V1–V15 | 成功；再次启动校验迁移 checksum 成功 |
| 后端完整回归（排除 Docker 专用会话迁移测试） | 92 项，失败 0、错误 0、跳过 0；BUILD SUCCESS，可执行 jar 打包成功 |
| 新 PlanWorkspaceIntegrationTest | 20 项；覆盖只读预览、追加保留数据、重复内容/ID/小数写法、过期预览、并发确认、事务回滚、格式/引用/覆盖校验、新增防重、排序/编辑/删除版本及学习量重算 |
| 前端 Vitest | 49 项通过（含计划页 11 项），7 个文件通过 |
| `pnpm run build` | vue-tsc 类型检查及 Vite/PWA 生产构建成功 |
| `git diff --check` | 通过 |

后端在 `backend` 目录执行（仅使用一次性测试库）：

```powershell
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:33309/planning_workspace_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
.\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' '-Dtest=*,!SessionTableCaseMigrationTest' verify
```

空密码仅为绑定 loopback 的临时实例配置，不作为实际部署方式。环境 Java 21.0.6、Maven Wrapper 3.9.16。前端在 `frontend` 执行 `pnpm test` 和 `pnpm run build`。

最终日志保存在被忽略的 `release/planning-verify-20261008/`：`backend-verify-final.log`、`frontend-tests-final.log`、`frontend-build-final.log`。

## 浏览器验收

Headless Microsoft Edge + Playwright，连接独立预览库 `planning_workspace_preview`（127.0.0.1:33309），后端 18089、前端 15173。使用临时测试账号，未连接个人或生产数据。预览运行复制后的 jar，避免 Windows 锁住 target 构建产物。

- 真实登录后默认整体，按月份时间轴展示按单位的计划/实际。
- 外部示例 JSON 经过 Java 校验、可读预览、勾选确认，成功追加；已有迁移生成的日安排仍在。
- 测试库科目叫“数学”，示例叫“数学一”：原文件被正确拒绝，按实际目录替换后成功。这证明目录规则生效，不代表任意 AI 输出都能导入。
- 日安排直接新建、调整数量、上下排序均收到 200；同日已有任务再次添加收到 409，输入和错误留在弹窗；按版本移除成功。
- 1440px 桌面和 390px 手机四视图无整页横向溢出；已目视检查整体、月历、日列表、周纵向列表和导入预览截图。
- 手机月历显示每天几项，点日期查看完整内容，避免任务文字挤在窄格内；日期时间轴月份保持一行。
- 断网后本周日安排及科目名称可读，显示快照同步时间；新增和导入禁用，联网恢复后刷新。
- 浏览器日编辑验收未出现 pageerror。

浏览器脚本/日志与截图同样在忽略目录：`browser-check.cjs` / `browser-check.log`、`browser-edit-check.cjs` / `browser-edit-check.log`、`desktop-overall.png`、`desktop-import-preview.png`、`desktop-day-edited.png`、`mobile-final-*.png`、`mobile-offline-week.png`。

## 本地验收限制与后续发布验证

- 原有 `SessionTableCaseMigrationTest` 的两项 Docker 专用用例未执行；Docker Desktop Linux engine 不可用。普通后端回归通过自备真实 MySQL 完成，未使用 Testcontainers。
- 本地数据库是 MySQL 8.0.45；后续已实时确认生产为 MySQL 8.4.11，在其隔离库验证恢复、V14 → V15 与接口，并在发布前停写备份及核对业务数量。
- 本次 Dockerfile/.dockerignore 已补充导入规范文件复制，两个 Nginx 配置已为导入路径设置 5m request body（原文件限制仍为 2 MB）和 180s 读超时。未重建 Docker 镜像；发布使用固定镜像挂载产物，生产 Nginx 配置检查与运行时已验证。
- 初次恢复时沙箱无法访问 node_modules 或连接本地测试端口；通过批准的执行环境后完成验证。上次 Windows jar 占用导致 repackage 失败的问题已通过复制 jar 运行解决；最终打包成功。
- 当前无内置 AI SDK、调用或生成入口；外部 AI 文件只能经正常 Java 校验与确认事务导入。

使用规范见 [外部 AI 规划书](../business/plan-import-guide.md)、[JSON Schema](../business/plan-import.schema.json)、[示例](../business/plan-import-example.json)。生产验证与备份位置见 [发布记录](../operations/planning-workspace-release-20261008.md)。
