# 高数完整进度目录发布与导入（2026-10-30）

百分比目录功能于 **2026-10-30 16:32:51 CST** 发布到 https://onlystudy.loc.cc/，随后经正式网站预览确认导入两本高数资料，真实页面和最终数据保护校验通过。无需重复发布或导入。

## 交付结果

- 数学 → 高等数学包含2027张宇《基础30讲》高数主教材和《1000题》基础篇高数。
- 两书各18章节、126段内容，共288目录节点、252原任务关联。教材内容沿用用户PDF校正后的计划及已核实页码；题册沿用原计划，不猜题量或页码。
- 每段从原有效学习记录累计百分比；章节和资料只统计已完成、进行中、未开始的内容项数，不换算讲数／题数，不生成整体完成百分比。
- 原任务ID、参考日期、估算、两书独立接续和全部原学习记录保持；目录只补充结构与归属，没有重导计划或写正式测试记录。
- 本轮Git交付仅包含高数内容百分比目录代码、迁移、测试及相关文档，推送develop。安卓、学习记录安全修复等并行改动保留，未纳入发布包或本次提交。

## 构建与测试

以afbd3cb隔离归档，scope.json限定13个源码、测试及说明文件，451个范围外跟踪文件与基线一致。JAR差异限定为progress包及V23迁移。真实MySQL8.0.45上31项后端测试及verify打包通过；24项前端测试、类型检查、Vite/PWA构建通过。未运行Docker／Testcontainers专测；正式环境MySQL8.4独立副本通过下述验收。具体命令及边界见[测试记录](../testing/math-content-catalog-20261030.md)。

本轮包849238字节，传输SHA256为f50d4e93728941e41311a6300c901c9d656e777ab911d64f7e03724a63e6c4df。

| 产物 | SHA256 |
| --- | --- |
| 新JAR | f9226032514c75fc5b654b910aa1348e77e6388c91f539fdda41139075c650d3 |
| 新index | 38ae4ee35eb9291dc8fcb2f416207236b07ea566d6b7e4f6e52a9b97c0c685cf |
| 原JAR | 430e81f4f53257081c6309fa0dd17cc93ffadfe341670c0b5e864b6eb035fc36 |
| 原index | 506bb5dbcc9da0e014f9b751d30d86bbc1661b439e4c316cd23b0a6325fc5280 |

## 备份、候选与发布

部署目录仍为/srv/408doing/study-20260928-150827，compose.bundle.yaml。发布包/root/math-catalog-release-20261030，私有状态与恢复点/srv/408doing/private/math-catalog-20261030。已校验上传包、还原完整产物及全部文件哈希。

prepare备份恢复到现有MySQL8.4的math_catalog_verify_20261030，31张业务表、24条原记录逐表哈希一致。预检备份SHA256为e38d7cb3ce67e47cd4ad2b68e8815a427f31408d28ea016f4fb0dcf8fe313e19。

候选限制256MiB内存、320MiB内存加交换、0.75CPU和128进程，无公开端口。短暂停正式后端腾出内存，候选结束由finally恢复原服务并等healthy、检查和重载Nginx。候选首次通过，验证252项原百分比、无数量转换、重复确认及再次导入防重、记录删除重算；旧课程响应和所有原业务行保持。仅副本写合成进度，结束后候选停止、测试账号锁定。未重启整台服务器，未额外启动MySQL实例。

正式切换前停后端写入并最终备份，SHA256为01d36a6c84f918b4bc6cfdbb1435e28e4d6c5215d277829a15f966901aa04089。原JAR／前端保存在状态目录previous-app.jar／previous-frontend。原子换JAR后force-recreate后端，V23迁移成功；按旧列清单比较31张原业务表完全一致，并显式校验旧课程progress_mode均为QUANTITY。更新前端、保留旧hash资源、Nginx检查和重载通过，三个服务healthy。

V23只新增progress_course.progress_mode，默认QUANTITY，不改写已应用迁移。回退先核对新目录模式和发布后数据兼容性；不得用旧数据库备份直接覆盖新记录，也不能仅换旧JAR后把CONTENT_PERCENT目录当数量目录运行。

## 正式导入及验收

Chrome点击PWA“立即更新”，数学原目录确认为空。正常选择目录JSON并“校验并预览”，两书各显示“0 → 126项内容 · 沿用原百分比”；逐门核对144节点、126任务关联后勾选核对，点击“确认保存目录”。没有经SQL直接写正式目录。

实际页面教材显示已完成1项、进行中0项、未开始125项；第一段函数、反函数、复合函数与隐函数（教材1—8页）为100%，其余125段为0%。题册126段均为0%。均来自原记录，无讲数／题数分母或资料整体环形图。

IMPORTED-VERIFIED已落盘：36章节、252内容及252任务关联、全部key／父级／标题／null总量／排序匹配；仅新增两门课程、288节点、252任务关联及目录版本／预览元数据。原目录行逐字节保留，所有非目录业务表和24条原学习记录与切换前一致。候选停止，三个服务健康。

公网逐字节核对41个静态文件通过，/api/today和/api/progress/catalog匿名返回401，/actuator/env返回404。本机首次公网核验因沙箱DNS失败未执行成功，允许联网后相同脚本执行通过，没有放宽校验。

证据在忽略目录release/math-catalog-20261030：production-candidate.png、production-published.png、production-import-preview.png、production-courses.png、production-text-percent.png、production-practice-percent.png、production-import-verified.png、public-check-result.json；完整服务器日志为/root/math-catalog-{prepare,candidate,publish,imported}.log。
