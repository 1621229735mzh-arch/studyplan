import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import AppLayout from '@/layouts/AppLayout.vue'

const mocks = vi.hoisted(() => ({ logout: vi.fn(), load: vi.fn(), online: { value: true } }))
vi.mock('@/stores/session', () => ({ useSessionStore: () => ({ displayName: '演示账号', logout: mocks.logout }) }))
vi.mock('@/stores/catalog', () => ({ useCatalogStore: () => ({ load: mocks.load }) }))
vi.mock('@/composables/useOnlineStatus', async () => {
  const { ref } = await import('vue')
  mocks.online = ref(true)
  return { useOnlineStatus: () => ({ online: mocks.online }) }
})
vi.mock('@/offline/useOfflineSnapshot', () => ({ useOfflineSnapshotStore: () => ({ syncedAt: '2026-10-18T08:00:00Z' }) }))
vi.mock('@/components/OfflineNotice.vue', () => ({ default: { template: '<div />' } }))

async function page(path = '/') {
  const placeholder = { template: '<div>页面内容</div>' }
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/', component: AppLayout, children: [
      { path: '', name: 'today', component: placeholder },
      ...['plan', 'learning', 'progress', 'review', 'memo', 'targets', 'settings'].map(path => ({ path, name: path, component: placeholder })),
      { path: 'progress/:direction', name: 'direction', component: placeholder }
    ] },
    { path: '/login', name: 'login', component: placeholder }
  ] })
  await router.push(path)
  const wrapper = mount(AppLayout, { global: { plugins: [router, ElementPlus], stubs: {
    ElDrawer: { props: ['modelValue'], template: '<section v-if="modelValue"><slot /></section>' }
  } } })
  await flushPromises()
  return { wrapper, router }
}
beforeEach(() => { vi.clearAllMocks(); mocks.online.value = true; mocks.logout.mockResolvedValue(undefined) })

describe('响应式导航', () => {
  it('课程详情仍选中进度，手机保留四个常用入口及全部更多页面', async () => {
    const { wrapper } = await page('/progress/CS408')
    const nav = wrapper.get('[aria-label="手机主导航"]')
    expect(nav.findAll('a').map(a => a.text())).toEqual(['今日', '计划', '记录', '进度'])
    expect(nav.get('a[href="/progress"]').attributes('aria-current')).toBe('page')
    await nav.get('button').trigger('click')
    expect(wrapper.get('[aria-label="更多页面"]').findAll('a').map(a => a.text())).toEqual(['复习', '备忘录', '目标', '设置'])
    wrapper.unmount()
  })
  it('从更多进入页面后关闭菜单，更多保持当前页高亮', async () => {
    const { wrapper, router } = await page()
    await wrapper.get('.mobile-navigation button').trigger('click')
    await wrapper.get('.more-menu a[href="/settings"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/settings')
    expect(wrapper.find('.more-menu').exists()).toBe(false)
    expect(wrapper.get('.mobile-navigation button').classes()).toContain('is-active')
    expect(wrapper.get('.mobile-navigation button').attributes('aria-expanded')).toBe('false')
    wrapper.unmount()
  })
  it('离线时手机更多中仍能查看同步时间并通过原会话服务退出', async () => {
    mocks.online.value = false
    const { wrapper, router } = await page()
    await wrapper.get('.mobile-navigation button').trigger('click')
    expect(wrapper.get('.more-session').text()).toContain('离线只读')
    expect(wrapper.get('.more-session').text()).toContain('最后同步')
    await wrapper.get('.more-session button').trigger('click')
    await flushPromises()
    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(router.currentRoute.value.name).toBe('login')
    wrapper.unmount()
  })
})
