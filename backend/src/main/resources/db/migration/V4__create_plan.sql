-- 计划模块：阶段目标 → 任务 → 周计划 → 每日安排。
-- 任务是被周计划与每日安排共同引用的事实，两边都不各自复制一份任务。

CREATE TABLE stage_goal (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject_id  BIGINT       NOT NULL COMMENT '所属科目',
    title       VARCHAR(200) NOT NULL COMMENT '目标标题',
    description VARCHAR(1000) NULL COMMENT '说明',
    start_date  DATE         NULL COMMENT '开始日期',
    target_date DATE         NULL COMMENT '目标完成日期',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ACHIEVED/ARCHIVED',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version     BIGINT       NOT NULL DEFAULT 0 COMMENT '数据版本，用于多设备编辑检查',
    PRIMARY KEY (id),
    KEY idx_stage_goal_subject (subject_id),
    CONSTRAINT fk_stage_goal_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '阶段目标';

CREATE TABLE task (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject_id     BIGINT        NOT NULL COMMENT '所属科目',
    stage_goal_id  BIGINT        NULL COMMENT '所属阶段目标，可空',
    title          VARCHAR(200)  NOT NULL COMMENT '任务标题',
    unit_id        BIGINT        NOT NULL COMMENT '计量单位',
    planned_amount DECIMAL(10, 2) NULL COMMENT '计划量，未确定时为空',
    status         VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DONE/ARCHIVED',
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version        BIGINT        NOT NULL DEFAULT 0 COMMENT '数据版本，用于多设备编辑检查',
    PRIMARY KEY (id),
    KEY idx_task_subject (subject_id),
    KEY idx_task_stage_goal (stage_goal_id),
    CONSTRAINT fk_task_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT fk_task_unit FOREIGN KEY (unit_id) REFERENCES study_unit (id),
    CONSTRAINT fk_task_stage_goal FOREIGN KEY (stage_goal_id) REFERENCES stage_goal (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务';

CREATE TABLE weekly_plan (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    week_start_date DATE         NOT NULL COMMENT '周一日期',
    note            VARCHAR(500) NULL COMMENT '备注',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_weekly_plan_week (week_start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '周计划';

CREATE TABLE weekly_plan_item (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    weekly_plan_id BIGINT        NOT NULL COMMENT '所属周计划',
    task_id        BIGINT        NOT NULL COMMENT '引用任务',
    planned_amount DECIMAL(10, 2) NOT NULL COMMENT '本周计划量',
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_weekly_plan_item (weekly_plan_id, task_id),
    KEY idx_weekly_plan_item_task (task_id),
    CONSTRAINT fk_weekly_plan_item_plan FOREIGN KEY (weekly_plan_id) REFERENCES weekly_plan (id) ON DELETE CASCADE,
    CONSTRAINT fk_weekly_plan_item_task FOREIGN KEY (task_id) REFERENCES task (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '周计划条目';

-- 每日安排。source 区分手动安排、由周计划下推、复习确认进入三种来源。
CREATE TABLE daily_plan_item (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_date      DATE          NOT NULL COMMENT '安排日期',
    task_id        BIGINT        NOT NULL COMMENT '引用任务',
    planned_amount DECIMAL(10, 2) NOT NULL COMMENT '当日计划量',
    source         VARCHAR(16)   NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/WEEKLY/REVIEW',
    review_item_id BIGINT        NULL COMMENT '复习来源时记录复习项 id（跨模块引用，不加外键）',
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version        BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_plan_item (plan_date, task_id, source),
    KEY idx_daily_plan_item_date (plan_date),
    KEY idx_daily_plan_item_task (task_id),
    CONSTRAINT fk_daily_plan_item_task FOREIGN KEY (task_id) REFERENCES task (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '每日安排';

-- 手动调整与后续智能建议应用的记录。apply_token 唯一，保证同一调整不会重复应用。
CREATE TABLE plan_adjustment (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    apply_token     VARCHAR(64)   NULL COMMENT '应用幂等令牌',
    plan_date       DATE          NULL COMMENT '涉及的日期',
    task_id         BIGINT        NULL COMMENT '涉及的任务',
    adjustment_type VARCHAR(32)   NOT NULL COMMENT '调整类型',
    before_amount   DECIMAL(10, 2) NULL COMMENT '调整前计划量',
    after_amount    DECIMAL(10, 2) NULL COMMENT '调整后计划量',
    reason          VARCHAR(500)  NULL COMMENT '调整原因',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_adjustment_token (apply_token),
    KEY idx_plan_adjustment_date (plan_date),
    CONSTRAINT fk_plan_adjustment_task FOREIGN KEY (task_id) REFERENCES task (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '计划调整记录';
