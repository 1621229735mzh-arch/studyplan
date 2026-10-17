# 英语实际数量发布进展（2026-10-17）

## 当前状态：V21已发布，原任务转换及数据验收完成

2026-10-17 11:40后控制恢复：使用Chrome原生标签列表和claimTab读取新Workbench标签1458833353。逐字输入曾截断，检查截图后清空未执行的部分命令，改为剪贴板粘贴；之后SHA256命令正常执行，服务器归档hash与下文预期完全一致。线上重新核对为V20、三个容器healthy、7条学习记录，任务510／511与原范围一致。解包至/root/english-quantity-release-20261017，确认此前无本次状态目录。

- prepare已完成：备份 `/srv/408doing/private/english-quantity-20261017/preflight-backup/study-20261017-114628.sql.gz`，SHA256 `345711d03f05893f1eec5e497c69c2021cc778718ba504425423dd20da4efde4`；恢复至既有MySQL8.4隔离库，31张业务表哈希及7条原记录一致，RESTORED已生成。
- candidate已通过，CANDIDATE-VERIFIED于11:50生成：V20→V21、任务510 COUNT_STUDY／511 COUNT_REVIEW、37／120数量、重复提交与旧版本拒绝、复习修改80、删除重算、次日独立和认证校验均通过，原记录与生产业务数据保留。候选容器已停止，受限账号已锁定。
- `python3 release.py publish > publish.log 2>&1` 于11:53:38 CST完成。最终备份位于状态目录的 `final-backup/`；后端单文件JAR挂载已强制重建，V21迁移通过，前端保留旧哈希资源、切换index与sw并重载Nginx。HTTPS入口／401权限检查及三个容器健康检查通过，31张业务表旧列哈希一致，原7条学习记录保留，PUBLISHED已生成。
- 用户在新开的正式网站标签登录，点击PWA“立即更新”。通过任务库分别预览并确认：510→COUNT_STUDY／每日背诵新词；511→COUNT_REVIEW／每日单词复习。各123条日安排保持2026-10-15—2027-02-14，取消固定计划量；两项各有2条原学习／确认记录保留。两项阶段目标通过正常编辑界面更新。
- `python3 update_text.py > update-text.log 2>&1` 对原导入3／4和各23条月周纲要执行事务更新，清除固定50／6150及仅勾选说明。保存 `text-before.json`，保留导入key、content_hash、日期及行身份；非目标行哈希一致，TEXT-UPDATED已生成。该操作脚本另存于本机ignored release目录，不改动已验证的发布包。
- `python3 release.py verify > verify.log 2>&1` 于12:03:29 CST通过，CONVERTED标记：englishSchedules=246、originalRecords=7、protectedBusinessTables=31、newProductionRecords=0。除task_activity新增1行外各表行数不变；所有受保护非目标行哈希保留。
- 正式网页验收：今日显示两项“填写数量”，新词／复习数量弹窗均正确且无需计时或掌握评分，检查后取消，没有提交测试数量；计划整体已无旧固定词数说明，原100词保留，历史复习在记录页显示“数量未记录”。数学两书90／30分钟只读估算、LeetCode接续保持。桌面真实页面已检查，手机尺寸本轮未单独验收。

本机正式今日页截图保存在ignored目录 `release/english-quantity/screenshots/production-today-20261017.jpg`。README、PROJECT_MEMORY、DECISIONS及本轮验证文档已同步为已上线状态，最终 `git diff --check` 通过。

发布已完成，不得重复执行prepare、candidate、publish、任务转换或导入。Git工作区仍在develop且未提交／推送。将来发布前须重新核对实时版本和新增学习记录；代码回退不能代替数据恢复，不能直接恢复旧库覆盖新记录。

## 发布前的控制阻碍（已解决）

用户已明确授权“部署”，包括发布V21及转换原有两项英语任务。发布前完成本地实现、MySQL8.0隔离验证和生产只读预检，随后因浏览器接管问题多次暂停。下面仅保留故障证据，实际完成状态以上节为准。

用户随后已手动上传，并于继续前确认Workbench已连接。文件管理器曾显示本次包已在服务器根目录 `/english-quantity-release-20261017.tar.gz`（并非`/root/`），上传任务带成功标记，但尚未执行SHA256核验。后续暂停后Chrome标签／浏览器绑定发生变化；重新发现页面成功，读取原标签、新标签以及DOM均多次超时，尚未取得终端可操作证据。用户再次确认“我链接了”后仍复现读取超时，不能据此要求用户重复登录或认定服务器断开。本机SSH BatchMode只读hostname探测返回Permission denied(publickey,password)，无可用直接SSH认证。

本轮最终通过文档接口的 `chrome.user.openTabs()`／`claimTab()` 恢复页面读取，使用剪贴板粘贴单行命令避开逐字输入中途超时。全局 `cua.getState()` 超时并不代表这个接口也失败；不得仅以浏览器发现、接管成功或终端已连接宣称发布完成。

用户要求“你再试一下”后的实测：Chrome 3 中标签1458833333返回 `Debugger unattached`；另一个原有Workbench标签1458833322曾成功返回完整可访问性树，显示“已连接”和“Terminal input”。随后截图超时，重新通过页面接口读取仍返回同一接管错误，未输入任何终端命令。新建正式网站标签能读取登录页，说明浏览器并非所有页面都不可读；这仍不足以认定Workbench控制已经恢复。发布包hash、服务器版本和数据均未在此次重试中核验，未执行任何发布阶段。

## 生产预检

2026-10-17约00:16 CST，通过原Chrome中的阿里云Workbench免密重新连接，核对实例 i-j6c1ulq7dru9ovj3df56、8.217.241.16，实际部署目录 /srv/408doing/study-20260928-150827，沿用 compose.bundle.yaml。

- Flyway为20:1，MySQL／backend／frontend均healthy。
- 旧jar SHA256为70e8719b4bc0edcac910540f140c36cf2a485f423d19f5f1582e356f986403a6。
- 旧index SHA256为c6542c1bfd3bc4a230c7929de42c8ce2cb53ac1b7e435746a9b32372b358becd。
- 实时学习记录7条，已多于上次发布时4条；必须保留实时数据，不能用旧备份覆盖。task=255、daily_plan_item=529、plan_import=4、plan_period_outline=77、task_activity=253。
- 英语任务仍为510（背诵50新词、总量6150）和511（CHECK_ONLY复习）。预检可用内存约456664KB，未启动服务器候选容器。

这些是预检时刻的结果。恢复后须重新核对版本、服务状态和数据，不能假定等待期间没有新学习记录。

## 已准备的产物

- 本机发布包 release/english-quantity-release-20261017.tar.gz，37099908字节。
- 包SHA256：c5b5ea83c2a3377a06b437342e33ef6763698436d66b41a758ea090b8e810eaa。
- 新jar SHA256：eba80293c680fe2bb67ef0f375d7f5b1c528723050458981afc703113d4e37d5。
- 新index SHA256：83941731f0ea3f2847314e3f808deb6c6801f6dc27fce0bd17a975fcb33bf993。
- 包内含 artifacts/app.jar、artifacts/frontend、base.py、release.py、SHA256SUMS。Python语法检查通过，未在生产执行脚本。

新脚本预定独立状态目录 /srv/408doing/private/english-quantity-20261017，恢复库 english_quantity_verify_20261017，候选 english-quantity-verify-20261017。prepare保存旧产物、做备份并验证在现有MySQL8.4中的恢复；candidate使用受限账号、320MB内存、0.75CPU、不公开端口，验证V20→V21和两项原任务转换、数量填写、重试、修改、删除及旧记录保留；publish在最终备份后切换前后端，并按旧列逐表核对业务行。V21新增review_amount应为空，不能把列新增造成的哈希差异误报为记录损坏。

正式上线后已通过网页任务库预览转换510／511，并核对目标、月周说明和只读verify结果，未创建生产验收账号或学习记录。各阶段日志保留在 `/root/english-quantity-release-20261017/{prepare.log,candidate-run.log,publish.log,update-text.log,verify.log}`，恢复点和保护快照位于状态目录。

## 早期上传阻碍（已由手动上传解决）

早期Chrome文件选择器setFiles曾返回“No node found for given backend id / fileChooser.setFiles failed”。之后用户已手动上传成功，当前包位置和未完成的hash核验以上文为准；不要重复上传。

用户截图已经确认Chrome扩展“允许访问文件网址”开启，不再要求重复设置。当前阻碍为Workbench接管不稳定，不能把它归因为文件访问权限或服务器未连接。

现有用户网站标签接管曾在Emulation.setFocusEmulationEnabled超时；另开同一Chrome中的 https://onlystudy.loc.cc/ 标签成功，但本轮尚未完成新版网页验收。发布成功后必须刷新PWA并检查真实表单，不能用服务健康替代页面验收。
