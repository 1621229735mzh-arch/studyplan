/**
 * 离线快照的唯一访问入口。
 *
 * 其它任何模块（视图、store、组件）都必须通过这个 store 读写本地缓存，
 * 不允许直接 import src/offline/snapshot.ts，以免缓存范围失控。
 */
import { defineStore } from 'pinia'
import { computed, shallowRef } from 'vue'
import { todayIso } from '@/composables/useFormat'

import type { WeekPlanResponse } from '../features/plan/types'
import type { ProgressOverview } from '../features/progress/types'
import type { TodayResponse } from '../features/today/types'
import { clearAll, readSnapshot, saveSnapshot } from './snapshot'
import type { OfflineSnapshotMeta, SaveSnapshotInput } from './types'

export const useOfflineSnapshotStore = defineStore('offlineSnapshot', () => {
  // 快照整体替换而不是细粒度修改，用 shallowRef 避免对大对象做深层响应式代理。
  const meta = shallowRef<OfflineSnapshotMeta | null>(null)
  const today = shallowRef<TodayResponse | null>(null)
  const week = shallowRef<WeekPlanResponse | null>(null)
  const progress = shallowRef<ProgressOverview | null>(null)
  const loading = shallowRef(false)

  let loaded = false

  const hasSnapshot = computed(() => meta.value !== null)
  const syncedAt = computed(() => meta.value?.syncedAt ?? null)
  const userName = computed(() => meta.value?.userName ?? null)
  const hasToday = computed(() => today.value !== null)
  const hasWeek = computed(() => week.value !== null)
  const hasProgress = computed(() => progress.value !== null)

  /** 读取本地快照；重复调用直接复用已读取的数据，force=true 时重新读取。 */
  async function load(force = false): Promise<void> {
    if (loaded && !force) {
      return
    }
    loading.value = true
    try {
      const snapshot = await readSnapshot()
      meta.value = snapshot?.meta ?? null
      today.value = snapshot?.today ?? null
      week.value = snapshot?.week ?? null
      progress.value = snapshot?.progress ?? null
      loaded = true
    } finally {
      loading.value = false
    }
  }

  /** 在线取到新数据后写入快照（只更新传入的切片）。 */
  async function save(input: SaveSnapshotInput): Promise<void> {
    meta.value = await saveSnapshot(input)
    if (input.today === null || input.today?.date === todayIso()) {
      today.value = input.today
    }
    if (input.week !== undefined) {
      week.value = input.week
    }
    if (input.progress !== undefined) {
      progress.value = input.progress
    }
    loaded = true
  }

  /** 退出登录必须调用：清理全部本地个人数据。 */
  async function clear(): Promise<void> {
    await clearAll()
    meta.value = null
    today.value = null
    week.value = null
    progress.value = null
    loaded = false
  }

  return {
    meta,
    today,
    week,
    progress,
    loading,
    hasSnapshot,
    hasToday,
    hasWeek,
    hasProgress,
    syncedAt,
    userName,
    load,
    save,
    clear
  }
})
