-- Opt-in per continuation book. No personal policy or learning/review facts are seeded.
CREATE TABLE review_auto_policy (
    stream_id BIGINT NOT NULL PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_review_auto_stream FOREIGN KEY (stream_id) REFERENCES study_continuation_stream(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE review_fsrs_state (
    review_item_id BIGINT NOT NULL PRIMARY KEY,
    stream_id BIGINT NOT NULL,
    completed_on DATE NOT NULL,
    source_valid BOOLEAN NOT NULL DEFAULT TRUE,
    paused BOOLEAN NOT NULL DEFAULT FALSE,
    stability DOUBLE NULL,
    difficulty DOUBLE NULL,
    last_review_date DATE NULL,
    repetitions INT NOT NULL DEFAULT 0,
    algorithm VARCHAR(32) NOT NULL DEFAULT 'FSRS4ANKI_5_0_0',
    CONSTRAINT fk_fsrs_item FOREIGN KEY (review_item_id) REFERENCES review_item(id),
    CONSTRAINT fk_fsrs_stream FOREIGN KEY (stream_id) REFERENCES study_continuation_stream(id),
    CONSTRAINT ck_fsrs_memory CHECK ((stability IS NULL AND difficulty IS NULL AND last_review_date IS NULL AND repetitions=0)
        OR (stability>0 AND difficulty BETWEEN 1 AND 10 AND last_review_date IS NOT NULL AND repetitions>0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE review_record ADD COLUMN submission_hash VARCHAR(64) NULL;
