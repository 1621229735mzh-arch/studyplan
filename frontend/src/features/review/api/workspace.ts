import { cleanParams, get, post } from '@/api/http'
import type { ReviewItem } from '../types'
import type { ReviewWorkspace } from '../workspace'

export function getReviewWorkspace(subjectId?: number): Promise<ReviewWorkspace> {
  return get('/review/workspace', { params: cleanParams({ subjectId }) })
}
export function scheduleReview(reviewItemId: number, date: string | null, version: number): Promise<ReviewItem> {
  return post('/review/workspace/schedule', { reviewItemId, date, version })
}
