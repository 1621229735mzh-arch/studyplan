-- 导入本人 2027 考研数学一轮计划（张宇基础30讲 + 1000题）。
--
-- 这是单用户个人工作台的一次性数据迁移：
-- 1. 复用已有的“数学”“讲”“题”和同名阶段目标/任务；
-- 2. 复用已有周计划，且不覆盖本人已经写入的周备注；
-- 3. 只补充缺失的周计划条目与每日安排，不删除、不顺延已有内容；
-- 4. 复习项必须由真实学习记录产生，因此本迁移不预造复习数据。

INSERT INTO subject (name, category, color, sort_order, enabled)
SELECT '数学', '公共课', '#409EFF', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM subject WHERE name = '数学');

INSERT INTO study_unit (name, sort_order, enabled)
SELECT '讲', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM study_unit WHERE name = '讲');

INSERT INTO study_unit (name, sort_order, enabled)
SELECT '题', 20, 1
WHERE NOT EXISTS (SELECT 1 FROM study_unit WHERE name = '题');

SET @math_subject_id := (SELECT id FROM subject WHERE name = '数学' ORDER BY id LIMIT 1);
SET @lecture_unit_id := (SELECT id FROM study_unit WHERE name = '讲' ORDER BY id LIMIT 1);
SET @question_unit_id := (SELECT id FROM study_unit WHERE name = '题' ORDER BY id LIMIT 1);

INSERT INTO stage_goal (subject_id, title, description, start_date, target_date, status)
SELECT @math_subject_id,
       '2027考研数学一轮：张宇基础30讲＋1000题',
       '暂按数学一范围导入。14周完成高数、线代、概率基础30讲的数学一适用内容，并同步推进1000题；周日机动，未完成任务不自动顺延。',
       '2026-09-28', '2027-01-03', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM stage_goal
    WHERE subject_id = @math_subject_id
      AND title = '2027考研数学一轮：张宇基础30讲＋1000题'
);

SET @math_goal_id := (
    SELECT id
    FROM stage_goal
    WHERE subject_id = @math_subject_id
      AND title = '2027考研数学一轮：张宇基础30讲＋1000题'
    ORDER BY id
    LIMIT 1
);

CREATE TEMPORARY TABLE math_plan_task_seed (
    code           VARCHAR(8)    NOT NULL PRIMARY KEY,
    title          VARCHAR(200)  NOT NULL,
    unit_id        BIGINT        NOT NULL,
    planned_amount DECIMAL(10,2) NOT NULL
) ENGINE = MEMORY;

INSERT INTO math_plan_task_seed (code, title, unit_id, planned_amount)
VALUES
    ('H01', '高数基础30讲·第1讲 高等数学预备知识', @lecture_unit_id, 1),
    ('H02', '高数基础30讲·第2讲 数列极限', @lecture_unit_id, 1),
    ('H03', '高数基础30讲·第3讲 函数极限与连续性', @lecture_unit_id, 1),
    ('H04', '高数基础30讲·第4讲 一元函数微分学的概念与计算', @lecture_unit_id, 1),
    ('H05', '高数基础30讲·第5讲 一元函数微分学的几何应用', @lecture_unit_id, 1),
    ('H06', '高数基础30讲·第6讲 中值定理', @lecture_unit_id, 1),
    ('H07', '高数基础30讲·第7讲 零点问题与微分不等式', @lecture_unit_id, 1),
    ('H08', '高数基础30讲·第8讲 一元函数积分学的概念与计算', @lecture_unit_id, 1),
    ('H09', '高数基础30讲·第9讲 一元函数积分学的几何应用', @lecture_unit_id, 1),
    ('H10', '高数基础30讲·第10讲 积分等式与积分不等式', @lecture_unit_id, 1),
    ('H11', '高数基础30讲·第11讲 多元函数微分学', @lecture_unit_id, 1),
    ('H12', '高数基础30讲·第12讲 二重积分', @lecture_unit_id, 1),
    ('H13', '高数基础30讲·第13讲 常微分方程', @lecture_unit_id, 1),
    ('H14', '高数基础30讲·第14讲 无穷级数（数学一）', @lecture_unit_id, 1),
    ('H15', '高数基础30讲·第15讲 数学一、数学二专题内容', @lecture_unit_id, 1),
    ('H17', '高数基础30讲·第17讲 多元函数积分学基础（数学一）', @lecture_unit_id, 1),
    ('H18', '高数基础30讲·第18讲 三重积分、曲线曲面积分（数学一）', @lecture_unit_id, 1),
    ('L01', '线代基础30讲·第1讲 行列式', @lecture_unit_id, 1),
    ('L02', '线代基础30讲·第2讲 矩阵', @lecture_unit_id, 1),
    ('L03', '线代基础30讲·第3讲 向量组', @lecture_unit_id, 1),
    ('L04', '线代基础30讲·第4讲 线性方程组', @lecture_unit_id, 1),
    ('L05', '线代基础30讲·第5讲 特征值与特征向量', @lecture_unit_id, 1),
    ('L06', '线代基础30讲·第6讲 二次型', @lecture_unit_id, 1),
    ('P01', '概率基础30讲·第1讲 随机事件与概率', @lecture_unit_id, 1),
    ('P02', '概率基础30讲·第2讲 一维随机变量及其分布', @lecture_unit_id, 1),
    ('P03', '概率基础30讲·第3讲 多维随机变量及其分布', @lecture_unit_id, 1),
    ('P04', '概率基础30讲·第4讲 随机变量的数字特征', @lecture_unit_id, 1),
    ('P05', '概率基础30讲·第5讲 大数定律与中心极限定理', @lecture_unit_id, 1),
    ('P06', '概率基础30讲·第6讲 数理统计', @lecture_unit_id, 1),
    ('Q1000', '张宇《1000题》一轮（按对应章节推进）', @question_unit_id, 1000);

INSERT INTO task (subject_id, stage_goal_id, title, unit_id, planned_amount, status)
SELECT @math_subject_id, @math_goal_id, seed.title, seed.unit_id, seed.planned_amount, 'ACTIVE'
FROM math_plan_task_seed seed
WHERE NOT EXISTS (
    SELECT 1
    FROM task existing
    WHERE existing.stage_goal_id = @math_goal_id
      AND existing.title = seed.title
);

CREATE TEMPORARY TABLE math_plan_week_seed (
    week_start_date DATE          NOT NULL PRIMARY KEY,
    note            VARCHAR(500)  NOT NULL,
    question_amount DECIMAL(10,2) NOT NULL
) ENGINE = MEMORY;

INSERT INTO math_plan_week_seed (week_start_date, note, question_amount)
VALUES
    ('2026-09-28', '高数第1-2讲：预备知识、数列极限；周六完成本周订正与闭卷回忆，周日机动。', 60),
    ('2026-10-05', '高数第3讲：函数极限与连续；重点完成极限计算与连续性基础题，周日机动。', 70),
    ('2026-10-12', '高数第4-5讲：一元微分概念、计算与几何应用；周六验收，周日机动。', 80),
    ('2026-10-19', '高数第6-7讲：中值定理、零点问题与微分不等式；周六验收，周日机动。', 80),
    ('2026-10-26', '高数第8讲：一元积分的概念与计算；优先保证基本计算熟练度，周日机动。', 70),
    ('2026-11-02', '高数第9-10讲：积分几何应用、积分等式与不等式；周六验收，周日机动。', 80),
    ('2026-11-09', '高数第11-12讲：多元函数微分、二重积分；周六验收，周日机动。', 80),
    ('2026-11-16', '高数第13-14讲：常微分方程、无穷级数；周六验收，周日机动。', 70),
    ('2026-11-23', '高数第15、17讲：数学一/二专题、多元积分基础；跳过数学三专属第16讲。', 70),
    ('2026-11-30', '高数第18讲：三重积分、曲线曲面积分；本周同时完成高数框架总复盘。', 80),
    ('2026-12-07', '线代第1-3讲：行列式、矩阵、向量组；注意概念和计算同步。', 70),
    ('2026-12-14', '线代第4-6讲：线性方程组、特征值与特征向量、二次型；完成线代验收。', 70),
    ('2026-12-21', '概率第1-3讲：随机事件、一维随机变量、多维随机变量；结合例题理解分布。', 70),
    ('2026-12-28', '概率第4-6讲：数字特征、大数定律与中心极限定理、数理统计；完成一轮验收。', 50);

INSERT INTO weekly_plan (week_start_date, note)
SELECT seed.week_start_date, seed.note
FROM math_plan_week_seed seed
WHERE NOT EXISTS (
    SELECT 1 FROM weekly_plan existing WHERE existing.week_start_date = seed.week_start_date
);

UPDATE weekly_plan plan
JOIN math_plan_week_seed seed ON seed.week_start_date = plan.week_start_date
SET plan.note = seed.note
WHERE plan.note IS NULL;

CREATE TEMPORARY TABLE math_plan_week_task_seed (
    week_start_date DATE       NOT NULL,
    task_code       VARCHAR(8) NOT NULL,
    day_offset      TINYINT    NOT NULL,
    PRIMARY KEY (week_start_date, task_code)
) ENGINE = MEMORY;

INSERT INTO math_plan_week_task_seed (week_start_date, task_code, day_offset)
VALUES
    ('2026-09-28', 'H01', 0), ('2026-09-28', 'H02', 3),
    ('2026-10-05', 'H03', 0),
    ('2026-10-12', 'H04', 0), ('2026-10-12', 'H05', 3),
    ('2026-10-19', 'H06', 0), ('2026-10-19', 'H07', 3),
    ('2026-10-26', 'H08', 0),
    ('2026-11-02', 'H09', 0), ('2026-11-02', 'H10', 3),
    ('2026-11-09', 'H11', 0), ('2026-11-09', 'H12', 3),
    ('2026-11-16', 'H13', 0), ('2026-11-16', 'H14', 3),
    ('2026-11-23', 'H15', 0), ('2026-11-23', 'H17', 3),
    ('2026-11-30', 'H18', 0),
    ('2026-12-07', 'L01', 0), ('2026-12-07', 'L02', 2), ('2026-12-07', 'L03', 4),
    ('2026-12-14', 'L04', 0), ('2026-12-14', 'L05', 2), ('2026-12-14', 'L06', 4),
    ('2026-12-21', 'P01', 0), ('2026-12-21', 'P02', 2), ('2026-12-21', 'P03', 4),
    ('2026-12-28', 'P04', 0), ('2026-12-28', 'P05', 2), ('2026-12-28', 'P06', 4);

-- 每个讲次进入其所在周；同一个任务事实随后被每日安排继续引用。
INSERT INTO weekly_plan_item (weekly_plan_id, task_id, planned_amount)
SELECT plan.id, task.id, 1
FROM math_plan_week_task_seed schedule
JOIN math_plan_task_seed seed ON seed.code = schedule.task_code
JOIN task ON task.stage_goal_id = @math_goal_id AND task.title = seed.title
JOIN weekly_plan plan ON plan.week_start_date = schedule.week_start_date
WHERE NOT EXISTS (
    SELECT 1
    FROM weekly_plan_item existing
    WHERE existing.weekly_plan_id = plan.id
      AND existing.task_id = task.id
);

-- 1000题是一个跨周任务，各周计划量之和为1000题，不按周复制任务事实。
INSERT INTO weekly_plan_item (weekly_plan_id, task_id, planned_amount)
SELECT plan.id, task.id, week_seed.question_amount
FROM math_plan_week_seed week_seed
JOIN weekly_plan plan ON plan.week_start_date = week_seed.week_start_date
JOIN task ON task.stage_goal_id = @math_goal_id
         AND task.title = '张宇《1000题》一轮（按对应章节推进）'
WHERE NOT EXISTS (
    SELECT 1
    FROM weekly_plan_item existing
    WHERE existing.weekly_plan_id = plan.id
      AND existing.task_id = task.id
);

-- 新课安排在周一/周四，三讲周安排在周一/周三/周五。
INSERT INTO daily_plan_item (plan_date, task_id, planned_amount, source)
SELECT DATE_ADD(schedule.week_start_date, INTERVAL schedule.day_offset DAY), task.id, 1, 'WEEKLY'
FROM math_plan_week_task_seed schedule
JOIN math_plan_task_seed seed ON seed.code = schedule.task_code
JOIN task ON task.stage_goal_id = @math_goal_id AND task.title = seed.title
WHERE NOT EXISTS (
    SELECT 1
    FROM daily_plan_item existing
    WHERE existing.plan_date = DATE_ADD(schedule.week_start_date, INTERVAL schedule.day_offset DAY)
      AND existing.task_id = task.id
      AND existing.source = 'WEEKLY'
);

-- 1000题平均分配到周一至周六；周日保持机动。余数优先落在周初。
INSERT INTO daily_plan_item (plan_date, task_id, planned_amount, source)
SELECT DATE_ADD(week_seed.week_start_date, INTERVAL days.day_offset DAY),
       task.id,
       FLOOR(week_seed.question_amount / 6)
           + CASE WHEN days.day_offset < MOD(week_seed.question_amount, 6) THEN 1 ELSE 0 END,
       'WEEKLY'
FROM math_plan_week_seed week_seed
CROSS JOIN (
    SELECT 0 AS day_offset
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
) days
JOIN task ON task.stage_goal_id = @math_goal_id
         AND task.title = '张宇《1000题》一轮（按对应章节推进）'
WHERE NOT EXISTS (
    SELECT 1
    FROM daily_plan_item existing
    WHERE existing.plan_date = DATE_ADD(week_seed.week_start_date, INTERVAL days.day_offset DAY)
      AND existing.task_id = task.id
      AND existing.source = 'WEEKLY'
);

DROP TEMPORARY TABLE math_plan_week_task_seed;
DROP TEMPORARY TABLE math_plan_week_seed;
DROP TEMPORARY TABLE math_plan_task_seed;
