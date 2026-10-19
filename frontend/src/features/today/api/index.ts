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
  problemNumber?: number | null
  date: string
  completionPercent: number
  durationMinutes?: number | null
  clientToken: string
  actualAmount?: number | null
  note?: string | null
}
export function saveLearningProgress(taskId: number, revision: string, payload: DayProgressPayload): Promise<unknown> {
  return post('/learning/records/day-progress', { ...payload, taskId, revision })
}
export function saveCompletedProblemNumbers(taskId: number, date: string, problemNumbers: number[], revision: string, clientToken: string): Promise<unknown> {
  return post('/learning/records/problem-numbers', {taskId, date, problemNumbers, revision, clientToken})
}
export function saveDayConfirmation(taskId: number, date: string, completed: boolean, revision: string, clientToken: string): Promise<unknown> {
  return post('/learning/records/day-confirmation', {taskId, date, completed, revision, clientToken})
}
export function saveReviewProgress(reviewItemId: number, version: number, result: string | null, payload: DayProgressPayload): Promise<unknown> {
  return post('/review/records/day-progress', { ...payload, reviewItemId, version, result })
}
