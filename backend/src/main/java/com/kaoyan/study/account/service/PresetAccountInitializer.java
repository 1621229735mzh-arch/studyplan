package com.kaoyan.study.account.service;

import com.kaoyan.study.config.PresetAccountProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 首次启动时创建预设账号。
 *
 * <p>系统不开放注册，账号只能由这里按配置创建。口令只以 BCrypt 哈希落库。
 * 账号已存在时不做任何修改，避免每次重启覆盖正在使用的口令。
 */
@Component
public class PresetAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PresetAccountInitializer.class);

    private final AccountService accountService;
    private final PresetAccountProperties properties;

    public PresetAccountInitializer(AccountService accountService, PresetAccountProperties properties) {
        this.accountService = accountService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = properties.getUsername();
        if (accountService.findByUsername(username).isPresent()) {
            log.info("预设账号 {} 已存在，跳过初始化", username);
            return;
        }
        if (!StringUtils.hasText(properties.getPassword())) {
            // 没有可用口令时宁可启动失败，也不要创建一个口令为空或默认口令的账号。
            throw new IllegalStateException(
                    "尚未创建预设账号，请先设置环境变量 PRESET_ACCOUNT_PASSWORD 后再启动；"
                            + "该口令仅用于首次初始化，之后以 BCrypt 哈希保存在数据库中。");
        }
        boolean created = accountService.createPresetAccount(username, properties.getPassword());
        if (created) {
            log.info("已创建预设账号 {}", username);
        }
    }
}
