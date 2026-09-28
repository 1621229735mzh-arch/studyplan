/**
 * 进度接口（只读统计，结果全部由后端计算）。
 */
import { cleanParams, get } from '@/api/http'
import type { ProgressOverview, TrendPointView } from '../types'

/** 各科进度概览（按科目+单位分组的计划与实际，含趋势）。 */
export function getProgressOverview(
  params: { from?: string; to?: string } = {},
  signal?: AbortSignal
): Promise<ProgressOverview> {
  return get<ProgressOverview>('/progress/overview', { params: cleanParams({ ...params }), signal })
}

/** 学习量与时长趋势；后端返回裸数组（不是带 from/to 的对象）。 */
export function getProgressTrend(from: string, to: string, signal?: AbortSignal): Promise<TrendPointView[]> {
  return get<TrendPointView[]>('/progress/trend', { params: cleanParams({ from, to }), signal })
}
