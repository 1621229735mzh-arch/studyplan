# 自动复习发布记录（2026-11-01）

本次发布用户已明确授权；交付仅自动复习及其与已发布复习工作区的关联，原安卓、通用学习记录安全修复不纳入。2026-11-01 15:56:45 CST正式发布V24，16:02:20 CST完成指定教材启用后的数据审计；正式PWA桌面与390px手机布局验收通过。

## 独立构建与范围

以已上线复习工作区的隔离源码为基础（Git基线7731b58c5320375d68339b1c50b76d53cc9738c0），共限定49个文件，444个范围外基线文件保持原值。学习记录服务只移入事务事件通知，不携带并行安全修改。JAR内容差异检查仅允许review模块、学习完成证据／事件服务、StudyRecordService、review SQL及新增V24；旧V1—V23迁移逐字节一致。没有Git提交／推送或合并。

隔离发布源码后端77项测试、前端155项测试、vue-tsc、Vite／PWA与Spring Boot打包通过。使用本机独立MySQL8.0.45；生产MySQL8.4恢复／候选测试见下文。日志和scope.json在忽略目录release/fsrs-deploy-20261101。

| 产物 | SHA256 |
|---|---|
| 实际终端差分传输包（133307字节） | d941cffb5d5403151a68e7adb0a296cf82f9853532f2a6610de1f76c653c4ca4 |
| 新JAR | 1a9ad7061eeaa922823895825d7b697afacc25cfa6006f4eaab1a99942f3236d |
| 新index | 979593b810a23c93eac099031e34b5d2ced6c33206b3f3de10458569b10a2ebb |
| 原JAR | d60494a16cd844687d1a7eda0ac68985f677fff3e71e60328b068d1c77e4e136 |
| 原index | 2825f35c93af73584a8c2acadf1bd07ce0a4fe8dbaf8fb0b9701c723ebb7940b |

Workbench文件选择事件没有成功返回，原生Chrome控件访问等待超时。未改变浏览器权限、服务器SSH授权或公开文件访问；改用既有Workbench终端分三段传输自建差分包，断点检查45000／90000／133307字节及完整SHA256通过。以正式旧产物重建新完整JAR及前端后，SHA256SUMS全部通过。

## 服务器与恢复点

沿用/srv/408doing/study-20260928-150827/compose.bundle.yaml。正式切换前重新核对：三个服务健康、MySQL8.4.11、V23、25条有效学习记录、0个复习项，原JAR及index哈希与上表一致。

本次远程发布目录/root/fsrs-review-release-20261101，私有状态目录/srv/408doing/private/fsrs-review-20261101。旧产物保存在previous-app.jar及previous-frontend；数据库备份为preflight-backup和final-backup目录。恢复验证使用现有MySQL中的fsrs_review_verify_20261101库，不启动第二个MySQL。


## 备份恢复与校验执行

预检备份SHA256：909dc196b37237c63fe1acce66d8b17e08f5bd29635ae184c8bd6c554adc2984。恢复库31张原业务表、25条学习记录逐表一致，RESTORED标记已生成。原校验逐次执行docker exec耗时较长，中途仅暂停只读prepare进程查看日志后继续原作业，未重复prepare或恢复数据，最终正常完成。

后续校验新增fast_snapshot.py，将只读SQL合并成批次，沿用原表排序、字段和字符串规范。正式31张表的批量结果已与prepare保存的原逐表哈希全部比对一致；发布校验脚本与SHA256SUMS同步更新并重新校验。业务JAR和前端产物哈希没有变化。

## 暂停与候选环境修正

首次候选健康检查超时，日志为`Unable to access jarfile /app/app.jar`，候选退出码1、未发生OOM。清理流程已停止候选、锁定测试账号并恢复原后端健康；正式库仍V23。用户暂停后再次明确要求继续。

继续时检查确认：候选JAR为root所有、权限600，容器运行UID10001，因而无法读取；正式三个服务健康，候选恢复库仍V23且31张表与恢复点完全相同。将候选JAR权限改为644，保留原失败容器为`fsrs-review-verify-20261101-failed-permission`；重试沿用已锁定测试账号和原凭据，不增加授权、不重复恢复数据库。发布脚本和清单同步更新，业务产物哈希保持不变。

权限修正后候选正常启动并升级V24，冒烟依次通过首轮日期、今日同源、40%部分复习、完整100%反馈与60%增量、下次日期、防重409、第二轮展示、手动改期及教材暂停写入。检查脚本在暂停后用`item()['scheduledDate']`读取已省略的空字段，报KeyError；改为`.get('scheduledDate')`。原服务再次恢复健康，候选保留断点；续验先检查原学习事实、V1—V23校验、已完成两条复习记录及教材暂停状态，再验证恢复保留手动日期和单项暂停，不重复反馈或重置恢复库。

## 正式切换与验证

执行`python3 release.py resume-candidate`退出0，`CANDIDATE_V24_FSRS_VERIFIED`与`ORIGINAL_BACKEND_RESTORED_HEALTHY`均确认。原教材streamId=1，恢复库内1段具备完整学习来源；日期、反馈、幂等、手动日期与暂停行为通过。随后执行`python3 release.py publish`退出0，15:56:45.354617+08:00写入PUBLISHED。

正式切换前final-backup的gzip及SHA256校验通过，摘要为`51fb5b85353d78c3e525e3a51cfa3249d7258ec39f9532acd3b02ec7a9614451`。后端强制重建、Flyway V24成功，前端保留旧hash资源并完成nginx检查和重载。31张原业务表按旧列逐表哈希不变，原25条学习记录保留，初始教材开关和记忆状态均空，未创建正式测试记录。

执行`python3 release.py verify`退出0，写入VERIFIED：原表保留、V24、候选已停止、测试账号锁定。三个正式服务均healthy。该核验在任何正式教材启用之前运行；随后若启用教材，新增review_item、review_auto_policy、review_fsrs_state属于明确预期变化，另行审计，不能继续套用零复习项基线。

Windows执行`python release/fsrs-deploy-20261101/public-check.py`联网核验通过：44个静态文件与产物哈希一致；自动教材、内容段、复习工作区、今日与进度接口匿名均401，`/actuator/env`404。初次受限环境DNS失败，使用已批准的联网执行后退出0。浏览器已点击PWA“立即更新”，用户自行在原网站恢复登录；没有索取密码或以服务器凭据替代个人登录。

## 指定教材启用与正式页面验收

在正式复习页核对两本原教材后，仅通过正常UI确认启用`《基础30讲》高数主教材`（streamId=1），`《1000题》基础篇高数`仍未启用。主教材共126个原每日内容段，当前1段满足有效学习记录累计100%：`第1讲 · 函数、反函数、复合函数与隐函数`，实际学完日期2026-10-29，首轮2026-10-30。页面显示1段逾期，保持真实到期日，不自动挪到今日。以后学完的小段自动纳入，各段独立反馈排下一次。

随后运行`python3 activation-audit.py`退出0，16:02:20.036983+08:00写入ACTIVATED-VERIFIED：仅streamId=1的开关启用，恰好1个复习项与1个FSRS状态；来源任务集合和完成日从原有效学习记录窗口累计独立重算，与实际生成逐条一致，首轮等于完成次日。stability、difficulty、last_review_date均null，repetitions=0，没有伪造首次掌握反馈。除预期新增复习项外，其余30张原表逐表哈希一致，原25条学习记录保留，正式review_record仍0；候选停止、测试账号锁定。

正式PWA桌面展开教材、实际内容段／日期／轮次及暂停／调整日期入口显示正常；临时390×844视口验证同一正式数据，页面宽380、视口390，无横向溢出，底部手机导航可见，随后已恢复默认视口。没有在正式库填写演示学习进度或复习反馈。相关写入、防重、部分复习与暂停恢复行为已在恢复候选库完成。

本地忽略目录`release/fsrs-deploy-20261101`保留`production-published-verified.jpg`、`production-activation-audit.jpg`、`production-review-desktop.jpg`、`production-review-mobile.jpg`、公网检查JSON和隔离构建日志。未提交／推送Git或合并分支。未执行真机WebView专项或完整断网离线专项；只读离线边界未扩展。

## 回退边界

启用前的零自动数据阶段已验证兼容保护；现在已启用正式教材，禁止直接运行旧脚本回退旧JAR继续写入。需要回退时先暂停自动写入，结合当前数据评估保留状态的兼容方案，不恢复旧备份覆盖发布后的记录。保留原正式备份、旧产物、失败日志及候选恢复库，不重复prepare、publish或自动启用。
