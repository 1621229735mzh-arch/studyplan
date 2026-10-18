import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { toApiError } from '@/api/http'
import { todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import { useSessionStore } from '@/stores/session'
import { getCatalog } from './api/catalog'
import type { CatalogLibrary, CatalogSummary } from './catalog'

// 跨方向页实例也共用写入顺序，旧页面正在进行的写入不会晚于新页面的结果落盘。
let snapshotWrites = Promise.resolve()

export function useCatalogWorkspace() {
  const { online } = useOnlineStatus()
  const cache = useOfflineSnapshotStore()
  const session = useSessionStore()
  const library = ref<CatalogLibrary | null>(null)
  const loading = ref(false)
  const error = ref('')
  const notice = ref('')
  const readonly = ref(!online.value)
  const syncedAt = ref('')
  let request = 0
  let controller: AbortController | null = null
  async function snapshot(value: CatalogLibrary, id: number) {
    const summary: CatalogSummary = { revision: value.revision, cumulative: value.cumulative, activities: value.activities, courses: value.courses.map(({ nodes: _nodes, completedProblemNumbers: _numbers, ...course }) => course) }
    const timestamp = new Date().toISOString()
    if (id === request) syncedAt.value = timestamp
    const username = session.username
    // 按顺序写快照，跳过排队时已过期的数据；旧写入结束后不会覆盖新状态提示。
    snapshotWrites = snapshotWrites.then(async () => {
      if (id !== request || !username || session.username !== username) return
      try { await cache.save({ userName: username, progress: { from: todayIso(), to: todayIso(), items: [], trend: [], planningBasis: 'catalog', syncedAt: timestamp, catalog: summary } }) }
      catch { if (id === request) notice.value = '在线数据已更新，离线快照未保存。' }
    })
    await snapshotWrites
  }
  async function cached(id: number) {
    try { await cache.load() }
    catch { if (id === request) notice.value = '无法读取本机进度快照，请联网重新同步。'; return }
    if (id !== request) return
    if (cache.progress?.planningBasis !== 'catalog' || !cache.progress.catalog) {
      notice.value = '没有当前目录口径的进度快照，请联网刷新。'
      return
    }
    const saved = cache.progress.catalog
    library.value = { ...saved, courses: saved.courses.map(c => ({ ...c, nodes: [] })), unclassified: [] }
    syncedAt.value = cache.progress.syncedAt || ''
    readonly.value = true
    notice.value = '正在查看进度概览快照；完整目录和归类需联网。'
  }
  async function reload() {
    const id = ++request
    controller?.abort()
    library.value = null; error.value = ''; notice.value = ''; syncedAt.value = ''
    readonly.value = !online.value
    loading.value = true
    try {
      if (!online.value) { await cached(id); return }
      controller = new AbortController()
      const value = await getCatalog(controller.signal)
      if (id !== request) return
      library.value = value
      await snapshot(value, id)
    } catch (caught) {
      if (id !== request) return
      const failure = toApiError(caught)
      error.value = failure.message
      if (failure.status === 0 || failure.status >= 500) await cached(id)
    } finally { if (id === request) loading.value = false }
  }
  async function accept(value: CatalogLibrary) {
    const id = ++request; controller?.abort(); library.value = value; loading.value = false; error.value = ''; notice.value = ''; readonly.value = !online.value
    await snapshot(value, id)
  }
  watch(online, () => { void reload() })
  onMounted(() => { void reload() })
  onBeforeUnmount(() => { ++request; controller?.abort() })
  return { library, loading, error, notice, readonly, syncedAt, online, reload, accept }
}
