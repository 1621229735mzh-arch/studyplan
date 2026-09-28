/**
 * 进度模块数据类型（对应 backend 的 com.kaoyan.study.progress.dto）。
 *
 * 后端负责统计；前端只负责展示。所有数量都带单位，且按“科目 + 单位”成组返回，
 * 因此前端不会（也不允许）把不同单位相加、相减或计算总体百分比。
 */
import type { UnitAmount } from '@/types/domain'

/** 科目下单个计量单位的计划与实际（对应后端 UnitProgressView）。 */
export interface UnitProgressView {
  subjectId: number
  subjectName: string
  unitId: number
  unitName: string
  /** 计划量；null 表示该单位下没有设置计划量，不是 0 */
  plannedAmount?: number | null
  completedAmount?: number | null
  /** 该科目该单位下的学习时长（分钟） */
  studyMinutes?: number | null
}

/** 趋势中的某一天的某个单位完成量（对应后端 DailyUnitAmountView）。 */
export type DailyUnitAmountView = UnitAmount

/** 趋势中的单日数据（对应后端 TrendPointView）：时长与各单位完成量分开呈现。 */
export interface TrendPointView {
  date: string
  studyMinutes: number
  /** 当日各单位的完成量，按单位分组 */
  amounts: UnitAmount[]
}

/** `GET /api/progress/overview` 响应（后端 ProgressOverviewResponse）。 */
export interface ProgressOverview {
  from: string
  to: string
  items: UnitProgressView[]
  trend: TrendPointView[]
}
