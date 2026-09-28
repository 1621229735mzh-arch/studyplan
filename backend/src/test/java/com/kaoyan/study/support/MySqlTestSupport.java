package com.kaoyan.study.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 集成测试使用的真实 MySQL。
 *
 * <p>数据库行为不能用内存库替代，因此集成测试始终连接真实 MySQL：
 * <ul>
 *   <li>默认启动 {@code mysql:8.4}（与计划中的版本线一致）Testcontainers 容器；</li>
 *   <li>无法使用 Docker 时，指向一个专门的一次性测试库，二选一：
 *     <ul>
 *       <li>环境变量 {@code TEST_MYSQL_URL} / {@code TEST_MYSQL_USERNAME} / {@code TEST_MYSQL_PASSWORD}</li>
 *       <li>系统属性 {@code -Dtest.mysql.url=...} 等（注意 JDBC 参数里的 {@code &} 会被
 *           {@code mvn.cmd} 的 cmd 解析吞掉，推荐用环境变量）</li>
 *     </ul>
 *   </li>
 * </ul>
 * 外部库必须是一次性测试库：迁移会按版本序列在该库上执行。
 */
public final class MySqlTestSupport {

    private static final Logger log = LoggerFactory.getLogger(MySqlTestSupport.class);

    /** 与计划一致的 MySQL LTS 版本。 */
    private static final String MYSQL_IMAGE = "mysql:8.4";

    private static MySQLContainer container;

    private MySqlTestSupport() {
    }

    /** 把数据源配置注册到 Spring 环境。 */
    public static synchronized void registerDataSource(DynamicPropertyRegistry registry) {
        String externalUrl = setting("test.mysql.url", "TEST_MYSQL_URL", null);
        if (externalUrl != null) {
            String username = setting("test.mysql.username", "TEST_MYSQL_USERNAME", "kaoyan");
            String password = setting("test.mysql.password", "TEST_MYSQL_PASSWORD", "");
            log.info("集成测试使用外部 MySQL：url={} user={}", externalUrl, username);
            registry.add("spring.datasource.url", () -> externalUrl);
            registry.add("spring.datasource.username", () -> username);
            registry.add("spring.datasource.password", () -> password);
            return;
        }

        MySQLContainer started = container();
        registry.add("spring.datasource.url", started::getJdbcUrl);
        registry.add("spring.datasource.username", started::getUsername);
        registry.add("spring.datasource.password", started::getPassword);
    }

    private static synchronized MySQLContainer container() {
        if (container == null) {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                throw new IllegalStateException("""
                        集成测试需要真实 MySQL，但当前没有可用的 Docker。
                        请启动 Docker，或指向一次性测试库，例如：
                          $env:TEST_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/kaoyan_study_test?allowPublicKeyRetrieval=true&useSSL=false'
                          $env:TEST_MYSQL_USERNAME='...'; $env:TEST_MYSQL_PASSWORD='...'
                        """);
            }
            container = new MySQLContainer(MYSQL_IMAGE).withDatabaseName("kaoyan_study_test");
            container.start();
        }
        return container;
    }

    /** 优先读取系统属性，其次环境变量。 */
    private static String setting(String propertyName, String envName, String defaultValue) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
