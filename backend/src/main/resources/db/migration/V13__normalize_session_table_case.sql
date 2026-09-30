-- Windows MySQL (lower_case_table_names=1) exports lowercase table names.
-- Linux (0) needs the uppercase names used by Spring Session's SQL.
-- Keep V2 unchanged. Rename only existing lowercase tables, preserving rows/FKs.
-- With case-insensitive table names, or a fresh Linux schema, this is a no-op.
-- A single atomic RENAME fails safely if both cases exist; never drop/merge tables.
SET @session_case_renames = (
    SELECT GROUP_CONCAT(
        CASE
            WHEN BINARY table_name = BINARY 'spring_session'
                THEN '`spring_session` TO `SPRING_SESSION`'
            WHEN BINARY table_name = BINARY 'spring_session_attributes'
                THEN '`spring_session_attributes` TO `SPRING_SESSION_ATTRIBUTES`'
        END ORDER BY table_name SEPARATOR ', '
    )
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND @@lower_case_table_names = 0
      AND (BINARY table_name = BINARY 'spring_session'
           OR BINARY table_name = BINARY 'spring_session_attributes')
);
SET @session_case_sql = IF(@session_case_renames IS NULL,
    'SELECT 1', CONCAT('RENAME TABLE ', @session_case_renames));
PREPARE session_case_statement FROM @session_case_sql;
EXECUTE session_case_statement;
DEALLOCATE PREPARE session_case_statement;
