-- Opt-in rotation: reference days choose today's required books; records remain the only completion facts.
ALTER TABLE study_continuation_stream
    ADD COLUMN reference_days_only TINYINT NOT NULL DEFAULT 0,
    ADD COLUMN available_from DATE NULL,
    ADD CONSTRAINT ck_continuation_reference_days CHECK(reference_days_only IN (0,1));
