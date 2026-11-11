# 计划资料归属显示验证（2026-11-11）

用户反馈：数学日／周／月安排只显示“数学”和内容标题，无法直接区分高数、线代、概率及具体资料。

后续状态：2026-11-11 18:38:05已完成限定发布及真实页面验证，见[发布记录](../../operations/releases/math-plan-display-carryover-20261111.md)。下文保留实现时的本地验证范围。

## 修改范围

- 日安排与工作区查询以任务已有的接续关系读取 `study_continuation_stream.title`，通过可空 `bookTitle` 返回；不猜测分科，不写计划、目录、学习记录或复习记录，无新增迁移。
- 日清单与周／月任务卡完整显示资料名，允许换行，具体内容标题保持。手机月历在项数下列出当天全部已关联资料，去重后可点击进入日详情。
- 本周离线快照随原工作区条目保存资料名；旧快照没有字段时仍可读取，重新联网同步后补齐。没有扩展离线范围。

## 实际验证

从 `frontend` 执行：

- `pnpm test -- tests/unit/planWorkspace.spec.ts`：12项通过。覆盖日／周／月同名内容的不同资料名、手机月历资料列表及普通任务不虚构归属。
- `pnpm test`：19个文件、173项通过。
- `pnpm build`：TypeScript检查、Vite及PWA构建成功。

从 `backend` 执行：

- `.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' -DskipTests package`：编译及打包成功；此命令跳过测试。
- `.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=PlanWorkspaceIntegrationTest' test`：21项通过。使用本次新建、仅监听本机13325端口的MySQL 8.0.45独立实例与 `math_plan_labels_test` 测试库，首次完整应用V1—V26。新增场景导入三份不同分科资料的同名任务，验证日安排和日历返回真实资料名、条目数不翻倍、普通任务资料为空、完成量仍为零。原排序、数量、防重、事务回滚、认证与CSRF测试通过。

以上绝对依赖缓存路径只是本机执行记录；其他环境按[本地开发说明](../../development/local-setup.md)配置依赖与一次性测试库。

本轮文件的 `git diff --check` 通过。`node tools/check-doc-links.mjs` 检查509条链接，发现原有5处引用缺失的 `source-handoff-clean-20261103.md`；本轮未新增断链。独立MySQL测试实例已关闭。

本次未提交、推送或部署，没有访问正式学习库。尚未做浏览器视觉验收或线上验收。
