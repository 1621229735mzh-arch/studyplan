-- Sequence metadata is not completion state. Only active study records complete an item.
CREATE TABLE task_sequence (
    task_id BIGINT NOT NULL PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    CONSTRAINT fk_sequence_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE task_sequence_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    position INT NOT NULL,
    item_key VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    title VARCHAR(200) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    UNIQUE KEY uk_sequence_position (task_id, position),
    UNIQUE KEY uk_sequence_key (task_id, item_key),
    CONSTRAINT fk_sequence_item_task FOREIGN KEY (task_id) REFERENCES task_sequence(task_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE study_record
    ADD COLUMN sequence_item_id BIGINT NULL,
    ADD KEY idx_record_sequence (sequence_item_id, deleted_at),
    ADD CONSTRAINT fk_record_sequence_item FOREIGN KEY (sequence_item_id) REFERENCES task_sequence_item(id);
