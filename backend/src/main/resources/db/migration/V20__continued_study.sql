-- Explicit per-book continuation. Learning records remain the only progress facts.
ALTER TABLE task_activity DROP CHECK ck_activity_mode;
ALTER TABLE task_activity ADD CONSTRAINT ck_activity_mode CHECK(mode IN ('TIMED_STUDY','RECAP','CHECK_ONLY','CONTINUED_STUDY'));
ALTER TABLE study_record ADD COLUMN continuation_delta DECIMAL(5,2) NULL;
ALTER TABLE study_record ADD CONSTRAINT ck_continuation_delta CHECK(continuation_delta IS NULL OR (continuation_delta > 0 AND continuation_delta <= 100 AND amount IS NULL));
CREATE TABLE study_continuation_stream (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    import_id BIGINT NOT NULL,
    stream_key VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    UNIQUE KEY uk_continuation_stream(import_id,stream_key),
    CONSTRAINT fk_continuation_import FOREIGN KEY(import_id) REFERENCES plan_import(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE task_continuation (
    task_id BIGINT NOT NULL PRIMARY KEY,
    stream_id BIGINT NOT NULL,
    position_index INT NOT NULL,
    UNIQUE KEY uk_continuation_position(stream_id,position_index),
    CONSTRAINT fk_continuation_task FOREIGN KEY(task_id) REFERENCES task(id) ON DELETE CASCADE,
    CONSTRAINT fk_continuation_stream FOREIGN KEY(stream_id) REFERENCES study_continuation_stream(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
