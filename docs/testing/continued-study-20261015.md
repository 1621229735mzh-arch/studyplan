# 张宇按书学习接续：开发与验证（2026-10-15）

## 本轮交付

- 新增显式 CONTINUED_STUDY 接续类型及导入 v5，保持普通任务、每日一题、时间型和每日勾选的旧规则。
- 今日按书收纳具体内容；剩余与新内容分别记录累计百分比。A 完全未推进则延迟 A；A 部分完成则下一日期出现 A 剩余＋B；只收尾 A 而 B 未动则继续 B，不加入 C。两书独立，允许超过参考结束日期。
- 百分比增量保存在原学习记录 continuation_delta 中，amount 为空。不会伪造成题数、讲数或课程学习量；记录页展示本次进度增量。
- 各书行锁、整书 revision 和 clientToken 保护并发及重试；删除后重算，后续记录身份保留。学习记录编辑不能改挂到尚未进入安排的内容。
- 新增 V20，不修改 V1—V19；同步 PLAN、README、目录、API、数据库说明、导入 Schema 与规划书。
- 导入窗口可下载两书七天格式样例，每天新内容合计120分钟、比例随内容变化，无复习和订正任务。它仅验证新格式和展示方式，不是完整126天方案。

## 中断续接检查

中断前 git 工作区包含本轮新类型、今日组件与首批测试；分支 develop，未提交。中断时启动的测试已在后台完成，首次后端50项、前端87项通过。恢复后检查 git status、git diff、未跟踪文件及日志，再补齐边界、文档、Schema 与最终测试，没有覆盖或重置已有代码。

## 最终后端验证

本机 Java 21.0.11、Maven Wrapper 3.9.16；新建独立 MySQL **9.7.0** 实例，仅127.0.0.1:33415，专用库 continued_study_test；不是生产库。数据库行为是真实 MySQL，不是 H2。Flyway 从空库执行到 V20，后续测试校验20份迁移；V20 的内容未在应用后改写。

实际命令（backend 目录）：

```bash
TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:33415/continued_study_test?allowPublicKeyRetrieval=true&useSSL=false' \
TEST_MYSQL_USERNAME=root TEST_MYSQL_PASSWORD='' \
MAVEN_USER_HOME=/private/tmp/408doing-cleanup-20261015/maven \
./mvnw -o -B '-Dtest=*,!SessionTableCaseMigrationTest' verify
```

结果：**136项通过，0失败、0错误，BUILD SUCCESS，打包成功**。其中新增 ContinuedStudyIntegrationTest 8项覆盖漏做不积累、部分完成、只收尾旧内容、两书独立、参考截止后继续、累计增量、防重、过期设备、删除重开、后续日期总进度保护、导入严格校验、参考安排维护、编辑入口保护、同书并发和可下载样例。

测试排除了必须依赖 Linux Docker 的 SessionTableCaseMigrationTest；没有运行 MySQL 8.4 / Linux 大小写专测或 Docker 镜像构建。Flyway 提示 MySQL9.7高于其已验证的9.4版本，不能把本轮验证写成生产8.4验收。

日志：本机临时 /tmp/continued-backend-final.log；不是跨电脑依赖。

## 最终前端验证

pnpm 入口在本机尝试自动联网检查/安装，受环境限制未执行成功；没有修改包清单或锁文件。最终直接使用仓库现有 node_modules/.bin 工具运行，frontend 目录实际命令：

```bash
./node_modules/.bin/vue-tsc --noEmit
./node_modules/.bin/vitest run
./node_modules/.bin/vite build
```

结果：类型检查通过；**87项、10文件通过**；生产构建与 PWA 预缓存生成通过。新增今日组件测试覆盖按书收纳、旧/新内容各自百分比提交、简化文案和离线只读。没有进行真实浏览器或手机尺寸视觉验收。

`git diff --check` 通过。测试完成时尚未提交或推送；随后按用户要求将代码与当前计划摘要一并提交到 develop，远程同步结果以 Git 实际状态及完成报告为准。本轮没有发布或操作生产学习记录。测试完成后已关闭本次专用 MySQL 实例，临时测试目录未作为项目依赖。

## 数据与上线边界

- 旧的张宇时间型计划不会因代码升级自动转换或删除；新导入仍是确认后追加，不是覆盖接口。上线后不能把首周样例直接当作完整方案替换旧数据。
- 本轮没有读取到原始完整计划 Markdown/JSON，也未连接生产数据库。完整逐日内容的重排与旧数据替换尚未执行。
- 计划日历保留参考安排并明确标注；实际执行内容、进度和延期在今日按记录派生。前端离线只展示已经同步的快照，不在浏览器自行推进。
- 记录编辑可修改用时及文字，进度或日期/任务归属更正使用删除后重记，防止将已有百分比悄悄转移到其他内容。
