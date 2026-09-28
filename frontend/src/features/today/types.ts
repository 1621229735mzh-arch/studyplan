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
}

/**
 * 今日计划任务的展示行。
 *
 * 后端 DailyPlanItemView 不带科目名，这里用科目目录按 subjectId 补出名称用于展示；
 * `hasRecord` 表示后端返回的当天学习记录里是否有该任务（不是新的统计口径）。
 */
export interface TodayPlanTaskRow extends DailyPlanItemView {
  subjectName: string | null
  hasRecord: boolean
}

/** 今日复习任务的展示行：后端 ReviewItemResponse 不带科目名，这里用科目目录补出。 */
export interface TodayReviewTaskRow extends ReviewItem {
  subjectName: string | null
}
