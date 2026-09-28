# 0004 复习安排记在复习项上，不塞进每日安排表

日期：2026-09-22
状态：已采纳

## 背景

PLAN 要求“建议经本人确认后才进入今日安排”，同时要求今日页面分别展示
**学习任务**与**已确认的复习任务**。

`daily_plan_item.task_id` 是 NOT NULL 且外键指向 `task`，而复习内容不一定来自任务：
PLAN 允许计划外学习，复习项也需要支持没有任务的自学内容。

## 备选方案

1. 确认复习时为内容自动建一个任务，再写 `daily_plan_item`。
   —— 会在任务列表里堆出“复习：xxx”的伪任务，污染计划数据。
2. 放宽 `daily_plan_item.task_id` 为可空并区分来源。
   —— 让“每日安排”同时承载两种语义，统计与校验都要加分支。
3. **在 `review_item` 上记录安排**（采纳）。

## 决定

- `review_item` 增加 `scheduled_date`，`status` 从 `PENDING` 变为 `SCHEDULED` 即表示
  “已确认排入这一天”；归档为 `ARCHIVED`。
- 确认接口按当日复习额度逐条判断：放得下的置为 `SCHEDULED`，放不下的**留在 `PENDING`**
  并返回原因，到期信息（`next_review_date`）保留。
- 今日视图分别返回 `plannedTasks`（学习任务）与 `reviewTasks`（已确认复习），二者不混。

## 影响

- 复习安排与学习任务在数据上分离，符合 PLAN 中“复习不重复增加首次学习量”，
  也不会出现伪任务。
- `daily_plan_item` 中保留的 `source='REVIEW'` 与 `review_item_id` 列在第一版未被写入；
  它们为将来“某条复习同时属于某任务计划”的扩展留位置，不参与当前逻辑。
- 已由 `ReviewFlowIntegrationTest.overQuotaItemsStayPending` 验证超额内容留在待安排列表。
