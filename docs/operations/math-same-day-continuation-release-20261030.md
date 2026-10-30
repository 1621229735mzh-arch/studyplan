# 数学同日继续学习发布（2026-10-30）

已于 **2026-10-30 11:10:51 CST** 发布到 https://onlystudy.loc.cc/，真实PWA更新及只读交互验收通过。

## 结果

每本书当天内容全部完成后，显示“继续学习下一段”；可在同一学习日逐段继续。后续内容未实际开始前，不增加今日还剩项数或预计负担。展开只改变页面状态，原学习记录继续保存累计百分比，部分完成、重试、同书版本、删除重算和两书独立推进保持。

原教材更正内容、126天参考排期、各日时长、任务身份和所有已学进度保留，未重新导入计划，无数据库迁移，仍V22。仅更新本次前后端产物，未纳入并行学习记录安全修复或安卓改动。本轮Git提交限于数学接续代码、测试和相关文档，不合并main；其他并行改动保留工作区。

## 构建与限定范围

隔离源码以develop的9dc6e40为基线，位于忽略目录release/math-continue-20261030/source。scope.json列出6个源码／测试文件和导入说明，其他453个跟踪文件逐字节相同。与旧正式JAR比对，仅ContinuationLearningService、其Context和TodayService共3个class内容变化。

本机真实MySQL8.0.45：ContinuedStudyIntegrationTest 14项、StudyDayIntegrationTest 1项、TodayChecklistIntegrationTest 10项，共25项通过；后端打包成功。今日页33项前端测试、类型检查和Vite/PWA构建通过。未运行Linux Docker／Testcontainers专测；MySQL8.4副本验收见下文及[验证记录](../testing/math-same-day-continuation-20261030.md)。临时本机MySQL已正常关闭。

- 新JAR：430e81f4f53257081c6309fa0dd17cc93ffadfe341670c0b5e864b6eb035fc36。
- 新index：506bb5dbcc9da0e014f9b751d30d86bbc1661b439e4c316cd23b0a6325fc5280。
- 原JAR：c435211cb9b281e2ac18c19e05ebc69c9899a5d5fa2b0e0def31fd2b11c65656。
- 初次上传包836586字节，SHA256 a08153cdf86a19f67c5b45bbe278fd54dd05c8140624310e5fab8ed43ab1f6e4。传输及完整还原文件哈希通过。
- 候选检查脚本修正空用时字段读取；远端和本地修正版脚本SHA256均为f2662da26507cd22bbc49ba7db7a35fe2f3ac6332360012ea628569d1d1575d2，SHA256SUMS同步更新。修正版包836607字节，SHA256 ca01d4329daa3235e19897955384e846c9974c1cc192125fae45308c6d2f9939；初次上传包单独保留，可复现修正后的最终脚本和产物。

## 备份、候选与恢复

正式部署目录仍/srv/408doing/study-20260928-150827，compose.bundle.yaml。发布包/root/math-continue-release-20261030，状态和恢复点/srv/408doing/private/math-continue-20261030。

prepare恢复到现有MySQL8.4的独立库，31张业务表及24条原学习记录逐表哈希一致。恢复后内存余量检查两次在启动候选前拒绝操作，未降低门槛；随后采用短维护窗口，暂停正式后端腾出内存，候选结束时finally恢复原后端并等待健康、重载Nginx。未重启整台服务器、未启动第二个MySQL。

候选256MiB内存、320MiB内存加交换、0.75CPU、128进程，无公开端口。首次检查因响应省略空durationMinutes而在脚本触发KeyError；修正为可空读取，从原备份新建math_continue_verify_20261030b重测，不沿用先前合成进度。最终输出CANDIDATE_SAME_DAY_CONTINUATION_VERIFIED，同日多段、部分完成、只读不写记录、两书独立、防重和过期设备保护、删除重算均为true。正式库原业务数据不变。

最终切换前停后端写入并再次备份，备份目录为状态目录final-backup，备份SHA256 2b1136ed7a2ed0bcacaf16812c89a6352c77869fc2c7766047fb14eaa8ca2aa7。旧JAR和前端保存在状态目录previous-app.jar和previous-frontend。原子替换JAR后force-recreate后端，再按资源、入口、Service Worker顺序发布前端，保留旧hash资源，Nginx配置检查和重载通过。

## 最终核验与真实页面

PUBLISHED与FINAL-VERIFIED均已落盘：三个服务healthy、Flyway22:1；31张业务表与24条原学习记录完整保留，41个正式静态文件匹配发布包，候选已停止、测试账号锁定。公网文件逐字节核验41项通过，匿名/api/today和/api/progress/catalog返回401，/actuator/env返回404。

正式Chrome登录后点击PWA“立即更新”，今日仍为真实0／0／5。只读查看10月29日已有的教材第一段完成记录，出现“本书当天内容已完成，可以继续往后学”和“继续学习下一段”；点击后展开第二段“函数的有界性、单调性、奇偶性与周期性”，标签为“继续学习·自选”，75分钟，顶部仍2／0／3。未点击完成／剩余或提交表单，没有新增正式测试学习记录。结束时返回10月30日。

截图和完整日志位于忽略目录release/math-continue-20261030：production-continue-button.png、production-next-expanded.png、production-published.png、production-final-audit.png；公网核验为public-check-result.json。

本次没有数据库结构变化，回退应使用本次保存的旧程序产物；禁止用旧数据库备份覆盖发布后的新学习记录。恢复时先核对PUBLISHED和FINAL-VERIFIED，不重复prepare、candidate或导入数学方案。
