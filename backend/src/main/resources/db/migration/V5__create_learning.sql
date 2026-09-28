-- 学习记录：完成量的唯一依据。创建计划不会增加完成量，只有这里的记录会。
-- 进度统计全部由本表推导，因此修改/删除记录后统计自然重算。

CREATE TABLE study_record (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id          BIGINT         NULL COMMENT '关联任务；计划外学习时为空',
    subject_id       BIGINT         NOT NULL COMMENT '所属科目',
    unit_id          BIGINT         NOT NULL COMMENT '计量单位，必须与任务单位一致',
    record_date      DATE           NOT NULL COMMENT '学习日期',
    amount           DECIMAL(10, 2) NOT NULL COMMENT '完成量，必须大于 0',
    duration_minutes INT            NULL COMMENT '用时（分钟），未记录时为空',
    content          VARCHAR(500)   NULL COMMENT '学习内容',
    note             VARCHAR(1000)  NULL COMMENT '简短复盘',
    client_token     VARCHAR(64)    NULL COMMENT '客户端提交令牌，用于重复提交防重',
    version          BIGINT         NOT NULL DEFAULT 0 COMMENT '数据版本，用于多设备编辑检查',
    created_at       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at       DATETIME(3)    NULL COMMENT '软删除时间；统计只计入未删除记录',
    PRIMARY KEY (id),
    UNIQUE KEY uk_study_record_client_token (client_token),
    KEY idx_study_record_date (record_date),
    KEY idx_study_record_task (task_id),
    KEY idx_study_record_subject (subject_id),
    CONSTRAINT fk_study_record_task FOREIGN KEY (task_id) REFERENCES task (id),
    CONSTRAINT fk_study_record_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_study_record_unit FOREIGN KEY (unit_id) REFERENCES study_unit (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学习记录';
