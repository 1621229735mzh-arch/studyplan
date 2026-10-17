-- Actual review quantity stays separate from first-time learning quantity.
ALTER TABLE task_activity DROP CHECK ck_activity_mode;
ALTER TABLE task_activity ADD CONSTRAINT ck_activity_mode CHECK(mode IN ('TIMED_STUDY','RECAP','CHECK_ONLY','CONTINUED_STUDY','COUNT_STUDY','COUNT_REVIEW'));
ALTER TABLE study_record ADD COLUMN review_amount DECIMAL(18,6) NULL;
ALTER TABLE study_record ADD CONSTRAINT ck_review_amount CHECK(review_amount IS NULL OR (review_amount > 0 AND amount IS NULL AND continuation_delta IS NULL));
