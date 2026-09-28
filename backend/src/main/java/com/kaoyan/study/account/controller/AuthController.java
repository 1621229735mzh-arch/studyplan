package com.kaoyan.study.account.controller;

import com.kaoyan.study.account.dto.CsrfTokenResponse;
import com.kaoyan.study.account.dto.CurrentUserResponse;
import com.kaoyan.study.account.dto.LoginRequest;
import com.kaoyan.study.common.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录、会话查询与退出。
 *
 * <p>退出由 {@code SecurityConfig} 中的 LogoutFilter 处理（POST /api/auth/logout），
 * 这里只提供登录、当前身份与 CSRF 令牌接口。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "账号", description = "预设账号登录、当前身份与 CSRF 令牌")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          CookieCsrfTokenRepository csrfTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /**
     * 读取（必要时生成）CSRF 令牌。
     *
     * <p>令牌同时写入 Cookie；返回的 {@code token} 是 Cookie 中的原始值，
     * 非浏览器客户端可以直接把它放进请求头。
     */
    @GetMapping("/csrf")
    @Operation(summary = "获取 CSRF 令牌")
    public CsrfTokenResponse csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.loadToken(request);
        if (token == null) {
            token = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(token, request, response);
        }
        return new CsrfTokenResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    /** 使用预设账号登录，成功后把认证信息写入服务端会话。 */
    @PostMapping("/login")
    @Operation(summary = "登录")
    public CurrentUserResponse login(@Valid @RequestBody LoginRequest body,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        } catch (AuthenticationException ex) {
            // 账号不存在与口令错误返回同一提示，不区分两种情况。
            throw new UnauthorizedException("BAD_CREDENTIALS", "账号或口令不正确");
        }

        // 防会话固定：认证成功后更换会话 ID，再保存认证信息。
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return CurrentUserResponse.authenticated(authentication.getName());
    }

    /** 查询当前身份。未登录时返回 {@code authenticated=false}，供前端路由守卫使用。 */
    @GetMapping("/me")
    @Operation(summary = "查询当前身份")
    public CurrentUserResponse me(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return CurrentUserResponse.authenticated(authentication.getName());
        }
        return CurrentUserResponse.anonymous();
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
