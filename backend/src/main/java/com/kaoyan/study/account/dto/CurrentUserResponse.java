package com.kaoyan.study.account.dto;

/** 当前登录身份。不含任何口令或哈希信息。 */
public record CurrentUserResponse(String username, boolean authenticated) {

    public static CurrentUserResponse authenticated(String username) {
        return new CurrentUserResponse(username, true);
    }

    public static CurrentUserResponse anonymous() {
        return new CurrentUserResponse(null, false);
    }
}
