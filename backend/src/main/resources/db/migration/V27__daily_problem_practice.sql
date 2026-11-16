-- Explicitly enabled daily practice. Preserve all existing plans and learning records.
ALTER TABLE task_sequence MODIFY end_date DATE NULL;
ALTER TABLE task_sequence ADD COLUMN daily_from DATE NULL;
ALTER TABLE task_sequence ADD CONSTRAINT ck_sequence_daily_practice
    CHECK (daily_from IS NULL OR (mode = 'SELECT_BY_NUMBER' AND end_date IS NULL AND daily_from >= start_date));
