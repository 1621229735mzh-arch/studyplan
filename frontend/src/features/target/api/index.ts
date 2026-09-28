/**
 * 候选目标接口。
 */
import { asList, cleanParams, del, get, patch, post, put } from '@/api/http'
import type { PageResult } from '@/types/api'
import type { Target, TargetRequest, TargetStatus, TargetStatusRequest } from '../types'

/** 候选目标列表（可按状态筛选）。 */
export async function listTargets(status?: TargetStatus): Promise<Target[]> {
  return asList(await get<Target[] | PageResult<Target>>('/targets', { params: cleanParams({ status }) }))
}

/** 新增候选目标；version 传 0。 */
export function createTarget(payload: TargetRequest): Promise<Target> {
  return post<Target>('/targets', payload)
}

/** 修改候选目标；payload 需包含 version，链接整体替换。 */
export function updateTarget(id: number, payload: TargetRequest): Promise<Target> {
  return put<Target>(`/targets/${id}`, payload)
}

/** 删除候选目标。 */
export function deleteTarget(id: number): Promise<void> {
  return del<void>(`/targets/${id}`)
}

/** 只修改状态（候选中 / 已选定 / 已放弃）；必须带上 version。 */
export function changeTargetStatus(id: number, status: TargetStatus, version: number): Promise<Target> {
  const payload: TargetStatusRequest = { status, version }
  return patch<Target>(`/targets/${id}/status`, payload)
}
