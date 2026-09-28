# 0001 依赖版本锁定与核对

日期：2026-09-22
状态：已采纳

## 背景

`PLAN.md` 第五节写明版本线“尚未实际构建验证”，并要求实施时核对兼容性后固定具体补丁版本。
`PLAN.md` 技术栈表中写的是 Spring Boot 4.1.x 搭配 MyBatis Spring Boot Starter 4.0.x。

## 核对方式

通过 Maven Central 元数据与上游 POM 核对，再用真实构建验证：

- `spring-boot-starter-parent` 最新稳定版为 **4.1.1**（4.2.0-M1 为里程碑版本，不采用）。
- `mybatis-spring-boot-starter` 版本线为 3.0.x（Boot 3）、4.0.x（Boot 4.0）、**4.1.0（Boot 4.1）**；
  其上游 `mybatis-spring-boot` 4.1.0 的 `spring-boot.version` 正是 **4.1.0**。
- `springdoc-openapi` 3.x 面向 Spring Boot 4。

## 决定

锁定以下版本，全部由构建实际加载验证：

| 依赖 | 版本 | 来源 |
| --- | --- | --- |
| Spring Boot | 4.1.1 | 父 POM |
| Spring Framework | 7.0.9 | 由 Boot 管理 |
| Spring Security | 7.1.1 | 由 Boot 管理 |
| MyBatis Spring Boot Starter | **4.1.0** | 显式指定 |
| mybatis / mybatis-spring | 3.5.19 / 4.1.0 | 由 starter 管理 |
| Flyway（core/mysql） | 12.4.0 | 由 Boot 管理 |
| spring-session-jdbc | 4.1.1 | 由 Boot 管理 |
| mysql-connector-j | 9.7.0 | 由 Boot 管理 |
| springdoc-openapi | 3.1.1 | 显式指定 |
| Testcontainers | 2.0.5 | 由 Boot 管理 |
| JUnit Jupiter | 6.0.3 | 由 Boot 管理 |

## 与 PLAN 的差异（显式说明，不静默改写）

**MyBatis Spring Boot Starter 使用 4.1.0，而不是 PLAN 中写的 4.0.x。**
4.0.x 对应 Spring Boot 4.0.x；与 Boot 4.1.x 搭配的是 4.1.0。这是核对后的修正，其余版本线不变。

数据库方面，PLAN 指定 MySQL 8.4 LTS：Testcontainers 使用 `mysql:8.4`；
本次本地验证在 MySQL 8.0.45 上执行（开发机未启用 Docker 且系统仅装了 8.0），
已验证的迁移与查询都只使用 8.0/8.4 共同支持的特性（`CHECK`、窗口外的常规聚合、`utf8mb4_0900_ai_ci`）。
换到 8.4 不需要改代码。

## 影响

- 依赖版本全部为上表中的确定值，没有使用 `latest` 或版本区间。
- 升级 Boot 时需同时核对 MyBatis starter 的对应版本线。
