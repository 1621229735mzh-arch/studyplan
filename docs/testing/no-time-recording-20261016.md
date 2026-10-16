# 不计时记录验证（2026-10-16）

## 范围与结果

用户要求移除所有需要手动填写学习时间的操作。今日完成／剩余、按书接续、每日一题尝试、活动结束、记录补记／修改、复习反馈、计划编辑及设置均移除用时／预计时间／预算输入。保留日期、复习日期间隔、完成量、百分比及掌握反馈；移除缺少用时提示，历史记录用时仍可查看且修改其他字段时保留。

- 前端 11 个测试文件、94 项测试通过；类型检查和 Vite/PWA 生产构建通过。
- 后端 Java 21.0.6、Maven Wrapper 3.9.16：144 项测试通过，0 失败、0 错误，jar 打包成功。数据库为本轮独立创建的 MySQL 8.0.45，127.0.0.1:13318/no_time_test；空库执行并校验 V1–V20，不新增或修改历史迁移。
- 两项 SessionTableCaseMigrationTest 依赖 Linux Docker；本机 Docker 未运行，因此显式排除。本轮未在 MySQL 8.4 或生产环境执行验证，未进行真实浏览器视觉验收。
- JSON Schema 用仓库已有 Ajv 2020 验证两份样例，共原格式／省略时间／时间为 null 六种组合；日期格式由 Java 业务校验覆盖。
- git diff --check 通过。

## 执行命令

仓库根目录：

```powershell
pnpm --dir frontend test
pnpm --dir frontend build
```

backend 目录，在本轮新建的一次性测试库上执行：

```powershell
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13318/no_time_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
$env:PRESET_ACCOUNT_PASSWORD='test-only-password'
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:\Project_JAVA\408doing\.m2repo' '-Dtest=*,!SessionTableCaseMigrationTest' verify
```

这里的绝对路径仅记录本机实际命令，不是项目运行依赖；其他电脑使用自己的 Maven 缓存。隔离实例仅用于测试，验证后关闭。日志在被忽略的 release/no-time-resume-validation/ 下。

## 关键覆盖与修复

- 不提供 durationMinutes 仍可保存学习及复习进度，持久化为 null，不把估算或 0 当成实际用时；数量和百分比按原规则派生。
- 记录重试复用令牌；过期版本拒绝覆盖；删除后普通进度、活动结束与接续内容重算。
- 每日一题不计时尝试仍绑定具体题目，尝试不推进，整题完成才推进。复盘不增加首次学习量。
- 无预算、无估算的复习可以通过 manualSelection=true 手动确认，重复确认不会创建重复安排；100% 仍要求掌握反馈，复习不增加首次学习量。旧客户端不传该字段时继续原额度校验。
- 计划估算可空，接续剩余时间计算避免空值异常；旧有估算、预算和记录用时在修改其他字段时保留。
- 第一轮发现每日一题服务还要求用时，现已移除；新增导入测试修正为独立 planId，并按独立科目过滤断言，避免同日其他测试数据干扰。最终全套结果如上。

## 交付边界

本轮是本地代码与文档修改。未提交、推送、合并或部署；未改动生产学习数据，也未重跑历史清理、发布或计划导入。此前工作区的今日项数等改动保留。

## 随后正式发布验收

用户随后授权部署。2026-10-16 16:18:21 CST 完成发布；已在现有 MySQL 8.4 的新隔离恢复库验证备份恢复及无用时学习／复习 API，31 张业务表在正式切换前后数量与行哈希一致。正式网站 PWA 更新及完成、剩余、补记、计划、设置和复习页面验收通过，原 4 条学习记录保留。上述“本轮未在 MySQL 8.4 或真实浏览器验证”仅指本地阶段，现已由本节补足；Linux Docker 专测仍未运行。证据见 [发布记录](../operations/no-time-recording-release-20261016.md)。
