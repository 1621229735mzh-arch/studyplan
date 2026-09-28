/**
 * 会话状态：当前登录用户、登录/退出与身份确认。
 *
 * 关键约定：
 * - 退出登录必须清理本地离线快照（个人数据不留在浏览器）；
 * - 离线（连不上后端）时，如果本地快照记录过登录名，允许只读查看快照，
 *   但不会伪装成“已向后端确认登录”，界面会显示离线提示。
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { isOfflineError, toApiError } from '@/api/http'
import { fetchCurrentUser, login as loginRequest, logout as logoutRequest } from '@/features/account/api'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import type { FieldViolation } from '@/types/api'

export const useSessionStore = defineStore('session', () => {
  const username = ref<string | null>(null)
  const authenticated = ref(false)
  /** 是否已经向后端确认过身份（未确认时路由守卫会调用 fetchMe） */
  const resolved = ref(false)
  const loading = ref(false)
  /** 本地有快照但当前连不上后端时的离线只读会话 */
  const offlineSession = ref(false)
  const errorMessage = ref<string | null>(null)
  const errorFieldErrors = ref<FieldViolation[]>([])

  const isAuthenticated = computed(() => authenticated.value)
  const displayName = computed(() => username.value ?? '未登录')

  function clearError(): void {
    errorMessage.value = null
    errorFieldErrors.value = []
  }

  function applyAnonymous(): void {
    username.value = null
    authenticated.value = false
    offlineSession.value = false
  }

  /** 401 时由 http 层调用：立即按未登录处理。 */
  function markAnonymous(): void {
    applyAnonymous()
    resolved.value = true
  }

  function reset(): void {
    applyAnonymous()
    resolved.value = false
    clearError()
  }

  /** 查询当前身份；resolved 之后默认不重复请求，force=true 时强制刷新。 */
  async function fetchMe(force = false): Promise<void> {
    if (resolved.value && !force) {
      return
    }
    loading.value = true
    try {
      const current = await fetchCurrentUser()
      if (current.authenticated) {
        username.value = current.username
        authenticated.value = true
        offlineSession.value = false
      } else {
        applyAnonymous()
      }
    } catch (error) {
      if (isOfflineError(error)) {
        // 离线：允许用本地快照只读查看，用户名取自快照元信息。
        const offline = useOfflineSnapshotStore()
        await offline.load()
        if (offline.meta !== null && offline.meta.userName) {
          username.value = offline.meta.userName
          authenticated.value = true
          offlineSession.value = true
        } else {
          applyAnonymous()
        }
      } else {
        applyAnonymous()
      }
    } finally {
      resolved.value = true
      loading.value = false
    }
  }

  /** 登录；失败时保留表单（表单状态在视图里）并返回 false。 */
  async function login(payload: { username: string; password: string }): Promise<boolean> {
    loading.value = true
    clearError()
    try {
      const current = await loginRequest(payload)
      username.value = current.username
      authenticated.value = true
      offlineSession.value = false
      resolved.value = true
      // 切换账号时清掉上一个账号的离线快照，避免串数据。
      const offline = useOfflineSnapshotStore()
      await offline.load()
      if (offline.meta !== null && offline.meta.userName !== current.username) {
        await offline.clear()
      }
      return true
    } catch (error) {
      const apiError = toApiError(error)
      errorMessage.value = apiError.message
      errorFieldErrors.value = apiError.fieldErrors
      applyAnonymous()
      resolved.value = true
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 退出登录。
   *
   * 即使退出接口失败（例如离线），也必须清理本地个人数据；
   * 服务端会话超时后同样会失效。
   */
  async function logout(): Promise<void> {
    loading.value = true
    try {
      await logoutRequest()
    } catch (error) {
      console.warn('[session] 退出接口调用失败，仍会清理本地数据', toApiError(error).message)
    } finally {
      const offline = useOfflineSnapshotStore()
      await offline.clear()
      reset()
      loading.value = false
    }
  }

  return {
    username,
    authenticated,
    resolved,
    loading,
    offlineSession,
    errorMessage,
    errorFieldErrors,
    isAuthenticated,
    displayName,
    clearError,
    markAnonymous,
    reset,
    fetchMe,
    login,
    logout
  }
})
