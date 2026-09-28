-- 设置模块：科目、计量单位与学习/复习预算配置。
-- 预算与复习额度都属于待定项，只做配置存储，不在代码中固定为产品规则。

CREATE TABLE subject (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(64)  NOT NULL COMMENT '科目名称，如 数据结构',
    category   VARCHAR(32)  NULL COMMENT '分类，如 专业课/公共课',
    color      VARCHAR(16)  NULL COMMENT '展示颜色',
    sort_order INT          NOT NULL DEFAULT 0 COMMENT '排序',
    enabled    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_subject_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '科目';

-- 计量单位是全局词表（题/讲/章/页……）；不同单位不直接相加。
CREATE TABLE study_unit (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(32) NOT NULL COMMENT '单位名称，如 讲',
    sort_order INT         NOT NULL DEFAULT 0 COMMENT '排序',
    enabled    TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_study_unit_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '计量单位';

-- 单用户设置，固定一行（id = 1）。字段可为空表示尚未确定，不臆造默认额度。
CREATE TABLE study_settings (
    id                  TINYINT     NOT NULL DEFAULT 1 COMMENT '固定为 1',
    exam_date           DATE        NULL COMMENT '考试时间',
    daily_study_minutes INT         NULL COMMENT '每日学习预算（分钟）',
    daily_review_minutes INT        NULL COMMENT '每日复习额度（分钟）',
    review_phase        VARCHAR(16) NOT NULL DEFAULT 'MAIN' COMMENT '复习阶段：MAIN 主学习 / INTENSIVE 集中复习',
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version             BIGINT      NOT NULL DEFAULT 0 COMMENT '数据版本，用于多设备编辑检查',
    PRIMARY KEY (id),
    CONSTRAINT ck_study_settings_single_row CHECK (id = 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学习设置';
