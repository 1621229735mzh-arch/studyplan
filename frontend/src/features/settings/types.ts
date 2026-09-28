/**
 * 设置模块类型（对应 backend 的 com.kaoyan.study.settings.dto）。
 *
 * 预算、额度与复习间隔都是待定项：后端用 null 表示“尚未确定”，前端必须显示“未设置”，
 * 不能当作 0，也不能据此宣称满足额度。复习间隔由本人配置，应用不提供默认值。
 */
import type { ReviewPhase } from '@/types/domain'

/** 学习设置（对应 backend 的 StudySettingsResponse）。 */
export interface StudySettings {
  examDate?: string | null
  /** 每日学习预算（分钟）；null 表示未设置 */
  dailyStudyMinutes?: number | null
  /** 每日复习额度（分钟）；null 表示未设置 */
  dailyReviewMinutes?: number | null
  reviewPhase?: ReviewPhase | null
  /** 不会档位的复习间隔（天）；null 表示未设置，后端不会臆造下次日期 */
  reviewIntervalForgotDays?: number | null
  /** 模糊档位的复习间隔（天）；null 表示未设置 */
  reviewIntervalVagueDays?: number | null
  /** 掌握档位的复习间隔（天）；null 表示未设置 */
  reviewIntervalMasteredDays?: number | null
  /** 并发控制版本，更新时必须回传 */
  version: number
}

/** 更新学习设置（后端 StudySettingsRequest）；version 必填。 */
export interface StudySettingsRequest {
  examDate?: string | null
  dailyStudyMinutes?: number | null
  dailyReviewMinutes?: number | null
  reviewPhase?: ReviewPhase | null
  reviewIntervalForgotDays?: number | null
  reviewIntervalVagueDays?: number | null
  reviewIntervalMasteredDays?: number | null
  version: number
}

/** 新建/修改科目。 */
export interface SubjectRequest {
  name: string
  category?: string | null
  color?: string | null
  sortOrder?: number
  enabled?: boolean
}

/** 新建/修改计量单位。 */
export interface StudyUnitRequest {
  name: string
  sortOrder?: number
  enabled?: boolean
}
