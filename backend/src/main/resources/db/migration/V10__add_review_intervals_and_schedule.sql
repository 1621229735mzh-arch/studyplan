-- 复习间隔参数属于 PLAN 中明确保留的待定事项，因此不写死在代码里：
-- 三个反馈档位的间隔天数保存在设置中，未设置时系统不给出具体日期建议。
ALTER TABLE study_settings
    ADD COLUMN review_interval_forgot_days   INT NULL COMMENT '反馈“不会”后的建议间隔天数，未设置为空',
    ADD COLUMN review_interval_vague_days    INT NULL COMMENT '反馈“模糊”后的建议间隔天数，未设置为空',
    ADD COLUMN review_interval_mastered_days INT NULL COMMENT '反馈“掌握”后的建议间隔天数，未设置为空';

-- 复习确认后的安排单独记录在复习项上：
-- 复习内容不一定关联任务，不能强行塞进要求 task_id 的每日安排表。
ALTER TABLE review_item
    ADD COLUMN scheduled_date DATE NULL COMMENT '已确认安排的复习日期',
    ADD KEY idx_review_item_scheduled (scheduled_date);
