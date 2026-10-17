-- Explicit opt-in; existing ordered plans retain their behavior and history.
ALTER TABLE task_sequence
    ADD COLUMN mode VARCHAR(32) NOT NULL DEFAULT 'COMPLETE_THEN_ADVANCE',
    ADD CONSTRAINT ck_sequence_mode CHECK (mode IN ('COMPLETE_THEN_ADVANCE', 'SELECT_BY_NUMBER'));
