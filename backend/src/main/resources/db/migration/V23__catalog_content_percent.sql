-- Content progress reads continuation percentages; it never invents quantity records.
ALTER TABLE progress_course
    ADD COLUMN progress_mode VARCHAR(24) NOT NULL DEFAULT 'QUANTITY',
    ADD CONSTRAINT ck_course_progress_mode CHECK (progress_mode IN ('QUANTITY', 'CONTENT_PERCENT'));
