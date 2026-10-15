# 记录与课程目录进度验收（2026-10-13，第二轮）

## 范围与结果

本轮按用户确认方案完成本地重构：记录按月 → 周 → 日 → 学科／课程当日汇总 → 单次明细展开；进度首页四个方向入口，详情按分科、课程／资料、完整目录组织。目录导入、手工整理、计划候选、任务关联和记录归类均经明确确认，原学习记录决定完成量。业务说明见 [工作区说明](../business/records-progress-workspace.md)。

今日、计划及今日原 RecordForm／validation／测试文件无本轮 Git 差异。没有提交、推送、部署或操作业务库。新增 V16 只在隔离测试和预览数据库执行，V1—V15 未改写。

## 前端

在 `frontend/` 执行：

```powershell
pnpm typecheck
pnpm test
pnpm build
```

- 类型检查与生产构建通过，PWA 生成 36 个预缓存条目。
- 完整测试 **9 个文件、76 项通过**。记录与目录工作区 17 项，保留今日、计划和原表单测试。
- 覆盖月周日默认展开、全部日期和自选日期、慢响应保护、版本冲突与保存失败保留输入、原内容与六位小数精度、防重令牌、纯四方向入口、课程目录及环形图、归类版本、已归类记录更正、目录确认失败保留文本、身份错误不退缓存、离线概览不泄露目录、旧口径阻止展示、快照读写失败提示。
- 最后修正“从计划带入候选时去掉默认空白草稿行”后重新构建通过，并以真实浏览器验证该路径；没有因此重复全部前端测试。

## 后端及数据库

MySQL **8.0.45**，本机 `127.0.0.1:33312`，专用 `catalog_test`；测试环境变量 `TEST_MYSQL_*` 明确指向此库。

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' '-Dtest=CatalogIntegrationTest,ProgressWorkspaceIntegrationTest,PlanLearningFlowIntegrationTest,TodayChecklistIntegrationTest,StudyFlowApiIntegrationTest' test
.\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' '-Dtest=CatalogIntegrationTest' test
.\mvnw.cmd -o -B '-Dmaven.repo.local=../.m2repo' -DskipTests package
```

相关测试共 **44 项最终通过**：核心 API 16 项、旧进度兼容 5 项、计划学习闭环 3 项、今日 7 项，以及目录 13 项。

首次受限执行因本机 socket 权限失败，随后在授权环境连接隔离库执行。相关套件中 31 项通过，目录测试两项因测试单位名称过长失败；缩短测试名称后重跑目录套件通过。补充任务归属解除／恢复测试后，最终目录 13 项 **0 失败、0 错误**。上述命令结果分别保留，不把首次失败说成通过。

新增测试覆盖完整课程分母、未排期目录、历史及后续任务记录自动累计、追加确认／重试防重／保留旧节点、显式单条归属不重复计量、修改删除实时重算、待归类、错误单位、超额单项不能抵掉未学条目、记录及目录版本冲突、任务上下文改变、预览过期、重复归属拒绝、原记录换任务后失效、跨月周分段、缺少用时、单位分离、JSON 类型／重复字段及循环拒绝、叶目录转章节的历史保护、任务归属解除／恢复。

Flyway 从空库执行 V1—V16，通过迁移校验；后端 jar 打包 **BUILD SUCCESS**。未执行全部后端套件、Docker 专用测试或 MySQL 8.4 验证。

## 实际浏览器

使用本机独立 `catalog_preview` 库、一次性测试账号、Playwright + headless Edge。桌面 1440×1000，手机 390×844，时区 Asia/Shanghai。

- 首页仅四个方向，方向 → 分科 → 课程跳转和 URL 详情路径正常。
- 上传 JSON → Java 预览 → 确认目录；课程完整总量为 10，实际量 3.123456，目录显示未学／学习中内容及环形图。
- 计划外记录归类后数量只增加到对应课程，学科累计不增加第二份。
- 手动维护追加新目录项，预览 10 → 12，确认后总量变更而已有完成量保留。
- 从已有计划选择候选 → 整理课程 → 预览确认；历史 1.25 讲自动累计。解除、恢复任务归属能回退和恢复课程量，学科累计不变。
- 记录按月／周／日展开，同课程多次学习合并；本月、本周默认展开，九月收起。修改单次复盘后原完成量保留。
- 手机记录、四方向、课程目录、环形图、补记弹窗和导入错误界面无水平溢出。表单在视口内，正文滚动后提交按钮可见。
- 已加载 SPA 切换离线后只显示课程概览，完整目录、维护及归类禁用，恢复网络重新加载目录。
- 不带 CSRF 的目录写请求返回 403。非法目录返回可读错误并保留输入。完整流程未发现页面 JavaScript 异常。

浏览器脚本首次重跑遇到隐藏下拉选项和同名旧测试记录定位问题；改用可见选项和唯一测试名称后通过。多选菜单选择后先正常关闭菜单再确认，避免菜单遮挡按钮。没有采用强制点击绕过交互。

离线检查针对已加载 SPA 与快照切换，不包括 Service Worker 离线冷启动。后台删除与编辑数量重算由真实 MySQL 测试验证，浏览器本轮主要验证编辑内容、归类与目录追加。

## 截图、日志与收尾

证据位于忽略目录 `release/retrospective-20261012/`：

- `catalog-browser.cjs`、`catalog-visual.cjs`、`catalog-candidates.cjs`。
- `catalog-desktop-home.png`、`catalog-desktop-course.png`、`catalog-desktop-records.png`、`catalog-desktop-edit.png`。
- `catalog-mobile-home.png`、`catalog-mobile-course.png`、`catalog-mobile-offline.png`。
- `catalog-mobile-records-final.png`、`catalog-mobile-form-final.png`、`catalog-mobile-import-error.png`、`catalog-plan-candidate-final.png`。
- `catalog-frontend-tests.log`、`catalog-frontend-build.log`、`catalog-backend-tests.log`、`catalog-backend-retest.log`、`catalog-package.log`。

截图逐张检查后，将手机记录标题与统计改为上下排列；重新检查稳定动画后的截图与弹窗视口、滚动提交能力。

命令行核对进程确属本项目后关闭 Vite 15174、Java 18082、隔离 MySQL 33312，并在授权环境检查 **三个端口无监听**。测试数据库、脚本和日志保留。`git diff --check` 通过，仅有 LearningView 换行规范提示；无提交、推送、生产发布。
