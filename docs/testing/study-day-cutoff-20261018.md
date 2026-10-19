# 学习日次日早上9点截止（2026-10-18）

后续状态：2026-10-18 23:30:10 CST已正式发布，19日继续后完成PWA与数据保留验收，见[发布记录](../operations/study-day-cutoff-release-20261018.md)。下文为实施阶段的本地验证；独立发布源码排除并行安全修复，前端130项、后端44项及打包通过。

## 规则与范围

用户要求：19日06:00填写的学习记录归18日。统一北京时间（Asia/Shanghai）每天09:00切换，09:00整点开始归新一天。今日、学习／复习默认记录日期、计划默认日期、记录默认回看期间、默认进度区间与复习建议采用同一口径。手动所选日期与已有记录不再偏移，没有历史日期迁移。

后端规则在 `settings/service/StudyDay`，前端默认日期及离线今日快照共用 `todayIso`。日期加减、预览过期和实际时间戳不变；备忘录独立的自然日到期提醒不改。每日一题沿用记录所属日期的名额，不额外产生凌晨名额。

今日页显示当前学习日及截止规则。页面保持打开、恢复焦点或恢复可见时检查是否跨截止；空闲时自动刷新。正在填写或保存时保留原日期、内容和令牌；手动查看历史日期不跳回今日。截止瞬间点击旧清单会先刷新再操作。离线快照在次日09:00前可继续只读查看，截止后失效；不扩展离线范围。

本次在 develop 增量修改，保留原学习记录安全修复及其文档。本地实施阶段未提交、推送、合并或部署，也没有连接现有学习数据库；后续发布状态见上方链接。无需新迁移，仍为V22。本地启动命令不变。

## 验证

前端使用现有锁定依赖，在 frontend 目录执行：

```powershell
.\node_modules\.bin\vitest.cmd run
.\node_modules\.bin\vue-tsc.cmd --noEmit
.\node_modules\.bin\vite.cmd build
```

全量16个测试文件、132项全部通过；类型检查与PWA生产构建通过。

边界用例覆盖：19日00:00、06:00、08:59:59.999归18日；09:00及之后归19日；跨月、跨年、闰日、同一时刻其他时区表示。两个记录表单验证凌晨默认前一天、手选日期保留及09:00后reset使用新日期。今日组件验证凌晨提交日期、持续打开自动切换、截止瞬间旧清单保护、已打开表单保留日期、历史日期不跳转。离线验证06:00保留原快照、09:00失效。

后端 Java21、MySQL8.0.45，使用本轮新建的隔离实例127.0.0.1:13322和study_day_test，数据位于忽略目录release/study-day-20261018/data。PowerShell在backend目录执行：

```powershell
$env:JAVA_HOME='D:/jdk'
$env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:13322/study_day_test?allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_MYSQL_USERNAME='root'
$env:TEST_MYSQL_PASSWORD=''
.\mvnw.cmd -o -B '-Dmaven.repo.local=D:/Project_JAVA/408doing/.m2repo' '-Dtest=StudyDayTest,StudyDayIntegrationTest,TodayChecklistIntegrationTest,SequenceLearningIntegrationTest,ReviewFlowIntegrationTest' verify
```

44项全部通过、0失败／错误／跳过，后端打包成功。包含8项精确日期边界测试，以及真实MySQL上的记录保存、默认今日／进度／复习日期切换、手选历史日期保留与原有今日／序列／复习回归；空库V1–V22迁移通过。测试实例在命令finally中通过mysqladmin正常关闭。

初次数据库回归受沙箱网络限制，连接失败，不能算通过；随后在同一获准执行环境中启动、连接隔离实例并完成上述回归。自动刷新测试首次未在更换fake-timer配置前恢复真实时钟，导致未模拟interval；修正测试时钟设置后再次运行。详细日志保留在忽略目录release/study-day-20261018/。

本地实施阶段未运行全套后端测试、Docker专用测试或生产验收；后续MySQL8.4恢复、候选与生产验收见发布记录。
