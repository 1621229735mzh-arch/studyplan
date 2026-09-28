import { ref, type Ref } from 'vue'

/**
 * 全局网络状态（模块级单例）。
 *
 * 布局、离线提示与各视图共享同一份状态，避免每个组件各自绑定监听。
 */
const online = ref(typeof navigator === 'undefined' ? true : navigator.onLine)

let listening = false

function ensureListeners(): void {
  if (listening || typeof window === 'undefined') {
    return
  }
  listening = true
  window.addEventListener('online', () => {
    online.value = true
  })
  window.addEventListener('offline', () => {
    online.value = false
  })
}

/** 订阅网络状态。 */
export function useOnlineStatus(): { online: Ref<boolean> } {
  ensureListeners()
  return { online }
}

/** 当前是否联网。 */
export function isOnline(): boolean {
  return online.value
}
