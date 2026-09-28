# 本地开发环境

## 依赖

| 组件 | 版本 | 检查命令 |
| --- | --- | --- |
| JDK | 21 | `java -version` |
| Maven | 3.9.16（Wrapper 固定） | `backend/mvnw -v` |
| Node.js | 22 | `node -v` |
| pnpm | 9 | `pnpm -v` |
| MySQL | 8.4（8.0 也可） | `mysql --version` |
| Docker | 可选，集成测试与部署用 | `docker info` |

后端不使用全局 `mvn`：仓库提交了 Maven Wrapper，首次运行 `./mvnw` 会按
`backend/.mvn/wrapper/maven-wrapper.properties` 下载固定的 Maven 3.9.16。

Windows PowerShell 在 `backend` 目录运行 `.\mvnw.cmd -v` 和 `.\mvnw.cmd -B verify`。
Wrapper 同时处理普通的 `.m2` 目录和具有 Target 的链接目录；普通目录不需要配置符号链接。

## 准备数据库

```sql
CREATE DATABASE kaoyan_study CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE kaoyan_study_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'kaoyan'@'%' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON kaoyan_study.* TO 'kaoyan'@'%';
GRANT ALL PRIVILEGES ON kaoyan_study_test.* TO 'kaoyan'@'%';
```

`kaoyan_study_test` 只给集成测试用：迁移会在该库上执行，不要指向生产库。

## 环境变量

| 变量 | 用途 | 默认值 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库连接 | `127.0.0.1` / `3306` / `kaoyan_study` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 | `kaoyan` / 空 |
| `PRESET_ACCOUNT_USERNAME` | 预设账号登录名 | `kaoyan` |
| `PRESET_ACCOUNT_PASSWORD` | **首次启动建号**用口令 | 无，必须由环境变量提供 |
| `SESSION_TIMEOUT` | 会话有效期 | `7d` |
| `SESSION_COOKIE_SECURE` | 会话 Cookie 是否只在 HTTPS 下发送 | `false`（生产为 `true`） |
| `SERVER_PORT` | 服务端口 | `8080` |
| `TEST_MYSQL_URL` / `TEST_MYSQL_USERNAME` / `TEST_MYSQL_PASSWORD` | 集成测试指向的外部测试库 | 无，缺省用 Testcontainers |
| `VITE_API_BASE`（前端） | 接口前缀 | `/api` |

仓库不保存任何真实口令；`.env` 与 `*.pem` / `*.key` 等已在 `.gitignore` 中排除。

## 启动

```bash
# 后端（dev profile 会开启 Swagger UI）
cd backend
PRESET_ACCOUNT_PASSWORD='首次口令' ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 前端
cd frontend
pnpm install
pnpm run dev
```

若数据库中已存在预设账号，`PRESET_ACCOUNT_PASSWORD` 可以不再提供：初始化器只在账号缺失时创建，
不会覆盖正在使用的口令，也不会把它写进日志。

## 前端调试代理

Vite 开发服务器把 `/api` 代理到 `http://127.0.0.1:8080`。由于会话与 CSRF 都依赖同源 Cookie，
请通过开发服务器地址访问页面，不要直接打开构建产物文件。

## 常见问题

**启动报“未配置预设账号口令”**
数据库里还没有账号，需要提供 `PRESET_ACCOUNT_PASSWORD`。这是刻意设计：不允许创建空口令账号。

**集成测试报“集成测试需要真实 MySQL，但当前没有可用的 Docker”**
启动 Docker，或按 README 设置 `TEST_MYSQL_URL` 等变量指向一次性测试库。

**写操作返回 403**
缺少或过期的 CSRF 令牌。先调用 `GET /api/auth/csrf`，再把返回值放进 `X-XSRF-TOKEN` 请求头
（浏览器端由 `src/api/http.ts` 自动完成）。

**Windows 下用 `-Dtest.mysql.url=...` 传参失败**
`mvn.cmd` 经由 cmd 解析命令，JDBC 参数里的 `&` 会被截断。请改用环境变量 `TEST_MYSQL_URL`。

**Flyway 报校验失败（checksum mismatch）**
已应用的迁移不允许改写。请新增一个迁移文件，而不是修改历史版本；本地测试库可直接重建。

**`pnpm install` 卡住或报 `ECONNRESET` / `ETIMEDOUT`**
默认源 `registry.npmjs.org` 在部分网络下不可达。可临时指定镜像安装，锁文件中只保存完整性哈希、
不保存源地址，因此换源安装不会污染 `pnpm-lock.yaml`：

```bash
pnpm install --registry=https://registry.npmmirror.com
```

**前端依赖里为什么固定 Vite 5**
`vitest 2.1.8` 依赖 `vite ^5`，而 `vite-plugin-pwa 0.21.1` 的 peer 范围是 `^3 || ^4 || ^5 || ^6`。
项目把 `vite` 固定为 `5.4.21`，让三者落在同一个 Vite 大版本上；否则会同时装上两个 Vite，
`vue-tsc` 会因为插件类型来自不同版本而报错。
