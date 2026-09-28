package com.kaoyan.study.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * 面向前后端分离（Cookie + 请求头）的 CSRF 处理器。
 *
 * <p>默认的 {@link XorCsrfTokenRequestAttributeHandler} 会对渲染到页面的令牌做掩码，
 * 前端从 Cookie 读到的却是原始令牌，两者不一致会导致写操作全部 403。
 * 这里按 Spring Security 官方推荐做法：请求头提交时按原始值校验，
 * 其它提交方式仍走 XOR 掩码，保留 BREACH 防护。
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        this.xor.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        // 请求头来自 Cookie 中的原始令牌，因此用明文校验；表单参数仍按掩码校验。
        return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
                .resolveCsrfTokenValue(request, csrfToken);
    }
}
