# Mac Codex 项目长期记忆

更新于 2026-10-09；范围为本次计划页澄清、实现、发布及 Git 归档对话。供 Mac 上继续同一仓库使用。事实以当时验证结果为准，服务器、分支和工作区状态在下次操作前重新核对。

## 协作偏好

- 使用中文，解释直接、具体。梳理设计时一次只问一个问题，结合回答逐步缩小选择。
- 用户认为原有计划内容基本可用，主要不满意页面整体层级与视觉呈现；优先解决信息组织与实际操作路径。
- 多轮回答中的单独 a/b/c 依赖当时选项，不能脱离问题重新推断需求。明确决策见 MAC_DECISIONS.md、PLAN.md 和实际验收文档。
- 用户可能因关电脑或额度不足要求“最快先到一个暂停点”。应尽快保存可恢复状态，写清已完成、未验证、运行进程和下一步，避免再展开大范围工作。
- “继续计划页优化”表示沿既有决策和交接继续，不重新访谈已经确认的内容。
- 本次明确要求在 develop 开发，随后分别授权部署、提交、推送。提交和推送是不同动作；“提交 Git”默认只做本地提交，不能把一次推送授权扩展到后续所有任务。
- 对“仅是记录，不回复”的记忆维护请求，完成文件记录与 Git 提交后保持静默；不把建议自动变成已批准的开发任务。

## 产品与工程入口

- 项目是个人考研学习工作台，单仓库 Vue 前端与 Spring Boot 单体。开始实现前完整阅读 PLAN.md，再读 docs/STRUCTURE.md，并遵守 AGENTS.md。
- 计划页用于制定和调整计划，默认整体；日常执行和学习记录在“今日”。手动按日规划，周/月汇总同一份日安排。
- 外部 AI 在站外讨论方案，通过仓库规划书、Schema、示例和网站实际目录约束 JSON；网站负责校验、预览和确认追加。当前内置 AI 封锁。
- 导入合同唯一来源：docs/business/plan-import-guide.md、plan-import.schema.json、plan-import-example.json。业务规则不要只记在对话里。
- 核心实现：PlanWorkspaceService、PlanImportService、WorkspaceMapper；前端 PlanView.vue、DayScheduleEditor.vue、PlanImportDialog.vue、workspace.ts / workspace.css。
- V15__planning_workspace.sql 已在本地及生产应用，不得改写；后续数据库调整新增迁移。

## 已完成与验证边界

- 计划页代码提交 `3612d15`：`feat(plan): add date-first planning workspace and external imports`，47 个文件。2026-10-09 已推送 origin/develop；当时本地与远端提交差异为 0/0。
- 2026-10-08 本地后端 92 项测试通过并打包；使用独立 MySQL 8.0.45。前端 49 项测试、类型检查与生产构建通过。
- 本地真实浏览器已验证导入确认、日新增/编辑/排序/重复提示/移除、1440px 桌面、390px 手机四视图及本周离线读取。
- 本机 Docker Desktop Linux engine 不可用，SessionTableCaseMigrationTest 的 2 项 Docker 用例未执行。本次未重建 Docker 镜像。不要把现有镜像运行验收称为镜像构建通过。
- 生产 MySQL 8.4.11 已验证隔离恢复、V14 → V15、登录/会话/CSRF/计划/今日/退出；不等同于 MySQL 8.4 全量业务测试。
- 2026-10-08 21:45:31（北京时间）发布至 https://onlystudy.loc.cc/plan ，21:49 完成公网及浏览器检查。原账号会话保留，四视图、导入入口与今日正常。
- 切换前停写备份；升级前后 16 张业务表记录数量一致。只读查看生产页面，未写入演示学习记录或导入演示方案。
- 完整证据见 docs/testing/planning-workspace-20261008.md 和 docs/operations/planning-workspace-release-20261008.md。本地日志及截图在 release/planning-verify-20261008/，不会随本次 Git 提交传到 Mac。

## 部署与跨机器续接

- 当时生产目录为 `/srv/408doing/study-20260928-150827`，使用 compose.bundle.yaml，挂载 artifacts/app.jar 与 artifacts/frontend。保留既有环境配置、证书、数据库卷和 Compose 项目。
- 本次发布包 planning-20261008-211313.tar.gz 的 SHA256：`7a1498ea0e96532431f6a79a0c56da9a6a47d1fd062e22c4f808cd806ccd6eab`。完整产物、备份哈希和回退边界见发布记录。
- 服务器私有备份目录：`/srv/408doing/private/planning-20261008-211313`。最终停写备份为 study-20261008-214450.sql.gz；旧 jar、前端和 Nginx 模板均保留。
- 在 2 GiB 服务器上使用现有 MySQL 的隔离 schema 做恢复与升级，不再额外启动一个 MySQL。候选 Java 限制 384MiB 内存、448MiB 含交换、1 CPU、128 pids，无公开端口。
- 原子替换 bind-mounted jar 后必须 force-recreate backend，使新文件 inode 生效。更新 Nginx 模板要重新渲染；单纯 reload 不会执行 envsubst。本次预检查 nginx -t 后重建 frontend。
- 前端发布保留旧 hash assets，逐文件更新，最后更新入口/PWA 文件。公网核对入口及 JS 字节一致，不能仅以容器 healthy 判断成功。
- 当时匿名 workspace 返回 401、无 CSRF 登录请求 403、/actuator 与 /actuator/health 返回 404；仅前端映射 80/443。
- Windows 工作区是 `D:\Project_JAVA\408doing`。Mac 应使用自己的实际仓库路径和 shell，不复制 PowerShell 或 Windows 绝对路径。Windows Git 所需的命令级 safe.directory 是当地所有权问题的处理，不应无条件写入 Mac 全局配置。

## Git 剩余现场（2026-10-09 快照）

- 计划页提交刻意保留之前的部署改动。工作区当时仍有 5 个修改文件和 11 个未跟踪文件，不等于计划页遗漏提交。
- 修改文件：.gitignore（release/、个人资料/ 忽略）、README.md、docs/STRUCTURE.md、docs/operations/deploy.md、docs/testing/acceptance.md；后四者剩余内容是旧部署说明与历史验收。
- 未跟踪：SessionTableCaseMigrationTest.java、deploy/compose.bundle.yaml、6 个部署脚本，以及 DEPLOYMENT-HANDOFF.md、deployment-journal-2026-09.md、manual-deploy.md。
- stash `preserve deployment edits before develop planning work` 保留。不要按猜测的数字索引 apply/drop；先核对名称和内容。
- README 与 STRUCTURE 曾用部分暂存，仅提交计划页相关内容；提交后仍显示修改是预期结果。禁止用 git add . 把旧部署工作、私有资料或 release 产物一并带入。
- 整理剩余部署工具和文档为独立提交只是助手建议，用户尚未授权执行该整理。旧文档存在历史“尚未部署”等状态，整理时应标明当时状态，避免误报当前系统。

## 踩坑与排查顺序

- SSH 曾在认证前断开，通过用户已登录的阿里云 Workbench 继续。不能因 SSH 不通就改安全组、重置口令或替换密钥。
- Chrome 上传需要 ChatGPT 扩展“允许访问文件网址”。工具禁止访问 chrome://extensions 时交给用户手动开启，不绕过浏览器限制；开启后扩展连接 ID 可能改变，重新发现已连接浏览器和原标签页。
- Workbench 连接可能空闲断开，使用页面“重新连接”。Shell 与 Agent 模式不同，执行命令前确认 Shell。文件选择器不一定支持多文件，先检查 isMultiple。
- 网页终端粘贴多行会出现确认对话框。JavaScript 模板字符串内的 Python `\\n` 容易被提前转义成真实换行，本次曾导致脚本 SyntaxError；优先上传已校验脚本，或明确处理每层转义。
- PRESET_ACCOUNT_PASSWORD 是初始化值，不保证等于现有数据库账号的当前口令。本次自动验证不能登录恢复库，改用隔离库临时账号；生产由用户原账号登录验证，未重置生产凭据。
- 隔离验证结束应停测试容器、锁定测试数据库用户、停用临时应用账号；保留恢复库的额外测试账号要在数量核对中明确说明。
- 更新后浏览器仍显示旧计划页，原因是 PWA 缓存。点击“有新版本可用 → 立即更新”后显示新版；先核对公网文件与缓存，不立即认定部署失败。
- 沙箱 DNS/网络失败与应用故障分别判断；本次公网验证在获准的执行环境重试后通过。自动审批网络断开意味着命令未执行，不代表操作不安全；同一授权动作可按工具规则重试，必须看到 push 成功及同步结果后才报告完成。
