CREATE TABLE learning_time_draft (
    username VARCHAR(64) NOT NULL,
    context_key VARCHAR(40) NOT NULL,
    started_at DATETIME NOT NULL,
    revision CHAR(36) NOT NULL,
    PRIMARY KEY (username, context_key),
    FOREIGN KEY (username) REFERENCES account(username) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
