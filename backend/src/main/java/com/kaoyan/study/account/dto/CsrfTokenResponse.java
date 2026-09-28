package com.kaoyan.study.account.dto;

/**
 * CSRF 令牌。
 *
 * <p>令牌同时通过 Cookie 下发；此处返回名称与值，便于前端在登录等写操作中回填请求头。
 */
public record CsrfTokenResponse(String headerName, String parameterName, String token) {
}
