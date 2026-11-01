# 复习工作区框架验证（2026-10-30）

2026-10-31后续：已使用隔离发布包完成正式部署；隔离包排除并行记录安全修复，因此前端为148项（下文完整工作区为150项），后端仍45项。MySQL8.4恢复／候选、正式数据保护和桌面PWA验收通过；正式手机尺寸检查未完成，具体边界与证据见[发布记录](../operations/review-workspace-release-20261031.md)。下文保留30日本地实施时的结果与状态。

## 范围

沿用计划页面的整体／月／周／日布局，连接原复习项、今日执行、记录树与进度概览。没有填写或导入个人复习内容，没有数据库迁移、Git 提交或正式发布。已有并行学习记录安全修复与安卓改动保留在工作区。

## 后端

Java 21.0.6，Maven Wrapper 使用仓库 `.m2repo` 离线依赖；本机 MySQL 8.0.45 仅监听 `127.0.0.1:13331`，本轮新建一次性 `review_framework_test` 空库，由 Flyway 执行原 V1—V23。没有连接正式学习数据库。日志在忽略目录 `release/review-framework-20261030/`。

```powershell
$env:JAVA_HOME='D:\jdk'
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13331/review_framework_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=ReviewWorkspaceIntegrationTest,ReviewFlowIntegrationTest,ProgressWorkspaceIntegrationTest,CatalogIntegrationTest,TodayChecklistIntegrationTest' verify
```

45项测试通过，0失败、0错误：新复习框架6项、原复习流程10项、记录与进度5项、课程目录14项、今日10项。覆盖未经确认不进入日历、原今日读取同一复习项、版本冲突、移除安排保留部分痕迹、只有复习的日期进入记录树、跨月不重复、科目／任务筛选、重复内容去重、归档保护和复习不增加首次学习量。

最终 verify 的测试部分全部通过，打包阶段因本轮预览进程占用 target JAR 而失败；关闭预览后执行以下命令，Spring Boot 打包成功。未改代码或重复运行已经通过的测试。

```powershell
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' -DskipTests package
```

## 前端

在 `frontend/` 执行：

```powershell
pnpm test
pnpm build
```

17个测试文件、150项全部通过；vue-tsc、Vite与PWA生产构建通过。新增复习工作区10项，另扩展原记录／进度回归以验证复习汇总快照、离线读取和旧快照缺少复习字段时显示尚未同步。覆盖四种视图、月份进入月历／日期详情、科目筛选、空状态、离线禁写、失败重试、按原版本确认安排以及保留失败表单。

## 真实页面检查

本轮本地空预览库 `review_framework_preview`、后端 `127.0.0.1:18093`、Vite `127.0.0.1:5179`，只使用临时测试账号。Chrome 实际检查复习整体空状态、周视图、添加安排弹窗和数学进度页复习入口；没有新增学习或复习内容。390×844 手机宽度下周视图纵向排列，导航均为44px高、无页面横向溢出，空内容弹窗禁止确认。发现并修复共享计划导航“整体”在窄屏换行；检查后恢复浏览器默认视口。

## 边界

Docker Desktop 引擎不可用，未运行 Testcontainers；使用真实 MySQL 8.0.45，未声称通过 MySQL8.4或生产数据验收。未重跑全部后端测试。本轮未部署、未改变正式学习数据；临时前后端与MySQL检查完成后关闭。
