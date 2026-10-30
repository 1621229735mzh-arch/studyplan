# 高数内容百分比目录验证（2026-10-30）

用户确认先补齐各内容原百分比展示，再导入高数目录。两本资料分别保留126段原任务；末级百分比从有效学习记录的 continuationDelta 汇总，章节和资料只汇总已完成、进行中、未开始的内容项数。不换算讲数、题数或页数，不生成资料整体完成百分比。

## 隔离构建

源码以 afbd3cb 的Git归档为基线，仅复制 scope.json 中13个源码、测试与导入说明文件。451个范围外跟踪文件与基线逐字节一致。发布JAR只允许progress包和新增V23迁移变化；未包含并行学习记录安全修复或安卓工作区。产物、数据文件、日志与截图位于忽略目录 release/math-catalog-20261030，不提交个人目录或学习记录。

Java21.0.6、Maven Wrapper离线仓库、本机MySQL8.0.45专用临时库 math_catalog_test（仅监听127.0.0.1:13330）执行：

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=ContinuedStudyIntegrationTest,CatalogIntegrationTest' verify
```

ContinuedStudyIntegrationTest 17项、CatalogIntegrationTest 14项，共31项通过，0失败、0错误，后端verify与打包成功。覆盖原百分比历史、幂等导入、无数量或日程写入、删除重算、不合法目录拒绝，以及原数量目录回归。本机临时MySQL已正常关闭；未运行Docker／Testcontainers专测。

前端在同一隔离源码执行：

```powershell
node node_modules/vitest/vitest.mjs run tests/unit/retrospective.spec.ts
node node_modules/vue-tsc/bin/vue-tsc.js --noEmit
node node_modules/vite/bin/vite.js build
```

24项测试、类型检查和Vite/PWA构建通过。最终导入说明更新后再次构建通过。覆盖内容百分比、资料项数、无整体环形图及离线仅概览。

## 目录来源与关联

教材内容沿用用户本地PDF校正后的计划，教材末级附已核实页码。题册沿用原计划内容，不推测缺少原书的题量或页码。章节为第1—18讲／章，两书各18章节和126末级，共288节点、252任务关联。资料均为MATH／高等数学，schemaVersion=2、progressMode=CONTENT_PERCENT，所有totalAmount为null，不导入完成数字。

通过正式网站下载当前任务，以每书unit分组、原任务ID顺序与计划continuation.position逐条比对126个标题一致，原数学目录为空。没有仅凭近似标题猜关联。

## 正式环境验证

备份已恢复到现有MySQL8.4独立库 math_catalog_verify_20261030，31张原业务表、24条原学习记录校验一致。V23仅新增课程统计方式列；保留原列清单进行逐表哈希比较，并显式校验旧课程为QUANTITY，避免新增列造成错误的数据差异。

候选已通过，输出CANDIDATE_CONTENT_CATALOG_VERIFIED：2门资料、36章节、252内容与252原任务关联，originalPercentages、noQuantityConversion、retryAndRepeatImport、recordDeletionRecalculated均为true。候选仅连接恢复副本，不公开端口，限制256MiB内存、320MiB内存加交换、0.75CPU和128进程；为腾出内存，短暂停正式后端，finally恢复原服务并等待健康。原库业务数据保留，候选结束后停止、测试账号锁定。

验证还检查未登录401、缺少CSRF拒绝、重复确认同token幂等、再次预览导入不重复节点、旧数量课程完整响应保持。仅在副本创建合成百分比记录再删除，原业务行保持；未写正式测试学习记录。

正式发布和网站导入结果见发布记录，不将副本验收等同于正式网站导入。
