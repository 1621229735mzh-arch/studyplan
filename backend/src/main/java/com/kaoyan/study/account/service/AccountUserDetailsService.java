package com.kaoyan.study.account.service;

import com.kaoyan.study.account.entity.Account;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 按登录名加载账号，供 Spring Security 认证使用。
 *
 * <p>账号不存在与账号停用都抛出同一异常，避免向调用方泄露账号是否存在。
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountService accountService;

    public AccountUserDetailsService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("账号或口令不正确"));
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .disabled(!account.isEnabled())
                .roles("USER")
                .build();
    }
}
