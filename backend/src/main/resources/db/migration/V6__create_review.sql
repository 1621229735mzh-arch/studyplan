-- 复习模块：只复习已经学过并且被加入复习的内容。
-- 建议由代码按掌握反馈与间隔参数计算；超出额度的内容留在待安排状态，不自动占用新学习时间。

CREATE TABLE review_item (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject_id        BIGINT       NOT NULL COMMENT '所属科目',
    task_id           BIGINT       NULL COMMENT '来源任务；同一任务只建立一个复习项',
    title             VARCHAR(200) NOT NULL COMMENT '复习内容标题',
    source_record_id  BIGINT       NULL COMMENT '来源学习记录',
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待安排 / SCHEDULED 已排入今日 / ARCHIVED 归档',
    next_review_date  DATE         NULL COMMENT '到期日期，待安排内容保留该信息',
    last_result       VARCHAR(16)  NULL COMMENT '最近一次掌握反馈：FORGOT/VAGUE/MASTERED',
    interval_days     INT          NULL COMMENT '当前复习间隔天数',
    estimated_minutes INT          NULL COMMENT '预计用时；缺失时不宣称符合额度',
    added_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入复习时间',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version           BIGINT       NOT NULL DEFAULT 0 COMMENT '数据版本，用于多设备编辑检查',
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_item_task (task_id),
    KEY idx_review_item_status (status),
    KEY idx_review_item_due (next_review_date),
    CONSTRAINT fk_review_item_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_review_item_task FOREIGN KEY (task_id) REFERENCES task (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '复习项';

CREATE TABLE review_record (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    review_item_id    BIGINT       NOT NULL COMMENT '所属复习项',
    review_date       DATE         NOT NULL COMMENT '复习日期',
    result            VARCHAR(16)  NOT NULL COMMENT '掌握反馈：FORGOT/VAGUE/MASTERED',
    duration_minutes  INT          NULL COMMENT '用时（分钟）',
    note              VARCHAR(500) NULL COMMENT '备注',
    next_review_date  DATE         NULL COMMENT '据此建议的下次日期，允许之后手动修改',
    interval_days     INT          NULL COMMENT '据此得到的间隔天数',
    client_token      VARCHAR(64)  NULL COMMENT '客户端提交令牌，用于重复提交防重',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_record_client_token (client_token),
    KEY idx_review_record_item (review_item_id),
    KEY idx_review_record_date (review_date),
    CONSTRAINT fk_review_record_item FOREIGN KEY (review_item_id) REFERENCES review_item (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '复习记录';
