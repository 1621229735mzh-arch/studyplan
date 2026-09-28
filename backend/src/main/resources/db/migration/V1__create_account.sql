-- 预设账号（不开放注册）。
-- 口令只保存 BCrypt 哈希；初始账号由应用启动时的初始化器写入，迁移只负责表结构。
CREATE TABLE account (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(64)  NOT NULL COMMENT '登录名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 口令哈希',
    enabled       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可用：1 可用，0 停用',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '预设账号';
