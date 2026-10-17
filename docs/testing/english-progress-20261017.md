# 英语背词与复习进度（2026-10-17）

用户要求将原“每日50”背词与单词复习加入英语进度。保留现行COUNT_STUDY／COUNT_REVIEW与实际数量规则，不恢复固定每日或全期目标，不改原计划及学习记录。

`GET /api/progress/catalog`新增activities统计：原任务ID、名称、方向、模式、科目与单位、已记录累计量、记录次数、不同学习日期数、历史未知数量次数。统计只读取未删除的原学习记录；复习仅汇总review_amount，新词仅汇总amount。全部历史记录均无数量时累计量为空（沿用全局JSON省略null字段），不返回0；已知与未知混合时显示已记录量并提示未知次数。无记录任务显示0次／0天／0词，没有课程分母或完成百分比。

方向页按任务所属方向展示，四方向首页保持不变。概览统计经原进度快照缓存，离线只读且显示同步时间；兼容没有activities的旧快照，不缓存新增原始记录。

已运行：

- `pnpm --dir frontend test -- tests/unit/retrospective.spec.ts`：20项通过。
- `pnpm --dir frontend test`：12个文件104项通过。
- `pnpm --dir frontend build`：vue-tsc、Vite及PWA构建通过。
- 后端在本机MySQL8.0.45隔离库english_progress_test运行 `mvnw.cmd -o -B -Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo -Dtest=CountedActivityIntegrationTest,CatalogIntegrationTest,ProgressWorkspaceIntegrationTest verify`：26项通过并打包。覆盖分次累计、不同日期去重、修改删除重算、未知历史与混合记录、复习不污染新词／课程量、目录归属原流程。未重跑全套后端及Linux Docker专测。

隔离测试服务已正常关闭。最后补跑 `pnpm --dir frontend typecheck` 与retrospective定向20项也通过。发布包及阶段日志放在ignored release目录。

正式发布于2026-10-17 12:47:20 CST，晚间真实英语方向页点击“刷新进度”同步至19:28，两个卡片分别显示新词100词／2天／2次、复习数量未记录／2天／2次及2条历史未知。学科累计仍100词，没有重复计复习或出现无依据百分比。31张业务表发布前后行哈希一致，原7条记录保留；MySQL8.4候选新增、编辑、删除及未知数量验收通过，正式库未写测试记录。备份与截图见 [发布记录](../operations/english-progress-release-20261017.md)。本轮未做手机视口及断网实测。
