package com.kaoyan.study.account.service;

import com.kaoyan.study.account.entity.Account;
import com.kaoyan.study.account.mapper.AccountMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 账号用例。
 *
 * <p>本系统只有预设账号、不开放注册，因此这里不提供注册入口，
 * 仅提供登录时需要的读取与首次启动时的账号初始化。
 */
@Service
public class AccountService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountMapper accountMapper, PasswordEncoder passwordEncoder) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Account> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(accountMapper.findByUsername(username));
    }

    public boolean hasAnyAccount() {
        return accountMapper.countAll() > 0;
    }

    /**
     * 创建预设账号，口令以 BCrypt 哈希保存。
     *
     * @return 是否实际创建（账号已存在时返回 false，不覆盖已有口令）
     */
    @Transactional
    public boolean createPresetAccount(String username, String rawPassword) {
        if (accountMapper.findByUsername(username) != null) {
            return false;
        }
        Account account = new Account();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setEnabled(true);
        accountMapper.insert(account);
        return true;
    }

    /** 修改口令。用于账号维护，不暴露未受保护的接口。 */
    @Transactional
    public void changePassword(Long accountId, String rawPassword) {
        accountMapper.updatePasswordHash(accountId, passwordEncoder.encode(rawPassword));
    }
}
