/**
 * 候选目标（院校/专业）数据类型（对应 backend 的 com.kaoyan.study.target.dto）。
 *
 * 状态取值以后端为准：CANDIDATE / CHOSEN / DROPPED。
 * 参考链接是目标的一部分（links），随目标整体提交、整体替换，不是单个 link 字段。
 */

/** 候选目标状态（对应后端 status 字段的取值）。 */
export type TargetStatus = 'CANDIDATE' | 'CHOSEN' | 'DROPPED'

/** 参考链接（对应后端 TargetResponse.LinkResponse）。 */
export interface TargetLink {
  id: number
  url: string
  label?: string | null
  sortOrder: number
}

/** 提交的参考链接（对应后端 TargetRequest.LinkRequest）。 */
export interface TargetLinkRequest {
  url: string
  label?: string | null
}

/** 候选目标（对应后端 TargetResponse）。 */
export interface Target {
  id: number
  schoolName: string
  majorName?: string | null
  note?: string | null
  /** 后端返回字符串状态，取值见 TargetStatus */
  status: string
  sortOrder: number
  links: TargetLink[]
  createdAt?: string | null
  updatedAt?: string | null
  version: number
}

/** 新建/修改候选目标（后端 TargetRequest）；version 必填（新建传 0）。 */
export interface TargetRequest {
  schoolName: string
  majorName?: string | null
  note?: string | null
  /** 为空表示新建用 CANDIDATE、修改保持原状态 */
  status?: TargetStatus | null
  sortOrder?: number | null
  /** 提交的列表就是目标现有的全部链接，后端按提交顺序整体替换 */
  links?: TargetLinkRequest[]
  version: number
}

/** 只修改状态（后端 TargetStatusRequest）；同样需要 version。 */
export interface TargetStatusRequest {
  status: TargetStatus
  version: number
}

export const TARGET_STATUS_LABELS: Record<TargetStatus, string> = {
  CANDIDATE: '候选中',
  CHOSEN: '已选定',
  DROPPED: '已放弃'
}

export const TARGET_STATUS_OPTIONS: TargetStatus[] = ['CANDIDATE', 'CHOSEN', 'DROPPED']

/** 状态展示；未知取值原样显示，不猜测含义。 */
export function targetStatusLabel(status?: string | null): string {
  if (!status) {
    return '—'
  }
  return TARGET_STATUS_LABELS[status as TargetStatus] ?? status
}
