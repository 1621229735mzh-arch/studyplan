/** 计划模块接口：阶段目标、任务及每日安排。完成量由学习记录决定。 */
import { cleanParams, del, get, post, put } from '@/api/http'
import type {
  DailyItemAdjustRequest,
  DailyPlanItemView,
  PlanTask,
  PlanTaskRequest,
  StageGoal,
  StageGoalRequest,
  StageGoalUpdateRequest,
  TaskProgressView
} from '../types'

/** 阶段目标列表。 */
export function listStageGoals(): Promise<StageGoal[]> {
  return get<StageGoal[]>('/plan/stage-goals')
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
export function listPlanTasks(params: {
  subjectId?: number
  stageGoalId?: number
  status?: string
} = {}): Promise<PlanTask[]> {
  return get<PlanTask[]>('/plan/tasks', { params: cleanParams({ ...params }) })
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

/** 读取某天的安排（含当天完成量）；后端返回裸数组。 */
export function getDayPlan(date: string): Promise<DailyPlanItemView[]> {
  return get<DailyPlanItemView[]>(`/plan/days/${date}`)
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
export function removeDayPlanItem(date: string, itemId: number, version: number): Promise<DailyPlanItemView[]> {
  return del<DailyPlanItemView[]>(`/plan/days/${date}/items/${itemId}`, { params: { version } })
}
