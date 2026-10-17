# 英语实际数量验证（2026-10-17）

## 已确认行为

用户确认每天完成后自行填写实际背诵和复习词数，同时修改网站功能与两份计划。取消每日50新词和全期6150词的固定目标，不要求计时或掌握评分。新词量计入首次学习，复习量独立保存。未学不提交，不累计欠量。分次学习只填本次数量，同一天由服务端累计；记录页修改、删除后同步重算。

今日入口为“填写数量”，保存后显示当日合计与“已记录”，已完成区可“再记一笔”。原有记录保留，历史仅勾选完成的复习记录保持数量未知。两份计划周期仍为2026-10-15至2027-02-14，各123天。

## 工程与数据

- 新增V21，仅扩展 task_activity 模式并为原 study_record 添加 review_amount；不修改V1–V20，不建立第二份完成事实。
- COUNT_STUDY／COUNT_REVIEW 使用正整数实际数量。对复习任务，学习记录请求的 amount 或 day-progress 的 actualAmount 作为实际复习数量输入，服务端保存为 reviewAmount，amount 保持null；新词仍保存为 amount。列表、任务进度、课程进度与计划首次完成量不会混入复习数量。
- v6导入要求任务计划量、每日计划量与预计用时为空；flexibleMinutes为0或省略。保留旧v1—v5兼容；旧版本拒绝新模式。
- 任务库“按实际量”通过 GET /api/plan/tasks/{id}/quantity-mode 预览影响范围，POST同地址携带任务版本、工作区版本、模式与标题后事务转换。保留任务ID、原日期、历史记录；清除固定日计划量、日估算与任务总量。题目接续、按书接续、复习模块排期及旧周排期拒绝转换。阶段目标及原导入月周说明不自动改写，发布时另行核对两份英语目标的旧文字。
- 写入沿用登录、CSRF、任务行锁、防重与版本检查；删除后的历史版本参与revision，已撤销令牌不能重放。离线仍只读。

## 已执行的验证

2026-10-16的验证日志保存在被忽略的 release/english-quantity/：

- 前端全量12个文件、101项通过，vue-tsc及Vite/PWA生产构建通过。
- 后端全量150项通过并打包；随后新增令牌异常案例，CountedActivityIntegrationTest共7项再次通过。真实数据库为本轮新建MySQL8.0.45实例127.0.0.1:13319，测试库 english_quantity_test，空库应用V1–V21。
- 两份完整个人v6计划分别通过候选服务真实Java预览和确认导入，共246条日安排。候选服务仅监听127.0.0.1:18089，独立库 english_quantity_preview。验证新词37、复习120，重试不重复、修改复习为80、删除后重开、次日独立及不污染新学量。候选空库迁移自带原数学计划，验收按COUNT_STUDY／COUNT_REVIEW定位新增两项，不把初始化数据算作新增英语任务。
- Ajv2020验证两份历史JSON与两份新v6 JSON；新模式改为v5后被拒绝。真实日期与跨字段由Java校验。
- 2026-10-17补充记录页组件回填／修改验证，最终结果见下方收尾记录。

实际命令：

```powershell
pnpm --dir frontend test
pnpm --dir frontend build
```

backend目录，一次性测试库：

```powershell
$env:JAVA_HOME='D:\jdk'
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13319/english_quantity_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
$env:PRESET_ACCOUNT_PASSWORD='test-only-password'
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=*,!SessionTableCaseMigrationTest' verify
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=CountedActivityIntegrationTest' test
```

绝对路径仅为本机证据，其他电脑按环境调整。未在本轮运行2项依赖Linux Docker的会话大小写专测；下述后续发布验证已覆盖MySQL8.4隔离候选和生产升级。

## 收尾记录（2026-10-17）

新增记录页案例验证COUNT_REVIEW的120词独立展示、编辑回填、原任务禁改、修改为80词并携带原版本、历史空用时保留为null。执行 `pnpm --dir frontend test -- tests/unit/retrospective.spec.ts`，该文件18项通过（原17项加1项）；前端全量101项结果仍为上轮结果，本轮只增加此测试，未改动产品代码。`git diff --check`通过，当前分支develop，工作区改动未提交。暂停时与本次收尾均关闭自建测试服务，数据仍保存在被忽略的release/english-quantity/，便于恢复。

## 页面与交付边界

2026-10-17恢复隔离候选服务及本地前端，内置浏览器返回 net::ERR_BLOCKED_BY_CLIENT，未完成真实浏览器视觉及手机尺寸验收；不能把接口和组件测试表述为已完成真实页面验收。

后续已按用户本轮“部署”授权，于2026-10-17 11:53:38 CST正式发布V21，12:03:29完成最终数据校验。实际执行 `python3 release.py prepare`、`candidate`、`publish`、`verify`，以及受事务和范围校验保护的 `python3 update_text.py`。MySQL8.4隔离恢复31张业务表一致，候选升级、任务转换、37／120数量、重试、旧版本拒绝、复习编辑80、删除重算及次日独立均通过。

正式网站通过预览转换510／511，共246条日安排保留日期、清除固定数量；同步更新两项目标、导入标题和46条月周说明。最终31张业务表的非目标行与原7条学习记录一致，没有新增正式测试记录。Chrome已点击PWA“立即更新”，实际检查今日新词和复习两个数量弹窗，无时间／掌握评分输入；原100个新词仍显示，历史复习显示“数量未记录”。桌面真实页面检查已完成，手机尺寸本轮未单独验收。未提交或推送。备份、产物与阶段标记见 [发布记录](../operations/english-quantity-release-20261017.md)。
