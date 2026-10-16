# 数学预计剩余时间显示（2026-10-16）

## 范围

按用户截图恢复数学学习任务旁的“预计剩余”时间，仅展示后端已有 remainingMinutes。缺少估算时隐藏，不催补；完成后不再显示。继续使用不计时完成／剩余表单，不恢复实际用时或预算输入，不恢复分区时间汇总。只更新前端。

## 构建与产物

- pnpm --dir frontend test：11 个文件、95 项测试通过。
- pnpm --dir frontend build：类型检查、Vite 与 PWA 构建通过。
- git diff --check 通过。后端无新增修改，本轮未重复后端测试。
- 发布包 release/math-estimate-display-20261016.tar.gz，474,303 字节，SHA256 为 0dc1029dd7b1337b8ee4ce4b57732575e0f6d08c597237c9914b657f488e1de4。
- 新入口 index.html SHA256 为 c6542c1bfd3bc4a230c7929de42c8ce2cb53ac1b7e435746a9b32372b358becd。
- 沿用后端 jar SHA256 70e8719b4bc0edcac910540f140c36cf2a485f423d19f5f1582e356f986403a6；旧入口为 bcee042db72fc736414d0fa6c51ad5d557b8684a980845ac4615e9dd4c724dff。

## 发布流程与接续

用户暂停时只完成上传，未运行发布。继续后先核对上传、哈希和旧版本；暂停期间 Workbench 会话失效，使用既有免密方式重新连接，未重启服务器。正式目录沿用 /srv/408doing/study-20260928-150827。

发布脚本在 /root/math-estimate-display-20261016/publish.py。状态目录为 /srv/408doing/private/math-estimate-display-20261016/，保存旧前端、切换前后业务表哈希及 PUBLISHED。脚本要求生产业务数据与上一轮已验证恢复的数据库备份基线一致，再切换前端，保留旧 hash 资源，最后核对 HTTPS 入口、401、后端哈希、服务健康和 31 张业务表。无数据库写入、迁移或后端重建。不得重复执行已完成的发布；如中断先只读检查标记和当前入口。

## 已完成结果

2026-10-16 **19:11:55 CST** 正式切换成功，输出 PUBLISHED_MATH_ESTIMATE_FRONTEND_ONLY_ALL_BUSINESS_ROWS_PRESERVED。前端入口哈希与上述构建一致，后端 jar 哈希保持不变，三个服务健康，Flyway 仍为 V20；HTTPS 新入口一致，匿名 /api/today 返回 401。

31 张业务表在切换前后数量和行哈希完全一致。关键数量保持 study_record=4、task=255、daily_plan_item=529、plan_import=4；沿用刚完成隔离恢复验收的上一轮数据库备份基线，无需重启后端或写入数据库。

19:13 在原 Chrome 登录会话中刷新并点击 PWA“立即更新”，真实今日页显示：数学主教材预计剩余 1 小时 30 分钟，题册预计剩余 30 分钟；英语和 408 没有恢复估算提示。今日顶部仍为 0／0／5。打开数学“完成”弹窗仅显示任务与取消／保存，不要求填写用时；取消退出，未写测试记录。日期和已有学习内容保留。

截图位于忽略目录 release/math-estimate-production-evidence/：today.jpg、math-estimates.jpg、complete-dialog.jpg；已直接检查数学区域显示。发布验收时尚未提交 Git；实现及本记录纳入本次 develop 提交，不合并 main。
