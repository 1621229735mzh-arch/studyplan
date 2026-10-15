-- Time budgets do not invent a quantity. Completion still belongs to study records.
CREATE TABLE task_activity (
    task_id BIGINT NOT NULL PRIMARY KEY,
    mode VARCHAR(16) NOT NULL,
    description VARCHAR(1000) NULL,
    flexible_minutes INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_activity_task FOREIGN KEY(task_id) REFERENCES task(id) ON DELETE CASCADE,
    CONSTRAINT ck_activity_mode CHECK(mode IN ('TIMED_STUDY','RECAP')),
    CONSTRAINT ck_activity_flexible CHECK(flexible_minutes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE daily_plan_item MODIFY planned_amount DECIMAL(10,2) NULL;
ALTER TABLE study_record ADD COLUMN activity_completed BOOLEAN NOT NULL DEFAULT FALSE;
