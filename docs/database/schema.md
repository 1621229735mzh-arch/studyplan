# 数据模型与迁移

所有表结构变更都通过 Flyway 迁移管理，文件位于 `backend/src/main/resources/db/migration/`，
模块共用同一版本序列。已应用的迁移不再改写，后续变更新增版本。

## 迁移清单

| 版本 | 内容 |
| --- | --- |
| V1 | `account` 预设账号表 |
| V2 | Spring Session JDBC 会话表（`SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES`） |
| V3 | `subject`、`study_unit`、`study_settings` |
| V4 | `stage_goal`、`task`、`weekly_plan`、`weekly_plan_item`、`daily_plan_item`、`plan_adjustment` |
| V5 | `study_record` |
| V6 | `review_item`、`review_record` |
| V7 | `memo` |
| V8 | `target_school`、`target_link` |
| V9 | 调整任务相关外键的删除行为 |
| V10 | 复习间隔配置与复习项安排日期 |
| V11 | 允许“只记时长”的学习记录（`amount`、`unit_id`、`subject_id` 可空） |
| V12 | 一次性导入本人 2027 考研数学一轮计划（基础30讲、1000题、周计划与每日安排） |
| V13 | JDBC Session 表大小写归一化 |
| V14 | 今日清单用时与部分复习，学习量精度扩展 |
| V15 | 按日规划工作区及外部方案导入防重 |
| V16 | 课程目录、任务／学习记录归属、目录版本与导入预览 |

会话表 DDL 取自 `spring-session-jdbc` 官方 `schema-mysql.sql`，并关闭框架的自动建表
（`spring.session.jdbc.initialize-schema=never`），保证应用表与会话表可以用同一套迁移重复部署。

V12 是个人单用户数据迁移：复用已有科目、单位和同名计划数据，只补充缺失项，
不覆盖已有周备注。1000题按一个跨周任务建模；周计划和每日安排引用同一任务，
各周题量合计 1000。复习项仍须在产生真实学习记录后创建，迁移不会预造复习完成量。

## 核心表

### 任务与计划

- `task`：任务的唯一事实，含 `subject_id`、`unit_id`、`planned_amount`、`status`、`version`。
  周计划与每日安排都引用它，不各自复制一份。
- `weekly_plan` / `weekly_plan_item`：以周一日期标识一周，条目引用任务并记录本周计划量。
- `daily_plan_item`：某天的安排，`source` 区分手动、周计划下推与复习来源；
  `UNIQUE (plan_date, task_id, source)` 保证同一天同一任务同一来源只有一条。
- `plan_adjustment`：调整记录（调整前/后数量与原因），`apply_token` 唯一，用于一次性应用防重。

V15 新增计划工作区表：`plan_workspace_revision` 为日安排写操作和导入确认共用修订锁；`plan_import` 唯一方案 ID/内容哈希；`plan_period_outline` 保存月、周说明；`plan_quick_submission` 保存唯一提交令牌及请求指纹；`daily_plan_item.sort_order` 保存日期内排序。新条目追加到末尾，排序需检查当天完整条目和各版本。同日同任务跨来源聚合计划量，实际学习量只读取一次，排除 REVIEW。旧 weekly_plan 数据保留，新页面不从它另加一份计划量。

V15 已在独立测试库执行，不能改写；生产尚未发布。JSON 文件输入规范由 Java 校验，不把外部 AI 输出直接作为 SQL 执行。

### 学习记录

`study_record` 是完成量的唯一来源：

- `amount DECIMAL(14,6)`（V14 扩展精度）与 `unit_id` 记录数量与单位，**不同单位不参与同一聚合**。
- `amount`、`unit_id`、`subject_id` 均可为空，表示“只记时长”的记录（见
  [决策 0005](../decisions/0005-time-only-study-records.md)）：它计入学习时长，
  不计入任何单位的完成量。
- `client_token` 唯一，重复提交同一条记录不会生成第二行。
- `deleted_at` 软删除；所有统计都过滤 `deleted_at IS NULL`，因此修改/删除后统计自然重算。
- `version` 用于多设备编辑检查。

### 进度统计

实际完成量不另存一份，全部从未删除 `study_record` 推导，按科目及单位分组。旧 overview 的计划量来自日期区间内的 `daily_plan_item`，排除 REVIEW；新课程进度的总量来自完整目录的末级条目，不依赖排期。

V16 的目录及归属表：

- `progress_course`：方向、分科、名称、稳定 course_key、科目、单位。
- `progress_catalog_node`：课程内稳定 node_key、parent_key、名称、顺序与末级 total_amount。章节没有数量，末级必须有完整总量。
- `progress_task_link`：task_id 主键，每项任务唯一归属一个末级节点；任务删除时只清理该关联，不删除目录。
- `progress_record_link`：record_id 主键，显式归属优先，记录当前 task_id 防止后续换任务沿用旧归属。原记录不改变，软删除后查询排除它。
- `progress_catalog_revision`：目录写操作与确认导入共用行锁及修订号。
- `progress_catalog_preview`：持久化预览文本、上下文指纹、修订号、15 分钟期限及应用状态；同令牌确认防重。

目录 key 使用二进制排序规则，区分大小写。新增迁移不回写既有学习记录、任务或日安排。课程完成度对每个末级项分别封顶，实际累计保留原数量。

- 索引：`idx_study_record_date`、`idx_study_record_task`、`idx_study_record_subject`
  覆盖按日期、任务、科目过滤的常见查询。
- `SUM(planned_amount)` 保持可空语义：该单位下没有计划量时返回 NULL，而不是 0。

### 复习

- `review_item`：只针对已学内容创建（服务层校验存在学习记录）；`task_id` 唯一，避免重复加入。
  `status` 为 `PENDING`（待安排）/ `SCHEDULED`（已确认到某天）/ `ARCHIVED`。
  `next_review_date` 保留到期信息，积压不会自动提高强度。
- `review_record`：一次复习的反馈与据此得到的下次日期；`client_token` 唯一防重。
  复习记录写入本表，**不写入 `study_record`**，因此复习不会重复增加首次学习量。
- 间隔参数存在 `study_settings`（三个档位，可为空）。为空时不给出具体日期建议。

### 设置

`study_settings` 是固定单行表（`CHECK (id = 1)`）：

- `daily_study_minutes`、`daily_review_minutes` 可为空，表示本人尚未确定；
  业务层不会用臆造的默认值来宣称满足额度。
- `review_phase`：主学习 / 集中复习，由本人主动切换。

## 外键与删除行为

- 大类数据（科目、单位）被引用时**拒绝删除**，引导改为停用，避免历史记录失去归属。
- 有学习记录的任务**拒绝删除**（学习记录是既成事实），可改为归档。
- V9 让安排类数据随任务级联清理，并让 `review_item.task_id`、`plan_adjustment.task_id`
  在任务被删除时置空——复习项自带标题，历史复习记录仍然完整。
- `target_link` 随 `target_school` 级联删除。

## 本地查看

```bash
mysql -u kaoyan -p kaoyan_study \
  -e "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

V17：`task_sequence` 保存本期日期，`task_sequence_item` 保存有序 key/title/HTTPS url；`study_record.sequence_item_id` 外键保留每次实际题目身份。有效学习记录的 amount=1 决定完成；没有独立完成状态或推进游标。旧数据该列为空。
# V18：时间型安排

`task_activity` 保存任务的 TIMED_STUDY／RECAP 模式、说明与弹性分钟上限，不保存完成量。`daily_plan_item.planned_amount` 可为 NULL，表示只按用时排期；普通数量任务仍由业务校验要求计划量。`study_record.activity_completed` 标记一条有效学习记录结束了当天安排，不表示整讲内容完成。软删除后不再计入结束状态，实际数量与时长继续从学习记录实时汇总。

## V19：完成确认型安排

新增迁移 V19__confirmation_only_activities.sql 扩展 task_activity.mode 约束，允许 CHECK_ONLY，并约束其 flexible_minutes=0。任务和日安排计划量、预计用时由业务层强制为空。有效确认仍为 study_record，activity_completed=true，amount、duration_minutes为空；取消沿用软删除。任务行锁确保一天只有一份有效确认，revision纳入软删除历史防止状态重放。不新增独立完成量表，不改写V18。
