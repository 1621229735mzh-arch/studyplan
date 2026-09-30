/**
 * 计划模块数据类型（对应 backend 的 com.kaoyan.study.plan.dto，Java record 的字段名即 JSON 字段名）。
 *
 * 关键业务前提（见 PLAN.md 二.2）：
 * - 任务、周任务与今日安排共享同一任务事实，周计划与今日安排引用同一个 taskId；
 * - 任务有明确计量单位，计划量/完成量/剩余量由后端给出，前端不做减法推算；
 * - 未完成任务不会自动顺延到次日，需要本人手动安排。
 */

/**
 * 任务状态（对应后端 TaskStatus）。
 *
 * 未完成的任务不会因为到期自动顺延；状态只反映本人对任务的处置。
 */
export type TaskStatus = 'ACTIVE' | 'DONE' | 'ARCHIVED'

/** 阶段目标（对应后端 StageGoalResponse）。 */
export interface StageGoal {
  id: number
  subjectId: number
  title: string
  description?: string | null
  startDate?: string | null
  /** 目标日期；后端字段名为 targetDate（不是 endDate） */
  targetDate?: string | null
  /** 后端为字符串状态，默认 ACTIVE */
  status: string
  version: number
}

/** 新建阶段目标（后端 StageGoalCreateRequest）。 */
export interface StageGoalRequest {
  subjectId: number
  title: string
  description?: string | null
  startDate?: string | null
  targetDate?: string | null
}

/** 修改阶段目标（后端 StageGoalUpdateRequest）；version 必填。 */
export interface StageGoalUpdateRequest extends StageGoalRequest {
  status?: string | null
  version: number
}

/**
 * 任务（对应后端 TaskResponse）。
 *
 * 后端不在这里返回科目名/单位名，也不返回完成量：计量单位名需要前端用 unitId 从
 * 科目/单位目录里取，完成量与剩余量见 `TaskProgressView`。
 */
export interface PlanTask {
  id: number
  subjectId: number
  stageGoalId?: number | null
  title: string
  unitId: number
  /** 计划量；null/undefined 表示尚未确定计划量，不是 0 */
  plannedAmount?: number | null
  status: TaskStatus
  version: number
}

/** 新建/修改任务（后端 TaskCreateRequest / TaskUpdateRequest）。 */
export interface PlanTaskRequest {
  subjectId: number
  stageGoalId?: number | null
  title: string
  unitId: number
  /** 计划量可以留空，表示尚未确定 */
  plannedAmount?: number | null
  /** 仅修改时可带 */
  status?: TaskStatus
  /** 修改时必填（后端用它做多设备冲突检查） */
  version?: number
}

/** 任务进度（对应后端 TaskProgressView）：完成量来自学习记录。 */
export interface TaskProgressView {
  taskId: number
  title: string
  subjectId: number
  unitId: number
  unitName: string
  /** 计划量未确定时为 null，不臆造“剩余工作量”与完成百分比 */
  plannedAmount?: number | null
  completedAmount?: number | null
  remainingAmount?: number | null
}

/** 每日安排来源（对应后端 PlanSource）。 */
export type PlanSource = 'MANUAL' | 'WEEKLY' | 'REVIEW'

/**
 * 每日安排条目（对应后端 DailyPlanItemView）。
 *
 * 注意：条目自身 id 是 `id`（不是 itemId），任务标题是 `taskTitle`（不是 title），
 * 当天完成量是 `completedAmount`（不是 actualAmount）。后端不返回剩余量。
 */
export interface DailyPlanItemView {
  /** 外部计划给出的完整预计用时（分钟），缺失不是 0 */
  estimatedMinutes?: number | null
  id: number
  taskId: number
  taskTitle: string
  subjectId?: number | null
  unitId?: number | null
  unitName: string
  plannedAmount: number
  source: PlanSource
  /** 由复习建议确认而来的条目会带复习项 id */
  reviewItemId?: number | null
  version: number
  completedAmount?: number | null
}

/** 加入某一天的安排（后端 DailyItemCreateRequest）。 */
export interface DailyItemCreateRequest {
  /** 外部计划给出的完整预计用时（分钟），缺失不是 0 */
  estimatedMinutes?: number | null
  taskId: number
  plannedAmount: number
  /** 为空时后端按 MANUAL 处理 */
  source?: PlanSource | null
}

/** 手动调整某一天的安排量（后端 DailyItemAdjustRequest）；调整不会顺延未完成内容。 */
export interface DailyItemAdjustRequest {
  /** 外部计划给出的完整预计用时（分钟），缺失不是 0 */
  estimatedMinutes?: number | null
  plannedAmount: number
  version: number
  reason?: string | null
}

/** 周计划中的一条任务安排（对应后端 WeeklyPlanItemView）。 */
export interface WeeklyPlanItemView {
  taskId: number
  taskTitle: string
  subjectId?: number | null
  unitId?: number | null
  unitName: string
  plannedAmount: number
  completedAmount?: number | null
}

/** `GET /api/plan/weeks/{weekStart}` 响应（后端 WeeklyPlanResponse）。 */
export interface WeekPlanResponse {
  /** 该周周一日期（后端字段名为 weekStartDate） */
  weekStartDate: string
  note?: string | null
  version: number
  items: WeeklyPlanItemView[]
}

/** 安排任务到本周（后端 WeeklyPlanItemRequest）：同一任务重复安排即更新计划量。 */
export interface WeeklyPlanItemRequest {
  taskId: number
  plannedAmount: number
}

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  ACTIVE: '进行中',
  DONE: '已完成',
  ARCHIVED: '已归档'
}

export const TASK_STATUS_OPTIONS: TaskStatus[] = ['ACTIVE', 'DONE', 'ARCHIVED']

/** 任务状态展示；未知取值原样显示。 */
export function taskStatusLabel(status?: string | null): string {
  if (!status) {
    return '—'
  }
  return TASK_STATUS_LABELS[status as TaskStatus] ?? status
}
