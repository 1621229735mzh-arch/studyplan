/**
 * Service Worker 注册与应用外壳更新提示。
 *
 * 只注册由 vite-plugin-pwa 生成的外壳缓存（workbox precache），
 * 不对 /api 做任何运行时缓存；业务数据的离线能力只由 src/offline 提供。
 */
import { readonly, ref } from 'vue'
import { registerSW } from 'virtual:pwa-register'

const needRefresh = ref(false)
const offlineReady = ref(false)

let updateServiceWorker: ((reloadPage?: boolean) => Promise<void>) | null = null
let initialized = false

/** 生产环境注册 Service Worker；开发环境不注册，避免缓存干扰调试。 */
export function initServiceWorker(): void {
  if (initialized || import.meta.env.DEV) {
    return
  }
  initialized = true
  updateServiceWorker = registerSW({
    immediate: true,
    onNeedRefresh() {
      needRefresh.value = true
    },
    onOfflineReady() {
      offlineReady.value = true
    },
    onRegisteredSW(_swUrl, registration) {
      if (!registration) {
        return
      }
      // 定期检查更新，但只在联网时执行。
      setInterval(() => {
        if (typeof navigator !== 'undefined' && navigator.onLine) {
          void registration.update()
        }
      }, 60 * 60 * 1000)
    }
  })
}

/** 供更新提示组件使用的只读状态与操作。 */
export function useAppUpdate() {
  return {
    needRefresh: readonly(needRefresh),
    offlineReady: readonly(offlineReady),
    /** 应用更新（会重新加载页面） */
    applyUpdate: async (): Promise<void> => {
      await updateServiceWorker?.(true)
      needRefresh.value = false
    },
    dismiss: (): void => {
      needRefresh.value = false
      offlineReady.value = false
    }
  }
}
