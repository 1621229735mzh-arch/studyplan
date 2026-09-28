/**
 * 离线快照的类型定义。
 *
 * 这里用 `import type` 引用各功能的响应类型：类型在编译后被擦除，
 * 不会造成运行时的循环依赖，同时保证缓存结构与接口响应结构始终一致。
 */
import type { WeekPlanResponse } from '../features/plan/types'
import type { ProgressOverview } from '../features/progress/types'
import type { TodayResponse } from '../features/today/types'

/** 同步元信息：只有登录并成功同步过的用户才会写入。 */
export interface OfflineSnapshotMeta {
  /** 上次成功同步的登录名 */
  userName: string
  /** 上次成功同步时间（ISO 字符串） */
  syncedAt: string
}

/** 单个切片的存储结构，带自己的同步时间。 */
export interface SnapshotEnvelope<T> {
  syncedAt: string
  data: T
}

/** 第一版离线只缓存这三块数据；新增范围必须同步更新此处与使用方。 */
export interface OfflineSnapshotData {
  today: TodayResponse | null
  week: WeekPlanResponse | null
  progress: ProgressOverview | null
}

export type SnapshotSlice = keyof OfflineSnapshotData

/** 读取结果：元信息加上三个切片。 */
export interface OfflineSnapshot extends OfflineSnapshotData {
  meta: OfflineSnapshotMeta
}

/** 写入参数：登录名必填，切片按需提供（只更新传入的切片，不影响其它切片）。 */
export type SaveSnapshotInput = { userName: string } & Partial<OfflineSnapshotData>
