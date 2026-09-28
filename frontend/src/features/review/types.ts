/**
 * 复习模块数据类型（对应 backend 的 com.kaoyan.study.review.dto）。
 *
 * 业务前提（PLAN.md 二.3）：
 * - 只有已学且已加入复习的内容才能生成建议；
 * - 建议必须经本人确认才进入今日安排；
 * - 缺少用时或未设额度时，前端不得宣称“符合额度”（见 ./quota.ts）。
 */
import type { MasteryLevel } from '@/types/domain'

/** 复习项状态（对应后端 ReviewStatus）。 */
export type ReviewStatus = 'PENDING' | 'SCHEDULED' | 'ARCHIVED'

/** 复习项（对应后端 ReviewItemResponse）。 */
export interface ReviewItem {
  id: number
  subjectId?: number | null
  taskId?: number | null
  title: string
  status: ReviewStatus
  /** 建议的下次复习日期；间隔未配置时为空，不要编造日期 */
  nextReviewDate?: string | null
  /** 上一次掌握反馈（后端字段名为 lastResult，不是 mastery） */
  lastResult?: MasteryLevel | null
  intervalDays?: number | null
  /** 预计用时（分钟）；为空表示尚未补充，此时无法判断额度 */
  estimatedMinutes?: number | null
  /** 已被确认安排到的日期 */
  scheduledDate?: string | null
  version: number
}

/** 把已学内容加入复习（后端 ReviewItemCreateRequest）。 */
export interface ReviewItemCreateRequest {
  /** 关联任务时，科目与标题取任务信息 */
  taskId?: number | null
  /** 计划外复习必须关联有效学习记录，科目由该记录确定 */
  sourceRecordId?: number | null
  subjectId?: number | null
  title?: string | null
  estimatedMinutes?: number | null
}

/** 提交复习反馈（后端 ReviewRecordCreateRequest）。 */
export interface ReviewRecordCreateRequest {
  reviewItemId: number
  /** 复习日期（后端字段名为 reviewDate，不是 reviewedOn） */
  reviewDate: string
  /** 掌握反馈（后端字段名为 result，不是 mastery） */
  result: MasteryLevel
  durationMinutes?: number | null
  note?: string | null
  /** 手动指定下次复习日期；不指定时按设置中的间隔推导 */
  nextReviewDate?: string | null
  /** 防重复提交令牌（后端字段名为 clientToken，不是 clientRequestId） */
  clientToken?: string | null
}

/**
 * 复习反馈结果（对应后端 ReviewRecordResponse）。
 *
 * `intervalConfigured=false` 表示设置里还没填该档位的间隔天数，因此 `nextReviewDate`
 * 为空——这是“待定参数未确定”，不是建议为“无”。界面必须提示去补充间隔，不能编造日期。
 */
export interface ReviewRecordResponse {
  id: number
  reviewItemId: number
  reviewDate: string
  result: MasteryLevel
  durationMinutes?: number | null
  note?: string | null
  nextReviewDate?: string | null
  intervalDays?: number | null
  intervalConfigured: boolean
}

/** 一条复习建议（后端 ReviewSuggestionResponse.Item）。 */
export interface ReviewSuggestionItem {
  /** 后端字段名为 reviewItemId（不是 itemId） */
  reviewItemId: number
  title: string
  subjectId?: number | null
  /** 到期日；积压时用 overdueDays 体现，不自动提高强度 */
  dueDate: string
  overdueDays: number
  /** 为空表示缺用时：无法判断是否在额度内 */
  estimatedMinutes?: number | null
}

/**
 * `GET /api/review/suggestions` 响应（后端 ReviewSuggestionResponse）。
 *
 * 后端不返回“是否符合额度”的结论，只返回额度、已安排时长、剩余额度与 warnings；
 * 因此前端的表述必须由 ./quota.ts 统一给出，禁止在缺少额度或用时时宣称符合额度。
 */
export interface ReviewSuggestionResponse {
  date: string
  /** 每日复习额度；null 表示未设置（不能当成 0） */
  dailyReviewMinutes?: number | null
  /** 当天已安排的复习用时合计（分钟） */
  scheduledMinutes: number
  /** 剩余额度；未设额度时为 null */
  remainingMinutes?: number | null
  items: ReviewSuggestionItem[]
  /** 后端明确列出的缺失输入（额度、预计用时） */
  warnings: string[]
}

/** 确认时逐条补充的预计用时（后端 ReviewConfirmRequest.Item）。 */
export interface ReviewConfirmItemRequest {
  reviewItemId: number
  estimatedMinutes: number
}

/** 确认把建议排入某一天（后端 ReviewConfirmRequest）。 */
export interface ReviewConfirmRequest {
  date: string
  items: ReviewConfirmItemRequest[]
}

/** 未能进入安排的内容及原因（后端 ReviewConfirmResponse.Rejected）。 */
export interface ReviewConfirmRejected {
  reviewItemId: number
  title?: string | null
  reason: string
}

/** 确认结果（后端 ReviewConfirmResponse）。 */
export interface ReviewConfirmResponse {
  date: string
  dailyReviewMinutes?: number | null
  /** 当天已安排的复习用时合计（分钟，含本次确认的条目） */
  usedMinutes: number
  scheduled: ReviewItem[]
  rejected: ReviewConfirmRejected[]
}
