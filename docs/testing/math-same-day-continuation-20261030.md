# 数学同日继续学习验证（2026-10-30）

## 行为

本书当天列出的内容全部完成后，提供“继续学习下一段”。可在同一学习日连续往后学，两书独立。未开始的可选段不计入今日还剩项数、预计负担；展开不产生记录。下一段部分学习后按原累计百分比保存，次日继续剩余并可带入下一段。原参考日程、教材内容、任务身份和已有记录保持，不重新导入，无数据库迁移。

## 独立源码与构建

发布源码由 `9dc6e40` 的 Git 归档加本次6个源码／测试文件及导入说明组成，位于忽略目录 `release/math-continue-20261030/source`。453个非范围文件逐字节与基线一致。JAR与上次正式产物比对，仅 ContinuationLearningService、其Context和TodayService三个class内容变化；并行学习记录安全修改和安卓工作区未打入发布包。

Java 21.0.6，Maven Wrapper离线依赖，MySQL 8.0.45专用临时库仅监听127.0.0.1:13330。实际命令在隔离源码backend目录执行，环境变量TEST_MYSQL_URL指向math_continue_test，root空口令仅用于该临时实例：

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=ContinuedStudyIntegrationTest,TodayServiceIntegrationTest,StudyDayIntegrationTest' verify
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=TodayChecklistIntegrationTest' test
```

第一条实际运行ContinuedStudyIntegrationTest 14项、StudyDayIntegrationTest 1项（不存在TodayServiceIntegrationTest，未计入通过数），第二条TodayChecklistIntegrationTest 10项；共25项通过，0失败、0错误，后端打包成功。未运行Docker／Testcontainers专测。

前端实际执行隔离目录中已有依赖的 `node node_modules/vitest/vitest.mjs run tests/unit/todayChecklist.spec.ts`、`node node_modules/vue-tsc/bin/vue-tsc.js --noEmit` 和 `node node_modules/vite/bin/vite.js build`：33项测试、类型检查及Vite/PWA构建通过。导入说明更新后重建通过。覆盖自选展开不写记录、保存后继续提供后续段、离线禁止展开和提交，以及原今日学习日和各类任务交互。

## 关键保护

- 当天完成第一段立即提供第二段；可连续完成多段，没有每日一段上限。
- 当前书籍仍有旧内容未完成时，不提前提供额外段；部分学习的次日接续规则保持。
- 提前学习不受后续参考日期限制；参考安排的任务、日期和时长不重排。
- 可选段不影响今日剩余项数或预计负担，实际记录后才进入执行统计。
- 同书revision、事务锁、重复令牌、过期快照拒绝、删除重算与原记录身份保留。
- 保留北京时间09:00学习日和明确补记日期。

日志、产物与作用范围审计保存在 `release/math-continue-20261030/`。正式发布与MySQL8.4验证状态另见发布记录，不把本机测试当作已上线证据。

## 正式环境副本验证

2026-10-30在服务器现有MySQL8.4中恢复正式备份，31张业务表、24条原学习记录逐表哈希一致。候选不公开端口，限制256MiB内存、320MiB内存加交换、0.75CPU和128个进程。恢复后内存余量检查曾拒绝并行启动，随后采用短维护窗口先暂停正式后端，再运行候选；无论候选结果如何，finally均恢复旧后端并等待健康。

初次候选检查将响应中省略的空用时字段当作必有字段，触发KeyError；修正为可空读取后，从原备份新建math_continue_verify_20261030b重测，未沿用先前合成进度。第二次输出CANDIDATE_SAME_DAY_CONTINUATION_VERIFIED：optionalSameDay、multipleChunks、partialAndCompletion、readsDoNotWrite、independentBooks、retryAndStaleProtection、deletionRecalculated全部为true。仅副本写入合成学习记录，原业务行保留，正式库无测试学习记录。

本机临时MySQL已正常关闭。正式网站切换和最终文件、数据核验结果见发布记录。
