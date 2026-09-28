import { describe, expect, it } from 'vitest'

import { CSRF_COOKIE_NAME, CSRF_HEADER_NAME, csrfHeaders, isUnsafeMethod, readCookie } from '@/api/http'

/**
 * CSRF 辅助方法测试。
 *
 * 与后端约定（backend 的 SecurityConfig / SpaCsrfTokenRequestHandler）：
 * Cookie 名 XSRF-TOKEN、请求头名 X-XSRF-TOKEN，且请求头使用 Cookie 中的原始值（不做掩码）。
 */
describe('CSRF 辅助方法', () => {
  it('与后端约定保持一致（Cookie 名与请求头名）', () => {
    expect(CSRF_COOKIE_NAME).toBe('XSRF-TOKEN')
    expect(CSRF_HEADER_NAME).toBe('X-XSRF-TOKEN')
  })

  it('从 Cookie 字符串中读取并解码令牌', () => {
    const cookie = 'SESSION=abc123; XSRF-TOKEN=token%2Dvalue; other=1'
    expect(readCookie(CSRF_COOKIE_NAME, cookie)).toBe('token-value')
  })

  it('Cookie 中不存在时返回 null', () => {
    expect(readCookie(CSRF_COOKIE_NAME, 'SESSION=abc123')).toBeNull()
    expect(readCookie(CSRF_COOKIE_NAME, '')).toBeNull()
  })

  it('能从 document.cookie 读取（jsdom 环境）', () => {
    document.cookie = 'XSRF-TOKEN=jsdom-token; path=/'
    expect(readCookie(CSRF_COOKIE_NAME)).toBe('jsdom-token')
  })

  it('只对写方法附加 CSRF 请求头', () => {
    expect(csrfHeaders('post', 'tok')).toEqual({ [CSRF_HEADER_NAME]: 'tok' })
    expect(csrfHeaders('PUT', 'tok')).toEqual({ [CSRF_HEADER_NAME]: 'tok' })
    expect(csrfHeaders('delete', 'tok')).toEqual({ [CSRF_HEADER_NAME]: 'tok' })
    expect(csrfHeaders('get', 'tok')).toEqual({})
    expect(csrfHeaders(undefined, 'tok')).toEqual({})
  })

  it('没有令牌时不附加请求头', () => {
    expect(csrfHeaders('post', null)).toEqual({})
  })

  it('写方法判断', () => {
    expect(isUnsafeMethod('PATCH')).toBe(true)
    expect(isUnsafeMethod('head')).toBe(false)
    expect(isUnsafeMethod(undefined)).toBe(false)
  })
})
