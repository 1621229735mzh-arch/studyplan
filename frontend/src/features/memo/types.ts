/**
 * 备忘录数据类型（对应 backend 的 com.kaoyan.study.memo.dto）。
 *
 * 站内到期提醒 + 转为任务。转任务走后端专门的
 * `POST /api/memo/{id}/convert-to-task`，不在这里另造任务写入路径。
 */

/** 备忘状态；后端仅使用 OPEN / DONE（响应里为字符串）。 */
export type MemoStatus = 'OPEN' | 'DONE'

/** 备忘录（对应后端 MemoResponse）。 */
export interface Memo {
  id: number
  subjectId?: number | null
  content: string
  /** 到期日（后端字段名为 dueDate，不是 remindAt） */
  dueDate?: string | null
  status: string
  /** 已转换出的任务 id（后端字段名为 convertedTaskId，不是 taskId） */
  convertedTaskId?: number | null
  /** 是否已忽略本次到期提醒 */
  reminderDismissed: boolean
  createdAt?: string | null
  updatedAt?: string | null
  version: number
}

/** 新建/修改备忘录（后端 MemoRequest）；version 必填（新建传 0）。 */
export interface MemoRequest {
  subjectId?: number | null
  content: string
  dueDate?: string | null
  /** 为空表示新建用 OPEN、修改保持原状态 */
  status?: MemoStatus | null
  version: number
}

/** 转为计划任务（后端 MemoConvertRequest）：题目、科目与单位由本人确认后提交。 */
export interface MemoConvertRequest {
  taskTitle: string
  subjectId: number
  unitId: number
  /** 计划量可以为空，表示尚未确定；为空不视为 0 */
  plannedAmount?: number | null
}

/** 列表查询条件（对应后端 /api/memo 的 status / subjectId / keyword）。 */
export interface MemoQuery {
  status?: string
  subjectId?: number
  keyword?: string
}

export const MEMO_STATUS_LABELS: Record<MemoStatus, string> = {
  OPEN: '未完成',
  DONE: '已完成'
}

export const MEMO_STATUS_OPTIONS: MemoStatus[] = ['OPEN', 'DONE']

/** 备忘状态展示；未知取值原样显示。 */
export function memoStatusLabel(status?: string | null): string {
  if (!status) {
    return '—'
  }
  return MEMO_STATUS_LABELS[status as MemoStatus] ?? status
}
