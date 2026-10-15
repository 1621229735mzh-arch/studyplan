# 接口约定

## 通用规则

- 统一前缀 `/api`，HTTP/JSON。
- 请求与响应都是明确的 DTO（Java record），不暴露数据库实体。
- 除登录相关接口外，所有 `/api/**` 都需要已登录会话；未登录返回 401：
  `{"code":"UNAUTHORIZED","message":"请先登录","path":"...","timestamp":"...","fieldErrors":[]}`
- 写操作（POST/PUT/PATCH/DELETE）需要携带 CSRF 令牌：
  1. `GET /api/auth/csrf` 取得令牌（同时写入 `XSRF-TOKEN` Cookie）；
  2. 后续写操作把该值放入请求头 `X-XSRF-TOKEN`。
- 错误响应统一结构：

```json
{
  "code": "CONFLICT",
  "message": "该任务已在其他设备上修改，请刷新后重试",
  "path": "/api/plan/tasks/1",
  "timestamp": "2026-09-22T10:00:00Z",
  "fieldErrors": [{ "field": "title", "message": "请填写任务标题" }]
}
```

常见错误码：`VALIDATION_FAILED`(400)、`UNAUTHORIZED`(401)、`BAD_CREDENTIALS`(401)、
`FORBIDDEN`(403)、`NOT_FOUND`(404)、`CONFLICT`(409)，以及各模块的业务码，例如
`TASK_STALE`、`STUDY_RECORD_STALE`、`SETTINGS_STALE`、`TASK_HAS_RECORDS`、
`REVIEW_REQUIRES_STUDY`、`REVIEW_QUOTA_REQUIRED`、`UNIT_MISMATCH`、`MEMO_STALE`、`TARGET_STALE`。

并发编辑：所有可多人编辑的资源都带回 `version`，更新时必须原样回传；
版本不一致返回 409 而不是静默覆盖。删除学习记录也要求 `version`。

## 端点清单

### 账号

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/auth/csrf` | 取得 CSRF 令牌 |
| POST | `/api/auth/login` | 登录，body `{username,password}` |
| GET | `/api/auth/me` | 当前身份，未登录返回 `{authenticated:false}` |
| POST | `/api/auth/logout` | 退出（204） |

### 设置

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/settings/subjects` | 科目列表 / 新增 |
| PUT/DELETE | `/api/settings/subjects/{id}` | 修改 / 删除（有任务时拒绝） |
| GET/POST | `/api/settings/units` | 计量单位列表 / 新增 |
| PUT/DELETE | `/api/settings/units/{id}` | 修改 / 删除（被引用时拒绝） |
| GET/PUT | `/api/settings/study` | 考试时间、学习预算、复习额度、复习间隔（PUT 需 `version`） |

### 计划

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/plan/tasks` | 任务列表（`subjectId`/`stageGoalId`/`status`）/ 新建 |
| GET/PUT/DELETE | `/api/plan/tasks/{id}` | 详情 / 修改（需 `version`）/ 删除（有学习记录时拒绝） |
| GET | `/api/plan/tasks/{id}/progress` | 计划量、完成量、剩余量 |
| GET/POST | `/api/plan/stage-goals` | 阶段目标 |
| PUT/DELETE | `/api/plan/stage-goals/{id}` | 修改 / 删除（被任务引用时拒绝） |
| GET | `/api/plan/weeks/{weekStart}` | 周计划（`weekStart` 必须是周一） |
| PUT | `/api/plan/weeks/{weekStart}/items` | 安排任务到本周 |
| DELETE | `/api/plan/weeks/{weekStart}/items/{taskId}` | 从本周移除 |
| GET | `/api/plan/days/{date}` | 某天安排（含当天已完成量） |
| POST | `/api/plan/days/{date}/items` | 加入当天安排 |
| PUT | `/api/plan/days/{date}/items/{itemId}` | 手动调整计划量（需 `version`，会记录调整） |
| DELETE | `/api/plan/days/{date}/items/{itemId}?version=` | 按版本移除当天安排，学习记录保留 |
| GET | `/api/plan/workspace` | 可选 startDate/endDate/subjectId；同日任务、按科目单位的月汇总、导入目标说明 |
| POST | `/api/plan/days/{date}/quick-items` | 新建内容或已有任务入日安排；clientToken 防重 |
| PUT | `/api/plan/days/{date}/order` | items 为当天全部条目的 id/version，过期或漏项拒绝 |
| POST | `/api/plan/imports/preview` | 原始 JSON 字符串 document；只读校验并返回预览与 previewToken |
| POST | `/api/plan/imports/confirm` | document 与 previewToken；重查快照后事务追加 |

新页面以日安排汇总周月；旧周接口保留兼容。新增日条目不再覆盖已有同来源条目，编辑使用 PUT 携带 version；REVIEW/IMPORT 来源只能通过专用确认入口新增。quick-items 支持 `{existingTaskId,plannedAmount,estimatedMinutes,clientToken}`，或 `{subjectId,unitId,title,plannedAmount,estimatedMinutes,clientToken}`。复习仍在复习模块管理。

导入合同见 [规划书](../business/plan-import-guide.md)：重复返回 PLAN_ALREADY_IMPORTED（409），过期返回 PLAN_PREVIEW_STALE（409），字段和业务校验失败返回 400。没有内置模型调用接口。

### 学习记录

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/learning/records` | 列表（`from`/`to`/`taskId`/`subjectId`） |
| GET | `/api/learning/archive` | 同样筛选下的月／周／日／课程组汇总及原始记录；缺失用时单独标注 |
| GET | `/api/learning/records/{id}` | 详情 |
| POST | `/api/learning/records` | 提交（带 `clientToken` 时重复提交不生成第二条） |
| PUT | `/api/learning/records/{id}` | 修改（需 `version`） |
| DELETE | `/api/learning/records/{id}?version=` | 软删除，统计随之重算 |

关联任务的记录以任务的科目与单位为准；计划外学习在填写完成量时必须自带 `subjectId` 与 `unitId`。
不填 `amount` 时表示“只记时长”：它计入学习时长，不计入任何单位的完成量，因此也不要求科目与单位。

### 今日与进度

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/today?date=` | 当天安排、当天记录、已确认的复习任务与提示 |
| GET | `/api/progress/overview` | 按科目+单位的计划与实际（含趋势） |
| GET | `/api/progress/trend` | 学习量与时长趋势（单位分别成列） |

进度的 `from/to` 同时约束日安排与学习记录日期。overview 的 `plannedAmount` 汇总区间内日安排（排除 REVIEW），不再读取全部任务的总计划量；无日安排为 null。`studyMinutes` 在该分组的记录均未填用时时为 null。完整交互与离线范围见 [记录与进度工作区](../business/records-progress-workspace.md)。

### 课程目录与累计进度

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/progress/catalog` | 目录版本、课程与完整节点、按科目单位的累计学习量、待归类记录 |
| GET | `/api/progress/catalog/candidates` | 现有任务候选及已关联节点；只读，不自动建课程 |
| POST | `/api/progress/catalog/preview` | `{document: JSON文本}`，校验后返回预览令牌、15 分钟期限、原／新总量及提交目录 |
| POST | `/api/progress/catalog/confirm` | `{previewToken}`，版本及上下文校验后事务保存；同令牌重试不重复应用 |
| PUT | `/api/progress/catalog/records/{id}` | `{nodeId, recordVersion, revision}`，确认单条归属；nodeId 为 null 恢复任务默认归属 |
| PUT | `/api/progress/catalog/nodes/{id}/tasks` | `{taskIds, revision}`，替换末级目录的任务关联，科目单位一致且任务唯一归属 |

课程总量由完整目录末级项产生，不从日安排产生。`completedAmount` 对每个末级条目分别以总量封顶后求和；`recordedAmount` 保留真实累计，学科累计同样不封顶。节点的 `completedAmount` 是真实记录量，可以超出其总量。实际数量都由未删除学习记录推导，目录不存手动完成量。

导入规则见 [目录导入说明](../business/progress-catalog-guide.md)。未知字段、重复字段、错误 JSON 类型、重复或跨单位归属返回 400；目录版本、记录版本或预览过期返回 409。新增接口沿用身份验证及 CSRF 防护。旧 overview/trend 仍保留，但新进度首页只展示四个入口。

### 复习

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/review/items` | 复习项列表 / 加入复习 |
| GET | `/api/review/items/{id}` | 详情 |
| POST | `/api/review/items/{id}/archive?version=` | 归档 |
| GET | `/api/review/items/{id}/records` | 复习记录 |
| POST | `/api/review/records` | 提交反馈（FORGOT/VAGUE/MASTERED） |
| GET | `/api/review/suggestions?date=` | 当天建议（含额度与预计用时缺口） |
| POST | `/api/review/suggestions/confirm` | 确认排入某一天 |

加入复习有两种来源：传 `taskId`（该任务必须有有效、非未来日期的学习记录），
或传 `sourceRecordId` 和 `title`（来源必须是未删除、已有学习量或时长的计划外学习记录，且有科目）。
计划外复习的科目从来源记录读取；可选的 `subjectId` 若与来源不一致则拒绝。
只有科目与标题、不关联学习记录的请求不再接受。

确认安排时先锁定单用户设置行，再以当前读读取当天占用和复习项；锁持续到事务提交。
并发确认共享同一个额度，预计用时与安排状态在一次带版本条件的更新中保存。

### 备忘录与候选目标

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/memo` | 列表（`status`/`subjectId`/`keyword`）/ 新建 |
| GET | `/api/memo/due?date=` | 到期提醒 |
| GET/PUT/DELETE | `/api/memo/{id}` | 详情 / 修改（需 `version`）/ 删除 |
| POST | `/api/memo/{id}/dismiss-reminder` | 忽略本次提醒 |
| POST | `/api/memo/{id}/convert-to-task` | 转为任务 |
| GET/POST | `/api/targets` | 候选院校列表 / 新建 |
| GET/PUT/DELETE | `/api/targets/{id}` | 详情 / 修改（链接整体替换，需 `version`）/ 删除 |
| PATCH | `/api/targets/{id}/status` | 只改状态 |

## 接口背后的业务约束

这些约束由后端强制，前端只负责展示结果：

- 完成量只来自学习记录；创建任务或安排不会产生完成量。
- 不同计量单位不直接相加，接口不提供跨单位的总体完成百分比。
- 修改或删除学习记录后，进度与趋势立即按记录重新计算。
- 任务已有学习记录（含软删除记录）后，不可修改科目或计量单位；仍可修改标题、计划量及归档。
- 删除科目、单位、任务时检查全部外键引用，包含软删除记录；仍被引用时返回 409，建议停用或归档。
- 同一备忘录重复或并发转任务返回第一次生成的任务，不再额外建任务；首次转换会递增备忘录版本。
- 未完成的安排不会自动出现在次日。
- 复习只针对已学内容；建议必须经确认才进入某一天；超出额度的内容留在待安排列表。
- 缺少复习额度或预计用时时，接口返回明确的提示字段，不声称满足额度。

有序练习：计划导入 v2 增加 `tasks[].sequence`；任务响应增加 `sequential`。`GET /api/learning/sequences/{taskId}?date=YYYY-MM-DD` 查询题目清单与日期展示题。记录 create/update 支持 `sequenceItemId`，响应增加题名与链接；已有接续记录编辑保持原题目身份。今日响应增加 sequence 摘要，day-progress 仅接受 0/100，详情见 [接续规则](../business/sequential-practice.md)。
# 时间型学习安排（V18 / 导入 v3）

`POST /api/learning/records/day-progress` 对时间型任务接受 `actualAmount`（本次实际首次完成量，可留空）与 `note`（教材位置、最后题号和卡点）。`completionPercent` 只能为 0（继续记录）或 100（结束当天安排），不是整讲完成比例。用时必须为正数；RECAP 任务禁止填写完成量，包括普通记录的新建和修改入口。身份、CSRF、clientToken 和 revision 检查沿用原流程。

今日清单额外返回 `activity: {mode,description,flexibleMinutes}` 和 `unitName`。时间型日安排 `plannedAmount` 可为空，月份不得将其当作 0 或已知题量；基本预计用时不含弹性上限。删除结束记录后，当天安排重新开放。

## 完成确认（V19 / 导入 v4）

POST /api/learning/records/day-confirmation 接收 `{taskId,date,completed,revision,clientToken}`，返回更新后的当天任务视图。仅用于 CHECK_ONLY；无需数量、时长或掌握评分。确认保存数量和用时均空的学习记录，取消软删除该日确认。重复目标状态不重复写入；改变状态时校验revision，包含删除历史以防旧请求覆盖新状态。身份、CSRF与任务行锁保持。

任务、日安排、工作区条目、学习记录增加activityMode。月份confirmationCount单独显示安排项数，与同科目学习数量分开。确认可在今日取消或从记录页删除，不可改为数量／用时记录。

### 学习接续（v5）

原计划导入 preview/confirm 支持 CONTINUED_STUDY + continuation（streamKey、title、position）。原 day-progress 接受每份内容的累计百分比，实际量 actualAmount 留空；今日 checklist 增加 continuation（streamId、title、position、referenceDate、carried），学习记录响应增加 continuationDelta。普通任务响应该字段为空。身份验证、CSRF、版本检查、防重及事务沿用原接口。日历日期为参考排期，实际继续内容由今日接口返回，允许超过参考截止日期。
