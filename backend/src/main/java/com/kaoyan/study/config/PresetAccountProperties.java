package com.kaoyan.study.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 预设账号配置。
 *
 * <p>口令来源为环境变量 {@code PRESET_ACCOUNT_PASSWORD}，仓库中不保存任何真实口令。
 * 仅在账号尚未创建时使用：账号已存在时不会覆盖既有口令。
 */
@ConfigurationProperties(prefix = "app.preset-account")
public class PresetAccountProperties {

    /** 预设登录名。 */
    private String username = "kaoyan";

    /** 首次初始化时使用的明文口令，来自环境变量；不回显、不落库。 */
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
