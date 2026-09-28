/**
 * 离线快照读写。
 *
 * 范围被刻意限制为：今日、本周、进度概览 + 同步元信息。
 * 不提供“任意 key 缓存”能力，避免退化成通用接口缓存。
 */
import { kvClear, kvGet, kvPut } from './db'
import { todayIso } from '@/composables/useFormat'
import type {
  OfflineSnapshot,
  OfflineSnapshotData,
  OfflineSnapshotMeta,
  SaveSnapshotInput,
  SnapshotEnvelope
} from './types'

const META_KEY = 'meta'
const TODAY_KEY = 'today'
const WEEK_KEY = 'week'
const PROGRESS_KEY = 'progress'

/** 写入快照：只覆盖传入的切片，元信息同步更新为当前时间。 */
export async function saveSnapshot(input: SaveSnapshotInput): Promise<OfflineSnapshotMeta> {
  const syncedAt = new Date().toISOString()
  if (input.today === null || input.today?.date === todayIso()) {
    await kvPut(TODAY_KEY, { syncedAt, data: input.today } satisfies SnapshotEnvelope<OfflineSnapshotData['today']>)
  }
  if (input.week !== undefined) {
    await kvPut(WEEK_KEY, { syncedAt, data: input.week } satisfies SnapshotEnvelope<OfflineSnapshotData['week']>)
  }
  if (input.progress !== undefined) {
    await kvPut(
      PROGRESS_KEY,
      { syncedAt, data: input.progress } satisfies SnapshotEnvelope<OfflineSnapshotData['progress']>
    )
  }
  const meta: OfflineSnapshotMeta = { userName: input.userName, syncedAt }
  await kvPut(META_KEY, meta)
  return meta
}

/** 读取同步元信息；没有缓存过时返回 null。 */
export async function readSnapshotMeta(): Promise<OfflineSnapshotMeta | null> {
  return kvGet<OfflineSnapshotMeta>(META_KEY)
}

/** 读取全部快照；没有缓存过时返回 null。 */
export async function readSnapshot(): Promise<OfflineSnapshot | null> {
  const meta = await readSnapshotMeta()
  if (meta === null) {
    return null
  }
  const today = await kvGet<SnapshotEnvelope<OfflineSnapshotData['today']>>(TODAY_KEY)
  const week = await kvGet<SnapshotEnvelope<OfflineSnapshotData['week']>>(WEEK_KEY)
  const progress = await kvGet<SnapshotEnvelope<OfflineSnapshotData['progress']>>(PROGRESS_KEY)
  return {
    meta,
    today: today?.data?.date === todayIso() ? today.data : null,
    week: week?.data ?? null,
    progress: progress?.data ?? null
  }
}

/** 清空全部本地离线数据（退出登录、切换账号时调用）。 */
export async function clearAll(): Promise<void> {
  await kvClear()
}
