package com.kaoyan.study.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 登录请求。 */
public record LoginRequest(
        @NotBlank(message = "请输入登录名")
        @Size(max = 64, message = "登录名过长")
        String username,

        @NotBlank(message = "请输入口令")
        @Size(max = 200, message = "口令过长")
        String password) {
}
