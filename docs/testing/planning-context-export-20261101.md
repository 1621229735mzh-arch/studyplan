# 外部规划上下文导出验证（2026-11-01）

后续用户授权部署：已于23:17:18 CST发布，限定包62项后端与163项前端测试、MySQL8.4候选、33张业务表保护、公网44文件及正式PWA真实下载／手机尺寸验收通过。两格式下载均落盘并核对同一快照，结果与产物见[发布记录](../operations/planning-context-export-release-20261101.md)。下文“未发布、未实机”等描述实现阶段的历史状态；安卓实体设备仍未验收。没有提交或推送Git。

本轮仅本地实现与验证，未发布、未提交或推送Git。已有自动复习、今日队列、学习记录安全等并行改动保留；以下测试针对当前完整工作区，不代表这些改动已经统一发布。

## 实际环境与结果

- JDK 21.0.6，Spring Boot 4.1.1，仓库Maven Wrapper，Vue／TypeScript／pnpm沿用现有版本，没有新依赖或迁移。
- Docker CLI存在，但引擎管道不可用，未运行Testcontainers。另建真实MySQL 8.0.45隔离实例，仅监听`127.0.0.1:13334`，数据位于忽略目录`release/planning-context-20261101/mysql-data`，一次性库`planning_context_test`。未连接正式数据库；未在MySQL8.4验证。
- 新增`PlanningContextExportIntegrationTest` 6项通过；计划工作区20项、课程目录15项、复习工作区6项通过，共47项、0失败／错误／跳过。`clean verify`编译、测试及JAR打包成功。
- 前端新增导出组件5项测试；相关计划页测试11项通过，完整前端19个文件165项通过。`pnpm typecheck`及最新`pnpm build`通过，PWA生成44个应用外壳预缓存资源，未新增个人API缓存。
- 现有变更文件`git diff --check`无空白错误。文档、DTO和前端入口已核对；本轮没有真实浏览器下载、安卓设备保存或生产验收。

## 覆盖的业务与接口行为

1. 保留课程、目录节点、任务、学习记录和备忘录原ID；课程进度源于原记录与原关联。参考排期日期不被实际学习改写。
2. 全量有效历史与近期摘要分离：第30天以前的记录保留在历史，但不混入最近30个学习日摘要；近期从当前学习日减29天开始，含首尾。
3. 导出前后任务、日安排、学习／复习记录、备忘录、自动复习状态和教材策略原表内容相同；连续读取指纹一致，修改或删除记录后指纹变化，删除后的完成量重算。
4. COUNT_REVIEW复习量与首次量分别输出；历史只有确认没有数量时保持未知，不推算零或固定词数，未知次数保留。
5. 没有反馈时不推断掌握；不会反馈有原复习记录证据，后续真实掌握反馈会移除当前薄弱项。
6. 未认证导出返回401；认证后返回明确DTO及`Cache-Control: no-store`，不导出账号密码、会话或提交令牌。
7. Markdown对用户文本中的HTML、竖线、换行等转义，保留复盘内容；JSON与Markdown来自同一次读取。
8. 前端下载保留同一时间与版本；失败可重试，离线禁用，关闭窗口后忽略晚到的响应，重新打开重新读取，断网后丢弃已读快照。

## 实际执行命令

仓库根目录启动隔离MySQL并创建上述一次性库后，在`backend/`执行：

```powershell
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13334/planning_context_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
.\mvnw.cmd -q -DskipTests compile
.\mvnw.cmd -q '-Dtest=PlanningContextExportIntegrationTest' test
.\mvnw.cmd -o -q clean verify '-Dtest=PlanningContextExportIntegrationTest,PlanWorkspaceIntegrationTest,CatalogIntegrationTest,ReviewWorkspaceIntegrationTest'
```

前两轮测试曾遇到测试夹具的flexibleMinutes非空约束及目录direction取值错误；修正夹具后，因编辑发生在编译期间，增量构建仍保留旧测试字节码。最终清理产物并重新编译，47项全部通过，不能将中间失败日志当作最终结果。

在`frontend/`执行：

```powershell
pnpm typecheck
pnpm test -- tests/unit/planningContextExport.spec.ts tests/unit/planWorkspace.spec.ts
pnpm test
pnpm build
```

最终日志：忽略目录`release/planning-context-20261101/backend-clean-verify.log`、`frontend-tests.log`、`frontend-build.log`。构建产物位于原`backend/target`和`frontend/dist`，不进入源码提交。验证结束后核对隔离实例数据目录，再经原13334端口关闭该MySQL；不删除数据目录或停止其他任务实例。
