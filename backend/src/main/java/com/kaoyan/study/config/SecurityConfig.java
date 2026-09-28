package com.kaoyan.study.config;

import com.kaoyan.study.account.service.AccountUserDetailsService;
import com.kaoyan.study.common.api.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * 安全配置：预设账号 + 服务端会话（会话内容存 MySQL）+ CSRF 防护。
 *
 * <p>不开放注册，也不引入 Basic 认证或表单跳转登录：登录接口返回 JSON，
 * 未认证访问业务接口一律得到 401 JSON，而不是重定向到登录页。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** CSRF Cookie 名称，前端按此名称读取并把值放入请求头。 */
    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    /** CSRF 请求头名称。 */
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AccountUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * 会话中的认证信息仓库。登录接口显式保存上下文，因此要求显式保存（requireExplicitSave），
     * 避免在接口内部临时修改认证状态时被静默持久化。
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName(CSRF_COOKIE_NAME);
        repository.setHeaderName(CSRF_HEADER_NAME);
        repository.setCookiePath("/");
        return repository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository,
                                                   CookieCsrfTokenRepository csrfTokenRepository,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        // 登录相关与健康检查无需认证；其余 /api 一律要求已登录。
                        .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/me").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(objectMapper, response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED", "请先登录", request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(objectMapper, response, HttpServletResponse.SC_FORBIDDEN,
                                        "FORBIDDEN", "没有访问该资源的权限", request.getRequestURI())))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("SESSION")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
        return http.build();
    }

    private static void writeError(ObjectMapper objectMapper, HttpServletResponse response, int status,
                                   String code, String message, String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(code, message, path));
    }
}
