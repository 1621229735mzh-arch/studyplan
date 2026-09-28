-- 调整任务相关外键的删除行为，使“删除任务”不依赖跨模块写操作：
--   * 安排类数据（周计划条目、每日安排、调整记录）随任务删除或解除引用；
--   * 复习项保留自身标题，仅在任务被删除时解除引用，复习历史不受影响；
--   * 学习记录仍是硬约束：有学习记录的任务不允许删除（见 TaskService）。
ALTER TABLE weekly_plan_item
    DROP FOREIGN KEY fk_weekly_plan_item_task;
ALTER TABLE weekly_plan_item
    ADD CONSTRAINT fk_weekly_plan_item_task FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE;

ALTER TABLE daily_plan_item
    DROP FOREIGN KEY fk_daily_plan_item_task;
ALTER TABLE daily_plan_item
    ADD CONSTRAINT fk_daily_plan_item_task FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE;

ALTER TABLE plan_adjustment
    DROP FOREIGN KEY fk_plan_adjustment_task;
ALTER TABLE plan_adjustment
    ADD CONSTRAINT fk_plan_adjustment_task FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE SET NULL;

ALTER TABLE review_item
    DROP FOREIGN KEY fk_review_item_task;
ALTER TABLE review_item
    ADD CONSTRAINT fk_review_item_task FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE SET NULL;
