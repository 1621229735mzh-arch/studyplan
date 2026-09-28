-- 允许“只记时长”的学习记录。
--
-- PLAN 二.2：允许计划外学习记录，“关联目标后再计入对应进度”；
-- 且学习时长与内容完成量是两个分别展示的维度。因此一条记录可以只记录用时，
-- 不带完成量、不关联科目与单位：它计入时长，但不计入任何单位的完成量。
ALTER TABLE study_record
    MODIFY COLUMN subject_id BIGINT NULL COMMENT '所属科目；只记时长时可空',
    MODIFY COLUMN unit_id BIGINT NULL COMMENT '计量单位；只记时长时可空',
    MODIFY COLUMN amount DECIMAL(10, 2) NULL COMMENT '完成量；为空表示未记录完成量，不计入任何单位的进度';

-- 统计只对“有单位的记录”按单位聚合，这里补一个覆盖索引列顺序的提示。
-- （索引 idx_study_record_date 已覆盖按日期过滤的场景，无需新增索引。）
