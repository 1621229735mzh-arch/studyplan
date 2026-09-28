import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'

import { isApiError, type ApiError, type FieldViolation, type PageResult } from '../types/api'
import type { CsrfTokenResponse } from './types'

/**
 * 唯一的 axios 实例：统一基地址、Cookie 携带、CSRF 请求头与错误归一化。
 *
 * 与后端约定（backend 的 SecurityConfig / SpaCsrfTokenRequestHandler）：
 * - 会话 Cookie 名为 SESSION，HttpOnly；
 * - CSRF 令牌以 XSRF-TOKEN Cookie 下发（HttpOnly=false，前端可读），
 *   写操作需要在请求头 X-XSRF-TOKEN 中回填该 Cookie 的原始值（不做 XOR 掩码校验）。
 */
const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'

/** CSRF Cookie 名称。 */
export const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
/** CSRF 请求头名称。 */
export const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'

const UNSAFE_METHODS = new Set(['post', 'put', 'patch', 'delete'])

/** 需要 CSRF 令牌的写方法。 */
export function isUnsafeMethod(method: string | undefined): boolean {
  return UNSAFE_METHODS.has((method ?? 'get').toLowerCase())
}

/**
 * 读取 Cookie。
 *
 * 单独抽出 cookieSource 参数是为了可测试：单元测试直接传入 Cookie 字符串，
 * 不依赖 jsdom 的 document.cookie。
 */
export function readCookie(name: string, cookieSource?: string): string | null {
  const source = cookieSource ?? (typeof document === 'undefined' ? '' : document.cookie)
  if (!source) {
    return null
  }
  for (const part of source.split(';')) {
    const separator = part.indexOf('=')
    if (separator < 0) {
      continue
    }
    if (part.slice(0, separator).trim() !== name) {
      continue
    }
    const rawValue = part.slice(separator + 1).trim()
    try {
      return decodeURIComponent(rawValue)
    } catch {
      return rawValue
    }
  }
  return null
}

/** 内存中的 CSRF 令牌：Cookie 不可读时（例如由代理改写）仍能使用 /auth/csrf 返回的值。 */
let csrfTokenInMemory: string | null = null

/** 写入内存令牌，同时保留 Cookie 作为首选来源。 */
export function setCsrfToken(token: string | null): void {
  csrfTokenInMemory = token
}

/** 取当前 CSRF 令牌：优先 Cookie，其次 /auth/csrf 的返回值。 */
export function getCsrfToken(cookieSource?: string): string | null {
  return readCookie(CSRF_COOKIE_NAME, cookieSource) ?? csrfTokenInMemory
}

/**
 * 计算写操作要附加的 CSRF 请求头。
 *
 * 纯函数，便于单元测试；返回空对象表示不需要附加（GET 或暂无令牌）。
 */
export function csrfHeaders(method: string | undefined, token: string | null): Record<string, string> {
  if (!token || !isUnsafeMethod(method)) {
    return {}
  }
  return { [CSRF_HEADER_NAME]: token }
}

/** 归一化后的接口错误，可像普通 ApiError 一样读取字段。 */
export class HttpApiError extends Error implements ApiError {
  readonly code: string
  readonly path: string
  readonly timestamp: string
  readonly fieldErrors: FieldViolation[]
  /** HTTP 状态码；网络不可达或超时为 0。 */
  readonly status: number

  constructor(init: {
    code: string
    message: string
    path?: string
    timestamp?: string
    fieldErrors?: FieldViolation[]
    status?: number
  }) {
    super(init.message)
    this.name = 'HttpApiError'
    this.code = init.code
    this.path = init.path ?? ''
    this.timestamp = init.timestamp ?? new Date().toISOString()
    this.fieldErrors = init.fieldErrors ?? []
    this.status = init.status ?? 0
  }
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return typeof value === 'object' && value !== null ? (value as Record<string, unknown>) : null
}

function normalizeFieldErrors(value: unknown): FieldViolation[] {
  if (!Array.isArray(value)) {
    return []
  }
  const violations: FieldViolation[] = []
  for (const item of value) {
    const record = asRecord(item)
    const field = record?.field
    const message = record?.message
    if (typeof field === 'string' && typeof message === 'string') {
      violations.push({ field, message })
    }
  }
  return violations
}

/**
 * 把任意异常归一化为 HttpApiError。
 *
 * 后端错误结构见 com.kaoyan.study.common.api.ApiError；结构不符时退回 HTTP 状态说明。
 */
export function toApiError(error: unknown): HttpApiError {
  if (error instanceof HttpApiError) {
    return error
  }
  if (error instanceof AxiosError) {
    const response = error.response
    if (response) {
      const path = response.config?.url ?? ''
      if (isApiError(response.data)) {
        return new HttpApiError({
          code: response.data.code,
          message: response.data.message,
          path: response.data.path ?? path,
          timestamp: response.data.timestamp,
          fieldErrors: normalizeFieldErrors(response.data.fieldErrors),
          status: response.status
        })
      }
      return new HttpApiError({
        code: `HTTP_${response.status}`,
        message: `请求失败（HTTP ${response.status}）`,
        path,
        status: response.status
      })
    }
    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      return new HttpApiError({ code: 'TIMEOUT', message: '请求超时，请稍后重试' })
    }
    return new HttpApiError({ code: 'NETWORK_ERROR', message: '网络不可用，请检查网络后重试' })
  }
  if (error instanceof Error) {
    return new HttpApiError({ code: 'UNKNOWN_ERROR', message: error.message })
  }
  return new HttpApiError({ code: 'UNKNOWN_ERROR', message: '发生未知错误' })
}

/** 是否属于“当前无法访问服务器”（离线、超时），用于离线降级判断。 */
export function isOfflineError(error: unknown): boolean {
  const apiError = toApiError(error)
  return apiError.code === 'NETWORK_ERROR' || apiError.code === 'TIMEOUT'
}

/** 会话失效时的统一处理（由 main.ts 注册，跳转登录页）。 */
export type UnauthorizedHandler = (error: ApiError) => void

let unauthorizedHandler: UnauthorizedHandler | null = null

/** 注册 401 处理；传 null 取消注册。 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler
}

/** 带重试标记的请求配置（内部使用）。 */
interface RetriableConfig extends InternalAxiosRequestConfig {
  __csrfRetried?: boolean
}

export const http = axios.create({
  baseURL: API_BASE,
  // 会话与 CSRF 都依赖 Cookie，必须始终携带凭证。
  withCredentials: true,
  headers: { Accept: 'application/json' },
  // 与后端 Cookie/请求头名称保持一致（默认值相同，这里显式声明以防被改动）。
  xsrfCookieName: CSRF_COOKIE_NAME,
  xsrfHeaderName: CSRF_HEADER_NAME
})

http.interceptors.request.use((config) => {
  for (const [name, value] of Object.entries(csrfHeaders(config.method, getCsrfToken()))) {
    config.headers.set(name, value)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    const axiosError = error instanceof AxiosError ? error : null
    const config = axiosError?.config as RetriableConfig | undefined
    const status = axiosError?.response?.status

    // 403 可能是 CSRF 令牌缺失或已过期（后端的 CSRF 失败与权限不足都返回 403 FORBIDDEN，
    // 无法从错误码区分）。因此对写操作只重试一次：重新取令牌后重放请求；
    // 若真的是权限问题，重放会再次 403，此时按原样报错，不会掩盖问题。
    if (status === 403 && config && isUnsafeMethod(config.method) && config.__csrfRetried !== true) {
      config.__csrfRetried = true
      try {
        const token = await fetchCsrfToken()
        config.headers.set(CSRF_HEADER_NAME, token)
        return await http.request(config)
      } catch {
        // 取令牌失败（例如离线）：继续走下面的错误归一化。
      }
    }

    const apiError = toApiError(error)
    if (apiError.status === 401 || apiError.code === 'UNAUTHORIZED') {
      unauthorizedHandler?.(apiError)
    }
    return Promise.reject(apiError)
  }
)

/** 拉取新的 CSRF 令牌并写入 Cookie（后端会同时下发 Cookie）。 */
export async function fetchCsrfToken(): Promise<string> {
  // 用裸 axios 调用：本实例的拦截器会在失败时再次触发取令牌逻辑。
  const response = await axios.get<CsrfTokenResponse>(`${API_BASE}/auth/csrf`, { withCredentials: true })
  const token = response.data?.token
  if (!token) {
    throw new HttpApiError({ code: 'CSRF_MISSING', message: '未能获取 CSRF 令牌', path: '/auth/csrf' })
  }
  setCsrfToken(token)
  return token
}

let primedRequest: Promise<string | null> | null = null

/**
 * 应用启动时预热 CSRF 令牌。
 *
 * 失败不抛错：离线启动时应用仍要能展示离线快照，真正的失败会在写操作时暴露。
 */
export function primeCsrfToken(): Promise<string | null> {
  if (primedRequest === null) {
    primedRequest = fetchCsrfToken()
      .catch(() => null)
      .finally(() => {
        primedRequest = null
      })
  }
  return primedRequest
}

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<T>(config)
  // 204 等空响应体时 response.data 为空字符串，调用方按 T 处理（例如 void）。
  return response.data as T
}

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request<T>({ ...config, url, method: 'get' })
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request<T>({ ...config, url, method: 'post', data })
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request<T>({ ...config, url, method: 'put', data })
}

export function patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request<T>({ ...config, url, method: 'patch', data })
}

export function del<T = void>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request<T>({ ...config, url, method: 'delete' })
}

/** 去掉查询参数中的空值，避免把 undefined 拼进 URL（例如 form=undefined）。 */
export function cleanParams(input: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(input)) {
    if (value === undefined || value === null || value === '') {
      continue
    }
    result[key] = value
  }
  return result
}

/**
 * 列表类接口既可能返回数组，也可能返回分页结构，这里统一取数组。
 *
 * 列表接口是并行开发中的接口，后端最终形态确定后只需调整此函数。
 */
export function asList<T>(response: T[] | PageResult<T> | null | undefined): T[] {
  if (Array.isArray(response)) {
    return response
  }
  return response?.items ?? []
}
