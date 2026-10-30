/**
 * 今日页面的数据类型。
 *
 * 对应 `GET /api/today?date=YYYY-MM-DD`（后端 TodayResponse）：
 * 当天计划安排、当天学习记录、已确认的复习任务、时长预算与提示。
 *
 * 后端不返回“未记录条目”“按单位的完成量合计”“掌握情况合计”，
 * 因此这些内容要么删除，要么只能由后端返回的记录如实呈现，绝不在这里自行汇总。
 */
import type { LearningRecord } from '@/features/learning/types'
import type { DailyPlanItemView } from '@/features/plan/types'
import type { ReviewItem } from '@/features/review/types'

/** `GET /api/today` 响应（Java record 字段名即 JSON 字段名）。 */
export interface TodayResponse {
  date: string
  /** 当天计划安排（DailyPlanItemView：id/taskTitle/completedAmount…） */
  plannedTasks: DailyPlanItemView[]
  /** 当天学习记录（StudyRecordView） */
  records: LearningRecord[]
  /** 当天已确认的复习任务（ReviewItemResponse） */
  reviewTasks: ReviewItem[]
  /** 每日学习预算（分钟）；null 表示未设置 */
  dailyStudyMinutes?: number | null
  /** 每日复习额度（分钟）；null 表示未设置 */
  dailyReviewMinutes?: number | null
  /** 当天学习时长合计（分钟），由后端统计 */
  studyMinutes: number
  /** 后端明确列出的缺失输入（例如未设置预算/额度） */
  warnings: string[]
  /** 新快照由后端返回汇总；旧版离线快照可没有这些字段。 */
  actualMinutes?: number
  remainingMinutes?: number
  missingEstimates?: number
  missingDurations?: number
  sections?: TodaySubjectSection[]
  /** 后端按当天清单统计，三类互斥；旧离线快照可能没有。 */
  taskCounts?: { completed: number; inProgress: number; remaining: number }
}

export interface TodayChecklistTask {
  recordedAmount?: number | null
  continuation?: { streamId: number; title: string; position: number; referenceDate: string; carried: boolean; optional?: boolean } | null
  activity?: { mode: 'TIMED_STUDY' | 'RECAP' | 'CHECK_ONLY' | 'CONTINUED_STUDY' | 'COUNT_STUDY' | 'COUNT_REVIEW'; description: string | null; flexibleMinutes: number } | null
  unitName?: string | null
  sequence?: import('@/features/learning/types').SequenceState | null
  kind: 'LEARNING' | 'REVIEW'
  id: number
  title: string
  completionPercent: number
  estimatedMinutes: number | null
  remainingMinutes: number | null
  revision: string | null
  version: number | null
  actionable: boolean
}
export interface TodaySubjectSection {
  name: string
  actualMinutes: number
  remainingMinutes: number
  missingEstimates: number
  missingDurations: number
  tasks: TodayChecklistTask[]
}
