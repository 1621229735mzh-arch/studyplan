import { get, post, put } from '@/api/http'
import type { DailyPlanItemView } from '../types'
import type { ImportPreview, PlanWorkspace, QuickDayRequest } from '../workspace'

export function getWorkspace(subjectId?: number): Promise<PlanWorkspace> {
  return get('/plan/workspace', { params: { subjectId } })
}
export function quickAddDay(date: string, payload: QuickDayRequest): Promise<DailyPlanItemView[]> {
  return post(`/plan/days/${date}/quick-items`, payload)
}
export function reorderDay(date: string, items: { id: number; version: number }[]): Promise<DailyPlanItemView[]> {
  return put(`/plan/days/${date}/order`, { items })
}
export function previewImport(document: string): Promise<ImportPreview> {
  return post('/plan/imports/preview', { document })
}
export function confirmImport(document: string, previewToken: string): Promise<ImportPreview> {
  return post('/plan/imports/confirm', { document, previewToken })
}
