/**
 * 复习接口。
 *
 * 复习项只能来自已学内容；建议必须经确认才进入今日安排；
 * 额度校验由后端执行，前端只展示后端返回的额度与拒绝原因（见 ./quota.ts）。
 */
import { asList, cleanParams, get, post } from '@/api/http'
import type { PageResult } from '@/types/api'
import type {
  ReviewConfirmRequest,
  ReviewConfirmResponse,
  ReviewItem,
  ReviewItemCreateRequest,
  ReviewRecordCreateRequest,
  ReviewRecordResponse,
  ReviewSuggestionResponse
} from '../types'

/** 复习项列表（可按状态、科目筛选）。 */
export async function listReviewItems(params: { status?: string; subjectId?: number } = {}): Promise<ReviewItem[]> {
  return asList(
    await get<ReviewItem[] | PageResult<ReviewItem>>('/review/items', { params: cleanParams({ ...params }) })
  )
}

/** 把已学内容加入复习。 */
export function createReviewItem(payload: ReviewItemCreateRequest): Promise<ReviewItem> {
  return post<ReviewItem>('/review/items', payload)
}

/** 提交复习反馈（不会 / 模糊 / 掌握）；返回后端推导出的下次复习日期与间隔。 */
export function createReviewRecord(payload: ReviewRecordCreateRequest): Promise<ReviewRecordResponse> {
  return post<ReviewRecordResponse>('/review/records', payload)
}

/** 某天的复习建议（含额度、已安排时长与缺失输入说明）；后端返回单个对象，不是数组。 */
export function getReviewSuggestions(date?: string): Promise<ReviewSuggestionResponse> {
  return get<ReviewSuggestionResponse>('/review/suggestions', { params: cleanParams({ date }) })
}

/** 确认建议进入某一天；后端在此逐条校验额度并可能拒绝部分条目。 */
export function confirmReviewSuggestions(payload: ReviewConfirmRequest): Promise<ReviewConfirmResponse> {
  return post<ReviewConfirmResponse>('/review/suggestions/confirm', payload)
}
