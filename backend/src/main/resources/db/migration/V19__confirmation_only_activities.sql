-- Completion confirmations have no quantity or time requirement; records remain the source of truth.
ALTER TABLE task_activity DROP CHECK ck_activity_mode;
ALTER TABLE task_activity ADD CONSTRAINT ck_activity_mode CHECK(mode IN ('TIMED_STUDY','RECAP','CHECK_ONLY'));
ALTER TABLE task_activity ADD CONSTRAINT ck_check_only_flexible CHECK(mode <> 'CHECK_ONLY' OR flexible_minutes = 0);
