# 今日任务分类按钮切换发布（2026-11-17）

2026-11-17 17:38:00（北京时间）已发布至[正式网站](https://onlystudy.loc.cc/)，随后完成正式桌面／手机切换验收及全部业务数据保留复核。

## 已上线行为

主线／每日／支线改为绑定内容区的按钮，默认每日。每次只显示选中的一类，按钮高亮并设置 `aria-pressed`；不再锚点跳转或上下同时展示三类内容。电脑侧边按钮、手机顶部按钮保持响应式布局，往返切换保留教材展开状态。切换本身不发业务请求或写入记录，全部今日任务统计继续采用原后端返回值。

本次仅基于17:00已发布源码覆盖 `TodayView.vue`、`styles/today.css`、`todayChecklist.spec.ts` 三个文件，独立源码位于 `release/life-quest-switch-20261117/source/`。没有夹带工作区其他改动，没有替换后端JAR、执行迁移、重启服务或提交／推送。

## 验证

- 独立源码 `pnpm test`：21个测试文件193项通过。分类用例检查默认可见区域、三类互斥、选中状态、往返主线保留展开内容，以及切换不增加请求或写入。
- 独立源码 `pnpm build`：vue-tsc、Vite及PWA构建通过；47个静态文件。
- 发布前再次校验源码覆盖清单、包SHA256和包内全部文件；线上旧index及V27 JAR与预期一致，三个服务健康，新的恢复点不存在，确认没有重复发布。
- 备份完整前端与正式数据库，在现有MySQL实例的全新空库 `life_quest_switch_restore_20261117` 恢复，并比较所有业务行、账号和Flyway历史，通过。没有新启数据库实例或恢复覆盖正式库。
- 资源先于index、Service Worker最后更新，保留旧hash资源。服务端逐一读取全部47个HTTPS文件，与候选哈希一致；匿名今日和计划接口返回401。
- 切换前后以及正式页面验收后逐行比较，55条学习记录及全部业务行、账号、迁移历史一致。仅排除正常读写的JDBC Session表。后端JAR保持原哈希，服务健康，无重启。

## 正式页面验收

原登录会话点击PWA“立即更新”后，默认页面只展示每日任务，主线及支线区域隐藏。点击主线后只显示主线，点击支线后只显示支线空状态；对应按钮选中状态一致。键盘Enter可切回每日。切换后原“填写完成题号”弹窗正常，随后取消，没有保存演示学习记录。

390×844手机尺寸确认顶部三类按钮、选中高亮、每日卡片及底部导航；手机切至支线后仅支线可见，页面宽度380小于视口390，无横向溢出。完成后恢复默认视口。未进行HarmonyOS真机验收。

浏览器日志只有沉浸式翻译扩展自身的规则同步失败，未采集到应用来源的error。此前工具超时／URL识别停止均发生在发布前，用户恢复连接后重新预检才执行发布，没有为此更改浏览器安全设置或重启服务器。

截图：本机 `release/life-quest-switch-20261117/online-daily.jpg`、`online-mobile.jpg`，均为正式网站真实页面。

## 恢复点与产物

| 项目 | 值 |
| --- | --- |
| 正式目录 | `/srv/408doing/study-20260928-150827` |
| 私有恢复点 | `/srv/408doing/private/life-quest-switch-20261117/` |
| 数据库备份 | `backup/study-20261117-173526.sql.gz` |
| 发布包 | `release/life-quest-switch-20261117/life-quest-switch-deploy.tar.gz` |
| 发布包SHA256 | `2d37e3f675012d62bd20c12edad003a57f903b82c91eece598e6024d8257fa8e` |
| 原index SHA256 | `516bc34f7492c2c569068f53690b864b0ea3ccaa0778300add37adbd3cb76dec` |
| 新index SHA256 | `005b57944da8ec21fa3257ab024bd3069293a4c0984a96f6d628f24023f1d214` |
| 后端JAR SHA256（未变） | `be27dd46ed80fe704e54da6c50ccb5d6d9490cc19f38e98666a8c87828afacdb` |

服务器发布脚本位于 `/root/life-quest-switch-20261117/publish.py`；恢复点保留旧前端、备份、业务快照及 `PREPARED.json`、`PUBLISHED.json`、`UI-VERIFIED.json`。最终终端结果为 `LIFE_QUEST_SWITCH_UI_VERIFIED_ALL_ROWS_PRESERVED 55`。已经发布，勿重复prepare或publish。

代码回退只恢复该恢复点的旧前端入口及Service Worker等文件，不覆盖发布后新增真实记录。原始仅前端静态包继续保留作为构建证据；实际上传执行的完整部署包以上表为准。
