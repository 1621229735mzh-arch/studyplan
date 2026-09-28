/**
 * 计划模块接口（阶段目标、任务、周计划、每日安排）。
 *
 * 任务、周计划与今日安排共享同一 taskId；周计划与每日安排只做“安排”，
 * 不产生完成量（完成量由学习记录决定）。
 *
 * 写接口都是“服务端返回刷新后的结果”：每日安排的三类写操作都返回该天的完整条目数组，
 * 周计划的写操作返回刷新后的周计划，因此调用方不需要自己拼装本地状态。
 */
import { asList, cleanParams, del, get, post, put } from '@/api/http'
import type { PageResult } from '@/types/api'
import type {
  DailyItemAdjustRequest,
  DailyItemCreateRequest,
  DailyPlanItemView,
  PlanTask,
  PlanTaskRequest,
  StageGoal,
  StageGoalRequest,
  StageGoalUpdateRequest,
  TaskProgressView,
  WeeklyPlanItemRequest,
  WeekPlanResponse
} from '../types'

/** 阶段目标列表。 */
export async function listStageGoals(): Promise<StageGoal[]> {
  return asList(await get<StageGoal[] | PageResult<StageGoal>>('/plan/stage-goals'))
}

/** 新增阶段目标。 */
export function createStageGoal(payload: StageGoalRequest): Promise<StageGoal> {
  return post<StageGoal>('/plan/stage-goals', payload)
}

/** 修改阶段目标；payload 需包含 version。 */
export function updateStageGoal(id: number, payload: StageGoalUpdateRequest): Promise<StageGoal> {
  return put<StageGoal>(`/plan/stage-goals/${id}`, payload)
}

/** 任务列表（可按科目、阶段目标与状态筛选）。 */
export async function listPlanTasks(params: {
  subjectId?: number
  stageGoalId?: number
  status?: string
} = {}): Promise<PlanTask[]> {
  return asList(await get<PlanTask[] | PageResult<PlanTask>>('/plan/tasks', { params: cleanParams({ ...params }) }))
}

/** 任务详情。 */
export function getPlanTask(id: number): Promise<PlanTask> {
  return get<PlanTask>(`/plan/tasks/${id}`)
}

/** 新增任务（科目 + 单位 + 计划量；计划量可留空）。 */
export function createPlanTask(payload: PlanTaskRequest): Promise<PlanTask> {
  return post<PlanTask>('/plan/tasks', payload)
}

/** 修改任务；payload 需包含 version。 */
export function updatePlanTask(id: number, payload: PlanTaskRequest): Promise<PlanTask> {
  return put<PlanTask>(`/plan/tasks/${id}`, payload)
}

/** 删除任务；任务已有学习记录时后端返回 409 TASK_HAS_RECORDS。 */
export function deletePlanTask(id: number): Promise<void> {
  return del<void>(`/plan/tasks/${id}`)
}

/** 任务进度（计划量 / 完成量 / 剩余量，均由后端计算）。 */
export function getTaskProgress(id: number): Promise<TaskProgressView> {
  return get<TaskProgressView>(`/plan/tasks/${id}/progress`)
}

/**
 * 读取周计划（weekStart 为该周周一，YYYY-MM-DD）。
 *
 * 后端总是成功：该周还没有计划时会创建空周计划再返回，因此前端不需要处理 404。
 */
export function getWeekPlan(weekStart: string): Promise<WeekPlanResponse> {
  return get<WeekPlanResponse>(`/plan/weeks/${weekStart}`)
}

/** 安排任务到本周（同一任务重复安排时更新计划量）。 */
export function addOrUpdateWeekPlanItem(
  weekStart: string,
  payload: WeeklyPlanItemRequest
): Promise<WeekPlanResponse> {
  return put<WeekPlanResponse>(`/plan/weeks/${weekStart}/items`, payload)
}

/** 从本周移除任务；返回刷新后的周计划。 */
export function removeWeekPlanItem(weekStart: string, taskId: number): Promise<WeekPlanResponse> {
  return del<WeekPlanResponse>(`/plan/weeks/${weekStart}/items/${taskId}`)
}

/** 读取某天的安排（含当天完成量）；后端返回裸数组。 */
export function getDayPlan(date: string): Promise<DailyPlanItemView[]> {
  return get<DailyPlanItemView[]>(`/plan/days/${date}`)
}

/** 加入当天安排；返回刷新后的当天条目数组。 */
export function addDayPlanItem(date: string, payload: DailyItemCreateRequest): Promise<DailyPlanItemView[]> {
  return post<DailyPlanItemView[]>(`/plan/days/${date}/items`, payload)
}

/**
 * 手动调整当天安排量（需携带 version，后端会记录调整）。
 *
 * 这是真正的“调整”接口：不要用“先删除再新增”替代（那样会丢掉条目与调整记录）。
 */
export function adjustDayPlanItem(
  date: string,
  itemId: number,
  payload: DailyItemAdjustRequest
): Promise<DailyPlanItemView[]> {
  return put<DailyPlanItemView[]>(`/plan/days/${date}/items/${itemId}`, payload)
}

/** 从某天安排中移除条目；返回刷新后的当天条目数组。 */
export function removeDayPlanItem(date: string, itemId: number): Promise<DailyPlanItemView[]> {
  return del<DailyPlanItemView[]>(`/plan/days/${date}/items/${itemId}`)
}
