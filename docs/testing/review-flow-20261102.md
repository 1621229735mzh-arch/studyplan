# 三个现有流程补强验证（2026-11-02）

2026-11-03后续：限定发布源码后端64项、前端175项及构建通过，MySQL8.4恢复／候选和正式V25切换、数据保护及45个公网资源核验完成。实际页面验收边界与证据见[发布记录](../operations/review-flow-release-20261103.md)。下文为11月2日本地阶段记录。

仅本地实现和验证，没有部署、Git提交、推送或修改个人线上记录。保留工作区原有学习记录安全修复、安卓等并行修改。业务规则见[反馈更正与恢复](../business/review-feedback-correction.md)。

## 环境与命令

前端使用原固定pnpm依赖；在`frontend/`执行：

```powershell
pnpm test
pnpm test -- reviewFeedbackCorrection.spec.ts todayChecklist.spec.ts autoReviewBooks.spec.ts reviewSource.spec.ts
pnpm build
```

全量20文件176项通过；最终弹窗补充“网络变化保留选择／当前手动日期”后，相关4文件56项通过。最终`vue-tsc`、Vite和PWA构建通过。没有新增依赖；最终产物45个预缓存条目，没有新增个人缓存切片。

后端使用`D:/jdk`的Java21、原Maven Wrapper和仓库`.m2repo`。现有`backend/target`资源文件不可写，因此复制当前后端源码至忽略目录`release/review-flow-20261102/backend`后构建，不修改构建描述或原业务文件权限。

Docker不可用，未运行Testcontainers／MySQL8.4；本轮使用真实MySQL8.0.45，独立数据目录`release/review-flow-20261102/mysql-data`，只监听`127.0.0.1:13333`，关闭MySQL X协议，最终一次性库为`review_flow_final`。没有使用已有业务库或正式数据。

在独立源码的`backend/`目录执行：

```powershell
$env:JAVA_HOME='D:/jdk'
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13333/review_flow_final?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
./mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=*Review*Test,FsrsSchedulerTest,TodayChecklistIntegrationTest,ProgressWorkspaceIntegrationTest,PlanningContextExportIntegrationTest' verify
```

最终**64项通过，0失败／错误／跳过，Spring Boot打包通过**。新库V1–V25完整迁移及25项迁移校验通过；其中自动复习／更正24项、原复习流程10项、复习工作区6项、FSRS纯计算3项、今日10项、进度5项、规划导出6项。

## 验证覆盖

- 今日缓存命中后仍显示离线、超时、网络不可达和服务异常原因；重读成功才开放写入。没有快照时保留恢复入口；401不展示缓存；缓存保存失败不错误降级实时数据。
- 最近反馈读取、原掌握情况、原记录日期、当前手动日期和更正后果说明；失败保留输入／令牌，换选项生成新令牌；409要求重新加载，编辑时断网保留选择。
- 完整FSRS历史重放后稳定度、难度、轮次、下一日期一致；更正不新增学习／复习事实或完成次数。审计、相同载荷重试、令牌换载荷冲突、旧版本和旧记录拒绝。
- 同一版本并发更正只有一个成功；后续部分进度阻止改写早期反馈；暂停、归档、来源删除及重新学完的日期边界。
- 手动项按当前设置重算，保持待安排；自动项替换手动改期；推荐说明展示部分优先及真实日期来源。
- 原复习流程、日期工作区、今日清单、进度及规划上下文导出回归；更正接口认证、CSRF、请求校验和正常HTTP响应。

首次MySQL回归发现新增record返回类型依赖查询列顺序，已将查询显式按构造参数排列。第二次复跑受到上一轮并发测试提交的合成数据影响，触发规划导出“初始无薄弱项”断言；最终改用全新一次性库验证，不删除或放宽原断言。

日志保存在忽略目录`release/review-flow-20261102/`：`frontend-tests-verified.log`、`frontend-targeted-final.log`、`frontend-build-final.log`、`backend-final.log`。`git diff --check`通过。

本轮未做真实浏览器桌面／手机视觉验收、Android WebView验收、Docker镜像构建或生产迁移／发布。启动命令沿用README；部署时需要新后端执行V25，并一并更新前端，不能只部署前端更正入口。
