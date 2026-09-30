/**
 * 今日模块接口。
 *
 * `GET /api/today?date=YYYY-MM-DD` 返回：当天计划安排、当天学习记录、
 * 已确认的复习任务、时长预算与后端给出的提示（warnings）。
 */
import { get, post } from '@/api/http'
import type { TodayResponse } from '../types'

/** 读取指定日期的今日安排。 */
export function fetchToday(date: string, signal?: AbortSignal): Promise<TodayResponse> {
  return get<TodayResponse>('/today', { params: { date }, signal })
}

export interface DayProgressPayload {
  date: string
  completionPercent: number
  durationMinutes: number
  clientToken: string
}
export function saveLearningProgress(taskId: number, revision: string, payload: DayProgressPayload): Promise<unknown> {
  return post('/learning/records/day-progress', { ...payload, taskId, revision })
}
export function saveReviewProgress(reviewItemId: number, version: number, result: string | null, payload: DayProgressPayload): Promise<unknown> {
  return post('/review/records/day-progress', { ...payload, reviewItemId, version, result })
}
