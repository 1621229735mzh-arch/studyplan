# 数学并行轮换验证

2026-11-08，验证范围为参考日轮换、自选入口及原数学排期重排。来源内容与题册范围沿用当日已核对的六份资料，没有重写教材文案或虚构1000题题号/题数。

## 已执行与实际结果

1. 隔离源码基线HEAD 7ac8706，仅复制10个本次数学源文件/测试文件。构建另采用工作区已有的规划导出测试夹具隔离修复作为测试依赖，不引入其之外的并行生产源码。JAR与原正式产物对比仅6个相关class和V26迁移变化；旧迁移及非目标业务字节保留。
2. `mvnw.cmd -o -B -Dmaven.repo.local=... -Dtest=ContinuedStudyIntegrationTest,TodayChecklistIntegrationTest,PlanningContextExportIntegrationTest,StudyDayIntegrationTest test`：真实MySQL8.0.45下接续18、今日10、学习日1项通过；规划导出最初因共享测试库夹具产生已有的“全库只应一门课程”断言失败。复制现有隔离修复后，复制文件较旧时间戳导致一次增量编译仍使用旧class；刷新测试源时间戳后单独重跑6项全部通过。合计35项相关测试通过。
3. 测试涵盖轮换日外不增加必学项数、同日自选仍可记录、不伪造数量、当天实际记录始终可见、部分进度在下个参考日接续、旧令牌拒绝、删除后重算、旧计划默认行为不变。没有用正式学习记录演示写入。
4. `pnpm exec vitest run tests/unit/todayChecklist.spec.ts --cache=false`：43项通过。首次共享node_modules的Vitest结果缓存写入遭EPERM，关闭测试缓存后通过；`pnpm build`（含类型检查）及PWA构建通过。
5. 离线Maven的clean插件尚未下载，`clean test`未能执行；后续采用更新测试源时间戳重新编译，测试通过。`mvnw.cmd ... -DskipTests package`成功；为保留部署复习XML的原换行字节进行资源重打包，Java代码未变。
6. 本地排期断言通过：120连续日期、259原任务ID唯一、每书顺序不变、每章教材在对应基础题之前、总17920分钟不变、各书日期非递减；线代与概率首次参考日均11月8日，未来118天日均150.5分钟，85天只排教材、4天只排基础题。
7. 服务器 `release.py prepare`：备份gzip与SHA256校验、恢复到原MySQL8.4独立库、完整业务行一致、V26 SQL语法与原字段不变验证通过。`candidate`输出 `CANDIDATE_TRANSACTION_VERIFIED`，原位更新及完整ROLLBACK对比通过。
8. 用户明确授权具体发布范围与后端容器重建后执行 `publish`，输出 `ROTATION_PUBLISHED_HEALTHY_DATA_PRESERVED`。真实应用Flyway V26:1，数学更新后再次回读一致，其他业务行未变；20:04原三个容器均健康，无残留候选服务。
9. Chrome真实网站点击PWA立即更新：今日线代90分钟、概率60分钟；高数和三个题册显示“今天未安排本书，可按需要自选学习”。只展开高数当前段，顶部剩余项数仍为6，未提交学习记录；未排内容不再被误报为当天已完成。20:16重新读取今日页面正常。
10. 正常下载规划上下文JSON，执行 `release/math-rotation-20261108/verify_export.py` 通过：259任务及内容、32条原学习记录、全部课程目录与百分比、复习项/策略、非数学任务/安排/目标逐项一致；数学259条参考日期/排序/预计分钟与六个序列的轮换标记及可开始日期匹配。输出 `export-verification.json`。

临时本机MySQL已正常关闭。日志、候选清单、完整中文排期及截图保存在忽略目录 `release/math-rotation-20261108/`；关键截图为 `site-rotation-today.png`、`site-optional-expanded.png`、`server-published.png`。

`git diff --check` 对本轮源文件通过；文档检查493个链接，仍有5处既有的 `source-handoff-clean-20261103.md` 缺失引用，本轮新增数学链接未报错。当前个人排期及正式导出均确认由Git忽略，旧顺序阶段排期另存历史文件；没有提交或推送。

## 验证边界

服务器未另起候选Java服务；V26迁移先在MySQL8.4副本执行SQL验证，正式Flyway由发布后的真实应用执行。未运行Linux Docker/Testcontainers专测，也未重新验收安卓真机、离线或跨设备并发。参考用时属于估算，复习另计；内容按真实范围安排，不声称个人学习效率已达到数学意义上的最优。
