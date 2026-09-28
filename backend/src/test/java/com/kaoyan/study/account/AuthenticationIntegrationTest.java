package com.kaoyan.study.account;

import com.kaoyan.study.account.service.AccountService;
import com.kaoyan.study.support.HttpTestClient;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录、会话与 CSRF 的端到端验证：真实 MySQL + 真实过滤器链。
 *
 * <p>覆盖验收点：未登录不能访问学习数据；口令以哈希保存；会话保存在 MySQL；
 * 写操作缺少 CSRF 令牌时被拒绝。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("账号：登录、会话与 CSRF")
class AuthenticationIntegrationTest {

    private static final String USERNAME = "kaoyan";
    private static final String PASSWORD = "test-only-password";

    @LocalServerPort
    private int port;

    @Autowired
    private AccountService accountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private HttpTestClient client;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username", () -> USERNAME);
        registry.add("app.preset-account.password", () -> PASSWORD);
    }

    @BeforeEach
    void setUp() {
        client = new HttpTestClient("http://127.0.0.1:" + port);
        // 外部测试库可能保留上次运行的数据，统一重置为已知口令，保证测试可重复。
        accountService.findByUsername(USERNAME)
                .ifPresent(account -> accountService.changePassword(account.getId(), PASSWORD));
    }

    @Test
    @DisplayName("预设账号已创建且口令以 BCrypt 哈希保存")
    void presetAccountIsCreatedWithHashedPassword() {
        var account = accountService.findByUsername(USERNAME);
        assertThat(account).isPresent();
        assertThat(account.get().getPasswordHash())
                .as("口令不得以明文保存")
                .isNotEqualTo(PASSWORD)
                .startsWith("$2");
    }

    @Test
    @DisplayName("CSRF 接口下发令牌，并写入同名 Cookie")
    void csrfEndpointReturnsToken() {
        HttpTestClient.Response response = client.get("/api/auth/csrf");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.string("$.headerName")).isEqualTo("X-XSRF-TOKEN");
        assertThat(response.string("$.token")).isNotBlank();
    }

    @Test
    @DisplayName("缺少 CSRF 令牌的登录请求被拒绝")
    void loginWithoutCsrfTokenIsRejected() {
        HttpTestClient.Response response = client.post("/api/auth/login", loginBody(PASSWORD), null);

        assertThat(response.status()).isEqualTo(403);
    }

    @Test
    @DisplayName("口令错误返回 401 且不建立会话")
    void loginWithWrongPasswordFails() {
        String token = client.fetchCsrfToken();
        long sessionsBefore = countSessions();

        HttpTestClient.Response response = client.post("/api/auth/login", loginBody("wrong-password"), token);

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.string("$.code")).isEqualTo("BAD_CREDENTIALS");
        assertThat(countSessions()).isEqualTo(sessionsBefore);
    }

    @Test
    @DisplayName("登录成功后会话写入 MySQL，并可读取当前身份")
    void loginStoresSessionInDatabase() {
        String token = client.fetchCsrfToken();

        HttpTestClient.Response login = client.post("/api/auth/login", loginBody(PASSWORD), token);
        assertThat(login.status()).isEqualTo(200);
        assertThat(login.string("$.username")).isEqualTo(USERNAME);
        assertThat(login.bool("$.authenticated")).isEqualTo(true);

        HttpTestClient.Response me = client.get("/api/auth/me");
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.string("$.username")).isEqualTo(USERNAME);

        Integer stored = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?", Integer.class, USERNAME);
        assertThat(stored).as("会话应保存在 MySQL 的 SPRING_SESSION 表中").isNotNull().isPositive();
    }

    @Test
    @DisplayName("未登录时业务接口返回 401")
    void businessApiRequiresAuthentication() {
        HttpTestClient.Response response = client.get("/api/plan/tasks");

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.string("$.code")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("未登录时当前身份为匿名")
    void currentUserIsAnonymousBeforeLogin() {
        HttpTestClient.Response response = client.get("/api/auth/me");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.bool("$.authenticated")).isEqualTo(false);
    }

    @Test
    @DisplayName("退出后会话失效，业务接口重新返回 401")
    void logoutInvalidatesSession() {
        String token = client.fetchCsrfToken();
        client.post("/api/auth/login", loginBody(PASSWORD), token);

        HttpTestClient.Response logout = client.post("/api/auth/logout", "", token);
        assertThat(logout.status()).isEqualTo(204);

        assertThat(client.get("/api/auth/me").bool("$.authenticated")).isEqualTo(false);
        assertThat(client.get("/api/plan/tasks").status()).isEqualTo(401);
    }

    private String loginBody(String password) {
        return """
                {"username":"%s","password":"%s"}""".formatted(USERNAME, password);
    }

    private long countSessions() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Long.class);
        return count == null ? 0 : count;
    }
}
