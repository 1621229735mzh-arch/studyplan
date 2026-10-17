# LeetCode 每日自行填写题号（2026-10-17）

## 本轮范围

在 develop 保留已有未提交工作，新增每日自填 LeetCode 题号；不固定31—61、不按序推进。保留每天一个名额与原2026-10-14—2026-11-13日期，完成整题记1，尝试不计量也不计时。同题不重复增加首次题量，记录页可更正题号。

用户确认前三次为31、32、49。现有任务通过“自填题号”预览原地转换，按日期和ID核对原记录；更正调用原学习记录业务服务，保留日期、数量、用时、内容与复盘。任务、工作区、原记录版本变化拒绝操作；任一步失败整体回滚。没有删除或重建原记录。

V22仅为task_sequence增加显式模式；旧有序模式保持默认，不迁移个人题号。原task_sequence_item保存按需创建的身份，原study_record仍是完成事实。新模式的今日快照不包含完整题目目录。

## 实际验证

- 初次 SequenceLearningIntegrationTest：15项通过，真实MySQL8.0.45，本机独立实例127.0.0.1:13320，库leetcode_number_test。空库V1—V22迁移通过。
- 覆盖按原ID将31/32/33更正为31/32/49、完成量仍为3，清单外3000题号、尝试/完成、同日和同题重复拒绝、无题号/非法值/部分题量/日期越界拒绝、令牌重试与内容变化拒绝、删除后重放拒绝、版本冲突、事务回滚、新旧导入兼容。
- 前端第一组49项通过：今日24、记录回看20、题号组件3、转换弹窗2。后续补充记录编辑表单完整提交验证；题号组件4项及计划工作区11项通过。合计5个相关测试文件当前61项通过（重跑的题号组件不重复计算）。
- 最终7个相关后端测试类共75项通过并打包：有序题目15、计划工作区20、今日10、按书接续10、英语实际量8、完成确认5、时间型学习7。未跑全后端或全前端测试。
- vue-tsc与Vite/PWA最终构建通过。构建初次发现ElInputNumber不接受null，修正为undefined后通过。
- Ajv2020校验当前v7完整31天计划、v1/v2历史备份通过；v6拒绝自填模式、v7拒绝预设清单。新计划保留原planId与任务key，不能重复导入已有网站。
- 真实浏览器视觉验收、生产MySQL8.4候选、Linux Docker专测、正式发布及线上历史更正均未执行。

主要命令（完整日志位于被忽略的release/leetcode-number）：

```powershell
pnpm --dir frontend test -- tests/unit/sequenceRecord.spec.ts tests/unit/problemNumberMode.spec.ts tests/unit/todayChecklist.spec.ts tests/unit/retrospective.spec.ts
pnpm --dir frontend test -- tests/unit/sequenceRecord.spec.ts tests/unit/planWorkspace.spec.ts
pnpm --dir frontend build
node release/leetcode-number/validate-schema.cjs
```

后端使用本机Java21及仓库Maven Wrapper，设置TEST_MYSQL_URL指向上述独立库，TEST_MYSQL_USERNAME=root，TEST_MYSQL_PASSWORD为空；使用一次性测试账号配置，不连接正式数据库：

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=SequenceLearningIntegrationTest' test
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=SequenceLearningIntegrationTest,PlanWorkspaceIntegrationTest,TodayChecklistIntegrationTest,CountedActivityIntegrationTest,ContinuedStudyIntegrationTest,ConfirmationActivityIntegrationTest,TimedActivityIntegrationTest' verify
```

## 后续线上应用

本轮未发布、未修改线上数据，不能把测试中的三次记录当作正式数据已更正。
发布前按problem.md核对当前服务器和部署目录、备份并验证恢复，在隔离MySQL8.4候选升级V22并验收。正式发布后读取原LeetCode任务及历史记录的实际ID、日期和版本，按用户指定前三次31/32/49预览转换；不推算日期，不新增替代记录。若历史记录数量或类型不符合三次预期，先核对，不自动覆盖其他记录。
另外通过原计划/目录维护流程核对原阶段目标、导入月周说明及进度课程/节点中的固定31—61旧标题。保留原ID、日安排、31个名额、目录归属及其他科目记录；完成后刷新正式PWA核验新填写入口和记录题号。个人目录文件标题已在本地更新，未导入生产。

收尾：git diff --check通过；已核对数据目录并关闭本轮专用MySQL实例，测试数据文件保留。当前develop未提交/推送，原工作区改动保留。

用户随后授权提交GitHub；提交前补跑前端全量13个测试文件109项通过。上述61项为实现期间的相关测试范围，当前代码同步仍不包含生产部署或线上题号更正。
