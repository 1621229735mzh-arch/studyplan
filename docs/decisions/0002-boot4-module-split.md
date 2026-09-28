# 0002 Spring Boot 4 的自动配置按技术拆分

日期：2026-09-22
状态：已采纳

## 背景

按 Spring Boot 3 的习惯只引入第三方库（`org.flywaydb:flyway-core`、`org.springframework.session:spring-session-jdbc`）
时，应用能启动、数据源也能工作，但 **Flyway 完全不执行、Spring Session 也不会切到 JDBC 存储**：
表不存在，会话落回容器内存。

原因：Spring Boot 4 把各技术的自动配置拆成了独立模块
（`spring-boot-flyway`、`spring-boot-session-jdbc`、`spring-boot-webmvc` 等），
只引第三方库不会带上对应的自动配置模块。

## 决定

使用 Boot 4 的技术 starter：

| 需求 | 依赖 |
| --- | --- |
| 数据库迁移 | `spring-boot-starter-flyway` + `flyway-mysql`（MySQL 方言） |
| 会话存 MySQL | `spring-boot-starter-session-jdbc` |
| Web MVC 测试切片 | `spring-boot-webmvc-test`（`starter-test` 不再包含 `@AutoConfigureMockMvc`） |

## 证据

`spring-boot-flyway` 的 POM 依赖 `flyway-core:12.4.0` 与 `spring-boot-jdbc`；
`spring-boot-session-jdbc` 依赖 `spring-session-jdbc:4.1.1` 与 `spring-boot-jdbc`。
换成 starter 后，启动日志出现
`Successfully applied 8 migrations to schema ...`，会话行出现在 `SPRING_SESSION` 表中
（`AuthenticationIntegrationTest.loginStoresSessionInDatabase`）。

## 影响

- 迁移与会话表都在真实数据库上验证通过，不再依赖框架自动建表。
- 升级 Boot 4.x 小版本时，仍需确认这些技术模块的坐标没有再次调整。
