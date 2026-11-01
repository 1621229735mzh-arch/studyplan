# 按教材自动复习验证（2026-11-01）

本文件记录本地实现验证；后续隔离发布源码另通过后端77项、前端155项及构建，MySQL8.4恢复候选和正式V24发布核验已完成，见[发布记录](../operations/automatic-review-release-20261101.md)。没有Git提交／推送。保留工作区原有复习框架、通用学习记录安全修复、安卓等并行改动，后两者未纳入本次发布。业务规则见 [自动复习](../business/automatic-review.md)。

## 环境

- Java 21（本机 `D:/jdk`）、仓库 Maven Wrapper、仓库 `.m2repo` 离线依赖。
- Docker引擎不可用，未运行Testcontainers。使用独立启动的真实 MySQL 8.0.45，仅监听 `127.0.0.1:13332`，数据目录为忽略的 `release/fsrs-review-20261101/mysql-data`，一次性库 `fsrs_review_test`。没有连接正式库；本轮未在MySQL8.4验证。
- 前端使用仓库固定pnpm依赖；未增加运行时包、外部模型或AI SDK。

## 后端命令和结果

以下在 `backend/` 执行，数据库环境变量仅指向一次性测试库：

```powershell
$env:JAVA_HOME='D:/jdk'
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13332/fsrs_review_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
./mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=*Review*Test,FsrsSchedulerTest,ContinuedStudyIntegrationTest,LearningRecordSafetyIntegrationTest,TodayChecklistIntegrationTest,ProgressWorkspaceIntegrationTest,CatalogIntegrationTest' verify
```

**91项通过，0失败／错误，打包通过。** 含自动复习12项、FSRS数值3项、旧复习流程10项、复习工作区6项、接续学习17项、记录安全14项、目录14项、记录进度5项及今日10项。日志：`release/fsrs-review-20261101/backend-verified.log`。

之后补充“教材暂停再启用仍保留手动改期”的处理及断言，执行：

```powershell
./mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=AutoReviewIntegrationTest,ReviewWorkspaceIntegrationTest,ReviewFlowIntegrationTest,FsrsSchedulerTest' verify
```

最终相关**31项复测通过**。这次打包受本轮浏览器预览占用目标JAR影响失败；停止18101临时后端后，没有修改代码或重复已通过测试，执行以下命令成功完成最终Spring Boot打包：

```powershell
./mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' -DskipTests package
```

日志：`backend-final-targeted.log`、`backend-package.log`，均在上述忽略目录。

验证内容：

- V1—V24真实迁移成功；之后24项迁移校验通过。V24不初始化个人规则或完成事实。
- 未启用不触发；40%不建立复习，跨天达到100%使用实际完成日＋1；教材／题册互不启用。
- 已学完整段启用时补纳入，逾期保留原日；反复启用／校准不增加复习记录。
- 首次实际反馈才初始化FSRS；后续以实际复习日计算下一次日期，复习不增加首次学习量。
- 部分40%不推进记忆状态，补足100%只新增60%，重试不重复推进。
- 删除来源后资格失效、暂停待执行日期并保留历史；重新学完复用原身份，日期不早于重新学完次日。
- 教材暂停、单项移除、归档、手动改期、版本冲突和令牌载荷冲突；手动项不被自动转换。
- 未来学习记录当前不具备资格；自动反馈拒绝未来日、学完当日、同日第二次完整反馈及旧版本。
- 启用与学习完成并发时只建立一项；登录、CSRF与请求校验通过。
- 锁定上游 `v5.0.0/fsrs4anki_scheduler.js`，SHA256为 `b97112fac02cf4ef5bb73129a0b63dd7880895af93303e866e0b87f1fb49671a`。通过Node VM独立执行原脚本（仅关闭fuzz），得到5步参考稳定度／难度／间隔序列并逐步核对Java结果；失败安排次日属于本站天级适配。MIT声明纳入仓库。

首轮回归曾发现旧手动模式的空Integer间隔被三元表达式拆箱导致异常，已经修复；另将“每天一次完整反馈”限制仅应用于自动项，保留旧手动流程的原兼容行为。没有通过删减原测试绕过问题。

## 前端

在 `frontend/` 执行：

```powershell
pnpm test
pnpm test -- autoReviewBooks.spec.ts reviewSource.spec.ts todayChecklist.spec.ts
pnpm build
```

全量**18个文件157项通过**；后续相关**42项复测通过**；最终 `vue-tsc`、Vite和PWA生产构建通过。前端构建曾发现新增测试的Element Plus重载返回值类型问题，已修复。日志为 `frontend-all-tests.log`、`frontend-changed-final.log`、`frontend-build-verified.log`。

新增7项覆盖教材分组／小段信息、确认后携带原版本启用、取消不写入、离线、读取失败重试、版本冲突保留状态、失效来源禁恢复及按科目筛选和改期入口。原复习工作区测试保留，补充自动接口的空数据替身，避免单元测试访问真实网络。

## 浏览器检查

本次临时后端 `127.0.0.1:18101`、前端 `127.0.0.1:5181`，只连接上述测试库；使用本地合成测试账号和课程段，没有真实个人数据。

Chrome检查：科目筛选后按教材展开，基础30讲显示真实源段、完成日、轮次、逾期；暂停后开关及日期状态回读成功，小段仍保留；再次启用保留原逾期日期；改期弹窗显示原日期和拟新日期，最后取消。日历／今日复用同一复习项和下一轮事实的功能在集成测试验证，没有把页面截图当成整条流程的唯一证据。

390×844手机宽度检查无横向溢出（窗口390、文档380），控件按窄屏排列。检查后恢复浏览器默认视口，关闭临时标签。截图位于：

- `release/fsrs-review-20261101/preview-desktop.jpg`
- `release/fsrs-review-20261101/preview-mobile.jpg`

验证结束停止本轮临时前端、后端和MySQL，日志及一次性库文件保留在忽略目录。

## 交付与运行边界

启动命令沿用README，新后端启动时Flyway执行V24。复习页按真实教材身份启用一次才开始纳入和自动排期；迁移不会猜测哪个教材是个人基础30讲或默认开启其他教材。周期校准可通过 `app.review.automatic-reconciliation-enabled=false` 关闭。

没有实施复习JSON导入、来源导出、个人FSRS参数训练或分钟级卡片学习步骤。正式V24备份、升级与数据保护核验已单独完成；旧版本后端不理解自动排期状态，不应在自动规则启用后直接换回旧JAR继续写入。
