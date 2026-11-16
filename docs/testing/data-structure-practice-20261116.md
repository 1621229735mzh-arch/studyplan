# 数据结构持续每日任务验证（2026-11-16）

最终源码以`origin/develop`的a707bde为基线，保留已上线主题、09:00学习日、自由题号／一天多题、自动复习和数学页码。未修改前端业务文件。用户要求先推送GitHub并暂停部署；本轮没有生产写入、程序切换或线上任务转换。

## 本机验证

Java21.0.11、仓库Maven3.9.16、本机临时MySQL9.7.0，仅监听127.0.0.1:33316。使用全新一次性库daily_practice_latest_test实际应用V1—V27，不连接生产或其他现有数据库。Maven Wrapper发行包使用临时缓存目录。

在backend下实际执行：

```sh
TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:33316/daily_practice_latest_test?allowPublicKeyRetrieval=true&useSSL=false' TEST_MYSQL_USERNAME=root MAVEN_USER_HOME=/private/tmp/study-optional-mysql.pPeBR2/maven ./mvnw -o -B package '-Dtest=*,!SessionTableCaseMigrationTest'
```

结果：242项通过，0失败、0错误、0跳过，JAR打包成功。明确排除依赖Linux Docker的SessionTableCaseMigrationTest，该专测未运行；本轮未做MySQL8.4验证。

新增数据库用例覆盖无截止、无计划题数、十年后仍显示、读操作不生成日安排、起点后排期不再进入统计、原生批量题号与同日追加、次日重开、防重、删除后重放、历史记录／身份保护、题号更正、归档、过期设备、非法转换来源／日期、上下文无截止导出；真实HTTP验证转换接口的登录、CSRF和版本检查。原固定清单、同日多题、自动复习、数学页码、规划导出等全套可运行测试同时通过。

在frontend下使用原有固定版本依赖执行：

```sh
node node_modules/vitest/vitest.mjs run
node node_modules/vue-tsc/bin/vue-tsc.js --noEmit
node node_modules/vite/bin/vite.js build
```

结果：21文件、191项通过；类型检查及Vite／PWA生产构建成功。隔离工作区临时引用已有node_modules进行验证，未重新安装依赖、修改锁文件或提交该引用。

`git diff --check`通过。日志保存于忽略目录`release/daily-practice-20261116/`，临时MySQL验收后关闭。命令中的临时绝对路径只记录本次执行证据，不是跨电脑启动依赖。

## 线上只读预检与范围改变

用户最初要求上线，曾只读核对正式HTTPS首页及匿名认证接口，服务正常；现有本机SSH认证被服务器拒绝，未获得管理连接。随后发现本地旧基线落后远端，建立最新develop隔离工作区实现并重新验证；旧V21实现不进入最终提交。

用户随后要求“先推到GitHub，先别部署”，因此停止远程发布工作。没有上传产物、备份／恢复生产库、执行迁移、重启服务或写入线上学习记录；正式网站的原任务尚未转换。此前旧基线的95／90项结果不是本次最终提交的验证依据，以本文件242／191项结果为准。
