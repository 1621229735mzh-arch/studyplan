# LeetCode 每日多题本地验证（2026-10-19）

## 变更范围

用户取消 `SELECT_BY_NUMBER` 每天最多一题的限制。今日页只填本次完成的题号，支持多题、分次追加及输入内去重；保存后显示当日实际题数并保留追加入口。历史记录编辑仍逐题进行，修改／删除实时重算实际完成量和课程题号。原日期、计划参考总量与09:00学习日不变，实际题量可超过参考目标，固定清单模式仍保持每日一题。

批量接口每题生成一条原学习记录，整批事务保存；重复已完成题、过期修订或日期越界则整批回滚。相同令牌与列表可重试，改动列表长度、顺序、题号或已编辑／删除原记录时拒绝重放。接口保留认证及CSRF，离线只读。无需数据库迁移。

## 工作区验证环境与命令（包含并行修复）

- Windows，Java 21，仓库 Maven Wrapper；后端版本沿用现有锁定配置。
- 真实 MySQL 8.0.45，隔离实例仅监听 `127.0.0.1:13321`，库 `review_learning_test`，Flyway验证22个迁移且未新增迁移；未运行Docker／Testcontainers。
- 测试在保留原有学习记录安全修复的工作区执行，未重置或覆盖这些修改；未连接或修改生产数据库。

从 `backend/` 执行（示例测试库账号仅用于本机隔离实例）：

```powershell
$env:JAVA_HOME='D:\jdk'
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13321/review_learning_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=SequenceLearningIntegrationTest,LearningRecordSafetyIntegrationTest,CountedActivityIntegrationTest,CatalogIntegrationTest,TodayChecklistIntegrationTest,StudyDayIntegrationTest,StudyDayTest' verify
```

结果：75项通过，0失败、0错误、0跳过，`BUILD SUCCESS`。包含原固定清单每日一题、英语实际数量、记录安全及学习日回归。随后补充批量HTTP认证／CSRF校验、今日实际题数与同日课程题号汇总断言，使用同一环境执行：

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=SequenceLearningIntegrationTest,CatalogIntegrationTest' verify
```

补充验证结果：35项通过（序列学习21项、课程目录14项），0失败、0错误、0跳过，`BUILD SUCCESS`。与首轮合计覆盖76个不同测试用例。批量HTTP实际返回201，缺少登录返回401，缺少CSRF返回403，非法列表返回400；当日已完成5题后仍可操作，课程题号按同日有效记录汇总并随修改／删除重算。

从 `frontend/` 执行：

```powershell
pnpm test -- tests/unit/todayChecklist.spec.ts tests/unit/sequenceRecord.spec.ts tests/unit/recordForm.spec.ts
pnpm build
```

结果：47项测试通过；类型检查、Vite构建和PWA生成通过。覆盖多分隔符与去重、非法题号、失败保留输入和令牌、完成后继续填写、补记不回填旧题号、09:00后保留表单日期、离线禁写及原有记录表单回归。构建仍有现有主包体积提示，不影响产物生成。

首次前端验证发现新增测试使用了错误的 `HttpApiError` 构造形式，以及非响应式网络mock无法模拟动态断网；已分别改为对象参数、离线快照入口校验后重跑通过。

## 交付边界

用户随后授权部署。发布采用排除并行修改后的独立源码：62项后端真实MySQL测试、133项前端测试及构建通过；MySQL8.4隔离库完成批量保存／追加／重试／删除验证，2026-10-19 21:16:56 CST正式发布并完成PWA真实页面验收，14条原记录与31张业务表保留。见 [发布验收](../operations/leetcode-multiple-problems-release-20261019.md)。本次提交推送仅纳入每日多题及相关文档，不合并main；其他并行修复保留工作区。测试日志和构建产物保留于被忽略的 `release/`、`target/`、`dist/`，不纳入源码交付。

## 提交源码验证

上述75项加补充35项是保留并行安全修复时的工作区验证历史，LearningRecordSafetyIntegrationTest未纳入本次提交。实际提交源码与已发布的独立源码一致，执行以下后端测试62项通过，前端全部16文件133项测试及构建通过：

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=SequenceLearningIntegrationTest,CatalogIntegrationTest,TodayChecklistIntegrationTest,StudyDayIntegrationTest,StudyDayTest,CountedActivityIntegrationTest' verify
```

Java21、真实MySQL8.0.45测试连接与上文相同；范围与日志见release/leetcode-multi-deploy-20261019/validation-summary.md、backend-scoped-tests.log、frontend-scoped-tests.log和frontend-scoped-build.log（均被忽略，不提交）。
