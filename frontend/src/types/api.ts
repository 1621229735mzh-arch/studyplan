/**
 * 后端统一错误结构与分页结构（对应 backend 的
 * com.kaoyan.study.common.api.ApiError / PageResult）。
 */

/** 单个字段的校验失败信息。 */
export interface FieldViolation {
  field: string
  message: string
}

/**
 * 统一接口错误。
 *
 * 后端在任何非 2xx 响应里都返回这个结构；前端只做展示，不据此推断业务结论。
 */
export interface ApiError {
  /** 稳定的机器可读错误码，例如 UNAUTHORIZED / FORBIDDEN / VALIDATION_FAILED */
  code: string
  /** 面向使用者的中文说明 */
  message: string
  /** 出错请求路径 */
  path: string
  /** 服务端时间（ISO 字符串） */
  timestamp: string
  /** 参数校验失败的字段明细，可为空数组 */
  fieldErrors: FieldViolation[]
}

/** 分页结果。 */
export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  total: number
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return typeof value === 'object' && value !== null ? (value as Record<string, unknown>) : null
}

/** 判断任意值是否符合后端错误结构（用于把响应体安全地归一化）。 */
export function isApiError(value: unknown): value is ApiError {
  const record = asRecord(value)
  if (record === null) {
    return false
  }
  return typeof record.code === 'string' && typeof record.message === 'string'
}
