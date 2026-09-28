package com.kaoyan.study.account.mapper;

import com.kaoyan.study.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 账号数据访问。SQL 见 {@code resources/mapper/account/AccountMapper.xml}。 */
@Mapper
public interface AccountMapper {

    Account findByUsername(@Param("username") String username);

    long countAll();

    int insert(Account account);

    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);
}
