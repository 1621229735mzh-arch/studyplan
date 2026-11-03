ALTER TABLE review_item ADD COLUMN date_source VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN';

-- Do not guess whether historical dates were overridden by the user.
CREATE TABLE review_feedback_correction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_item_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL,
    previous_result VARCHAR(16) NOT NULL,
    result VARCHAR(16) NOT NULL,
    previous_date DATE NULL,
    next_review_date DATE NULL,
    scheduled_date DATE NULL,
    interval_days INT NULL,
    version BIGINT NOT NULL,
    client_token VARCHAR(80) NOT NULL UNIQUE,
    request_hash CHAR(64) NOT NULL,
    corrected_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_correction_item FOREIGN KEY (review_item_id) REFERENCES review_item(id),
    CONSTRAINT fk_correction_record FOREIGN KEY (record_id) REFERENCES review_record(id)
);
