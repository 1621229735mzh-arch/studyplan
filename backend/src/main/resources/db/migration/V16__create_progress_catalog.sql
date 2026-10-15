-- 目录及归属只保存结构；实际完成量始终由未删除的 study_record 推导。
CREATE TABLE progress_catalog_revision (
    id TINYINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;
INSERT INTO progress_catalog_revision(id, version) VALUES (1, 0);

CREATE TABLE progress_course (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_key VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    direction VARCHAR(16) NOT NULL,
    branch VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    subject_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL,
    UNIQUE KEY uk_progress_course_key(course_key),
    CONSTRAINT fk_progress_course_subject FOREIGN KEY(subject_id) REFERENCES subject(id),
    CONSTRAINT fk_progress_course_unit FOREIGN KEY(unit_id) REFERENCES study_unit(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE progress_catalog_node (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    node_key VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    parent_key VARCHAR(64) COLLATE utf8mb4_bin NULL,
    title VARCHAR(200) NOT NULL,
    total_amount DECIMAL(18,6) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_progress_node_key(course_id,node_key),
    CONSTRAINT fk_progress_node_course FOREIGN KEY(course_id) REFERENCES progress_course(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE progress_task_link (
    task_id BIGINT NOT NULL PRIMARY KEY,
    node_id BIGINT NOT NULL,
    CONSTRAINT fk_progress_task_task FOREIGN KEY(task_id) REFERENCES task(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_task_node FOREIGN KEY(node_id) REFERENCES progress_catalog_node(id)
) ENGINE=InnoDB;

CREATE TABLE progress_record_link (
    record_id BIGINT NOT NULL PRIMARY KEY,
    node_id BIGINT NOT NULL,
    source_task_id BIGINT NULL,
    CONSTRAINT fk_progress_record_record FOREIGN KEY(record_id) REFERENCES study_record(id),
    CONSTRAINT fk_progress_record_node FOREIGN KEY(node_id) REFERENCES progress_catalog_node(id)
) ENGINE=InnoDB;

CREATE TABLE progress_catalog_preview (
    token CHAR(36) NOT NULL PRIMARY KEY,
    revision BIGINT NOT NULL,
    payload MEDIUMTEXT NOT NULL,
    context_hash CHAR(64) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    applied TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;
