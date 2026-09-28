-- 候选目标：院校、专业、参考链接与个人备注。

CREATE TABLE target_school (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    school_name VARCHAR(100)  NOT NULL COMMENT '院校名称',
    major_name  VARCHAR(100)  NULL COMMENT '专业名称',
    note        VARCHAR(1000) NULL COMMENT '个人备注',
    status      VARCHAR(16)   NOT NULL DEFAULT 'CANDIDATE' COMMENT 'CANDIDATE 候选 / CHOSEN 已定 / DROPPED 放弃',
    sort_order  INT           NOT NULL DEFAULT 0 COMMENT '排序',
    created_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version     BIGINT        NOT NULL DEFAULT 0 COMMENT '数据版本，用于多设备编辑检查',
    PRIMARY KEY (id),
    KEY idx_target_school_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '候选目标院校';

CREATE TABLE target_link (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    target_id  BIGINT       NOT NULL COMMENT '所属目标',
    url        VARCHAR(500) NOT NULL COMMENT '参考链接',
    label      VARCHAR(100) NULL COMMENT '链接名称',
    sort_order INT          NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_target_link_target (target_id),
    CONSTRAINT fk_target_link_target FOREIGN KEY (target_id) REFERENCES target_school (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '候选目标参考链接';
