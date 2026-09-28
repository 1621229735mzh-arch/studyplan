-- 备忘录：随手记，可关联科目、到期提醒与转任务。

CREATE TABLE memo (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject_id        BIGINT        NULL COMMENT '关联科目，可空',
    content           VARCHAR(1000) NOT NULL COMMENT '内容',
    due_date          DATE          NULL COMMENT '到期日期，用于站内提醒',
    status            VARCHAR(16)   NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/DONE',
    converted_task_id BIGINT        NULL COMMENT '转成的任务',
    reminder_dismissed TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已忽略本次到期提醒',
    created_at        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version           BIGINT        NOT NULL DEFAULT 0 COMMENT '数据版本，用于多设备编辑检查',
    PRIMARY KEY (id),
    KEY idx_memo_status (status),
    KEY idx_memo_due_date (due_date),
    CONSTRAINT fk_memo_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_memo_task FOREIGN KEY (converted_task_id) REFERENCES task (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '备忘录';
