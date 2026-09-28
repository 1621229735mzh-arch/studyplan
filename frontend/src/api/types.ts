/**
 * HTTP 层的公共类型。
 *
 * 共享错误与分页结构定义在 src/types/api.ts，这里只补充与请求本身相关的类型。
 */

export type { ApiError, FieldViolation, PageResult } from '../types/api'
export { isApiError } from '../types/api'

/** GET /api/auth/csrf 的响应。 */
export interface CsrfTokenResponse {
  headerName: string
  parameterName: string
  token: string
}

/** GET /api/auth/me 与登录接口的响应。 */
export interface CurrentUserResponse {
  username: string | null
  authenticated: boolean
}

/** 分页与排序的通用查询参数。 */
export interface PageQuery {
  page?: number
  size?: number
}
