-- 日安排是时间视图的唯一排期来源。旧周计划和学习记录保持原状。
CREATE TABLE plan_workspace_revision (
    id INT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;
INSERT INTO plan_workspace_revision (id, version) VALUES (1, 0);

CREATE TABLE plan_import (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plan_key VARCHAR(64) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_plan_import_key (plan_key),
    UNIQUE KEY uk_plan_import_hash (content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE plan_period_outline (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    import_id BIGINT NOT NULL,
    period_type VARCHAR(8) NOT NULL,
    start_date DATE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    CONSTRAINT fk_plan_outline_import FOREIGN KEY (import_id) REFERENCES plan_import(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE daily_plan_item ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

CREATE TABLE plan_quick_submission (
    client_token VARCHAR(64) NOT NULL PRIMARY KEY,
    request_hash CHAR(64) NOT NULL,
    plan_date DATE NOT NULL
) ENGINE=InnoDB;
