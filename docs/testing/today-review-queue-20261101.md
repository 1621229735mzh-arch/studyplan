# 今日逐项复习验证（2026-11-01）

本文件记录首次V24上线后的后续实现验证。用户随后要求部署，已于17:29:25 CST完成独立包正式发布，33张业务表和25条学习记录保留，未写入正式测试记录；见[发布记录](../operations/today-review-queue-release-20261101.md)。无Git提交／推送，原并行学习记录安全修改和安卓代码保留且未纳入发布。规则见[自动复习](../business/automatic-review.md)。

## 实现边界

已启用教材在今日对应学科中提供一个当前项；当天部分复习优先，其余先逾期再当日到期，按相对逾期程度、上次反馈、到期日和ID排序。完成后下一项为自选，点击只展开，不写事实；候选在开始前不计入今日剩余。保留原到期日，实际今日反馈可处理逾期，FSRS只在完整反馈推进。手动项仍要求原日期；暂停、归档、停用、来源失效和未来项不会被推荐。历史回看不自动带入今日积压。

用户后续明确选择高等数学／操作系统等分科：复习概览仅在分科内展示。后端依据任务课程关联／来源记录已确认归属／唯一来源分科生成branchReviews；多分科不明确的来源集中到待归类，原subject统计保留兼容。新增真实MySQL场景验证同科目高数／线代分离、来源不明不重复、计划外记录跟随确认归属及同日多项复习天数去重。分科汇总进入原只读概览，旧快照提示同步。

## 已执行验证

Java21、固定Maven Wrapper和本地离线依赖。隔离MySQL8.0.45仅监听127.0.0.1:13332，复用忽略目录下的一次性fsrs_review_test库，不连接正式库。Docker不可用，本轮未运行Testcontainers或MySQL8.4验证。

首次数据库在受限环境启动，连接测试被网络权限阻止；允许测试连接后，该实例仍无法接受外部进程连接。严格核对进程和测试目录后停止无业务写入的实例，在允许连接的本机环境启动同一实例，确认SELECT VERSION()返回8.0.45，再运行下列最终命令：

```powershell
# backend，环境变量仅指向上述一次性库
$env:JAVA_HOME='D:/jdk'
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13332/fsrs_review_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
./mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=AutoReviewIntegrationTest,TodayChecklistIntegrationTest,ReviewFlowIntegrationTest,FsrsSchedulerTest,CatalogIntegrationTest,ReviewWorkspaceIntegrationTest' verify

# frontend
pnpm exec vitest run
pnpm build
```

后端38项通过，0失败／错误，Spring Boot打包成功。新增真实MySQL测试覆盖不改期的推荐、逾期优先与相对逾期排序、部分进度防重、实际今日完整反馈、下一项optional标志、无效来源／暂停排除及旧手动日期限制。原今日10项、复习10项和FSRS数值3项通过。

用户明确分科位置后，加入目录15项和复习工作区6项，最终**59项全部通过，0失败／错误，打包成功**；新增分科用例覆盖同科目多个分科不重复、来源不明的待归类、计划外已确认归属和同日复习天数去重。最终后端日志为today-queue-branch-verified.log。

前端18个文件160项全部通过，包含逐项展示、继续展开不提交、正确任务ID／版本／日期、部分复习及离线禁写、所属学科筛选与页面顺序。vue-tsc、Vite和PWA生产构建通过。日志均在release/fsrs-review-20261101，最终为today-queue-verified.log、today-queue-front-all.log、today-queue-front-build.log。

分科位置确认后，相关前端26项复测、类型检查和构建通过，日志today-queue-branch-front.log；随后全量复测见today-queue-front-final.log。

## 界面与清理

只连接合成测试数据的本机18101后端、5181前端，使用本轮预设测试账号。今日复习区在对应学科／教材下显示原到期日、逾期天数、轮次与原完成／部分按钮，桌面验收通过。临时390×844手机视口下页面宽380、视口390，无横向溢出，两个操作按钮可见，随后恢复默认视口。数学方向页无复习面板，点击真实高等数学分科入口后，复习面板在该分科内容下显示。更换最终JAR前已关闭占用它的临时后端。

截图位于release/fsrs-review-20261101：today-queue-desktop.jpg、today-queue-mobile.jpg、today-queue-progress-branch.jpg。没有使用真实个人数据或在正式网站写入。临时服务在验收后关闭，数据库保留为一次性测试库。
