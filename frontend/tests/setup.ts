import { afterEach, vi } from 'vitest'

/**
 * 测试环境准备。
 *
 * jsdom 没有实现 matchMedia，而 Element Plus 的部分组件会读取它，
 * 因此这里提供一个最小实现，避免组件测试报错。
 */
if (typeof window !== 'undefined' && typeof window.matchMedia !== 'function') {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn()
    })
  })
}

/** jsdom 默认没有 IndexedDB，src/offline 会自动退回到内存实现（见 src/offline/db.ts）。 */

afterEach(() => {
  // 清理 Cookie，避免测试之间互相影响
  for (const cookie of document.cookie.split(';')) {
    const name = cookie.split('=')[0]?.trim()
    if (name) {
      document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
    }
  }
  document.body.innerHTML = ''
})
