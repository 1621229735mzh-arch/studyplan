-- V13 restores the exact already-deployed session normalization migration.
ALTER TABLE daily_plan_item ADD COLUMN estimated_minutes INT NULL;
-- Percent-based recording requires more precision than the original two decimal places.
ALTER TABLE study_record MODIFY COLUMN amount DECIMAL(14, 6) NULL;
ALTER TABLE review_record
    MODIFY COLUMN result VARCHAR(16) NULL,
    ADD COLUMN progress_delta DECIMAL(5, 2) NOT NULL DEFAULT 100,
    ADD COLUMN estimated_minutes INT NULL;
