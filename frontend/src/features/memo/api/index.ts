/**
 * 备忘录接口。
 *
 * “转为任务”使用后端专门接口 `POST /api/memo/{id}/convert-to-task`：
 * 任务由计划模块创建、备忘录原文保留，不需要前端“先建任务再回写备忘录”。
 */
import { asList, cleanParams, del, get, post, put } from '@/api/http'
import type { PageResult } from '@/types/api'
import type { Memo, MemoConvertRequest, MemoQuery, MemoRequest } from '../types'

/** 备忘录列表（支持状态、科目与关键字筛选）。 */
export async function listMemos(query: MemoQuery = {}): Promise<Memo[]> {
  const response = await get<Memo[] | PageResult<Memo>>('/memo', { params: cleanParams({ ...query }) })
  return asList(response)
}

/** 到期提醒列表（默认当天，含此前未完成的备忘录）。 */
export async function listDueMemos(date?: string): Promise<Memo[]> {
  return asList(await get<Memo[] | PageResult<Memo>>('/memo/due', { params: cleanParams({ date }) }))
}

/** 单条备忘录。 */
export function getMemo(id: number): Promise<Memo> {
  return get<Memo>(`/memo/${id}`)
}

/** 新建备忘录；version 传 0。 */
export function createMemo(payload: MemoRequest): Promise<Memo> {
  return post<Memo>('/memo', payload)
}

/** 修改备忘录；payload 需包含 version。 */
export function updateMemo(id: number, payload: MemoRequest): Promise<Memo> {
  return put<Memo>(`/memo/${id}`, payload)
}

/** 删除备忘录（不删除已转成的任务）。 */
export function deleteMemo(id: number): Promise<void> {
  return del<void>(`/memo/${id}`)
}

/** 忽略本次到期提醒。 */
export function dismissMemoReminder(id: number): Promise<Memo> {
  return post<Memo>(`/memo/${id}/dismiss-reminder`)
}

/** 转为计划任务；返回更新后的备忘录（含 convertedTaskId）。 */
export function convertMemoToTask(id: number, payload: MemoConvertRequest): Promise<Memo> {
  return post<Memo>(`/memo/${id}/convert-to-task`, payload)
}
