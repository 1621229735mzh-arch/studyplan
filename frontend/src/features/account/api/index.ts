/**
 * 账号模块接口（对应 backend 的 AuthController）。
 *
 * 注意：CSRF 令牌的获取、请求头回填与 403 重试都在 src/api/http.ts 中统一处理，
 * 这里只负责业务路径与载荷。
 */
import { get, post } from '@/api/http'
import type { CurrentUserResponse } from '@/api/types'

/** 查询当前身份；未登录时返回 authenticated=false（HTTP 200）。 */
export function fetchCurrentUser(): Promise<CurrentUserResponse> {
  return get<CurrentUserResponse>('/auth/me')
}

/** 登录。成功后服务端会更换会话 ID。 */
export function login(payload: { username: string; password: string }): Promise<CurrentUserResponse> {
  return post<CurrentUserResponse>('/auth/login', payload)
}

/** 退出登录；后端返回 204。 */
export function logout(): Promise<void> {
  return post<void>('/auth/logout')
}
