/**
 * HTTP 层的公共类型。
 *
 * 共享错误结构定义在 src/types/api.ts，这里只定义认证接口的响应类型。
 */

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
