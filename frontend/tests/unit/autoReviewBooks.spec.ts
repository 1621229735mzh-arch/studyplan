import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus, { ElButton, ElMessageBox } from 'element-plus'
import AutoReviewBooks from '@/features/review/components/AutoReviewBooks.vue'

const api = vi.hoisted(() => ({ books: vi.fn(), segments: vi.fn(), policy: vi.fn() }))
vi.mock('@/features/review/api/automatic', () => ({ getAutoReviewBooks: api.books, getAutoReviewSegments: api.segments, setAutoReviewPolicy: api.policy }))
const book = { streamId: 10, subjectId: 1, title: '基础30讲', enabled: false, version: 4, segmentCount: 126, reviewCount: 1, overdueCount: 1, suspendedCount: 0 }
const segment = { reviewItemId: 21, streamId: 10, title: '函数与反函数', completedOn: '2026-10-28', sourceValid: true, paused: false, enabled: true, status: 'SCHEDULED', scheduledDate: '2026-10-29', nextReviewDate: '2026-10-29', repetitions: 0, version: 2 }
const wrappers: ReturnType<typeof mount>[] = []
async function render(props = {}) {
  const w = mount(AutoReviewBooks, { props: { online: true, workspace: {}, ...props }, global: { plugins: [ElementPlus] } })
  wrappers.push(w); await flushPromises(); return w
}
function button(w: ReturnType<typeof mount>, text: string) { return w.findAllComponents(ElButton).find(b => b.text() === text)! }
beforeEach(() => {
  vi.clearAllMocks(); api.books.mockResolvedValue([book]); api.segments.mockResolvedValue([segment]); api.policy.mockResolvedValue({ ...book, enabled: true, version: 5 })
  // Element Plus convenience methods have intersecting overloads; this fixture returns only the action.
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never)
  vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date('2026-11-01T12:00:00+08:00'))
})
afterEach(() => { wrappers.splice(0).forEach(w => w.unmount()); vi.restoreAllMocks(); vi.useRealTimers() })

describe('教材自动复习', () => {
  it('按教材收纳小段，显示真实完成日、轮次与逾期，不因查看创建安排', async () => {
    const w = await render()
    expect(w.findAll('details')).toHaveLength(1); expect(w.text()).toContain('基础30讲')
    expect(w.text()).toContain('函数与反函数'); expect(w.text()).toContain('学完于 2026-10-28')
    expect(w.text()).toContain('2026-10-29 · 第1次复习'); expect(w.text()).toContain('已逾期')
    expect(api.policy).not.toHaveBeenCalled()
  })
  it('确认一次后携带教材身份和版本启用，取消时不写入', async () => {
    const w = await render(); await button(w, '启用自动复习').trigger('click'); await flushPromises()
    expect(api.policy).toHaveBeenCalledWith(book, true); expect(w.emitted('changed')).toHaveLength(1)
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')
    await button(w, '启用自动复习').trigger('click'); await flushPromises()
    expect(api.policy).toHaveBeenCalledTimes(1)
  })
  it('离线不读完整数据、不开放启用', async () => {
    const w = await render({ online: false })
    expect(api.books).not.toHaveBeenCalled(); expect(api.segments).not.toHaveBeenCalled()
    expect(w.text()).toContain('联网后查看和调整自动复习'); expect(api.policy).not.toHaveBeenCalled()
  })
  it('错误不伪装成空教材，重试后恢复', async () => {
    api.books.mockRejectedValueOnce(new Error('自动复习读取失败'))
    const w = await render(); expect(w.text()).toContain('自动复习读取失败'); expect(w.text()).not.toContain('还没有可接入')
    await button(w, '重新加载自动复习').trigger('click'); await flushPromises()
    expect(w.text()).toContain('基础30讲')
  })
  it('版本冲突保留现有规则，不发出成功事件', async () => {
    api.policy.mockRejectedValueOnce(new Error('规则已变化，请刷新'))
    const w = await render(); await button(w, '启用自动复习').trigger('click'); await flushPromises()
    expect(w.text()).toContain('规则已变化，请刷新'); expect(w.emitted('changed')).toBeUndefined()
    expect(button(w, '启用自动复习')).toBeDefined()
  })
  it('教材暂停与小段来源失效均展示原因，失效段不能恢复安排', async () => {
    api.books.mockResolvedValue([{ ...book, enabled: true }]); api.segments.mockResolvedValue([{ ...segment, sourceValid: false, scheduledDate: null }])
    const w = await render(); expect(w.text()).toContain('来源未学完 · 已暂停')
    expect(w.findAllComponents(ElButton).map(b => b.text())).not.toContain('恢复安排')
    await button(w, '暂停自动复习').trigger('click'); await flushPromises()
    expect(api.policy).toHaveBeenCalledWith({ ...book, enabled: true }, false)
  })
  it('按科目筛选且改期只打开原复习项的日期编辑', async () => {
    api.books.mockResolvedValue([{ ...book, enabled: true }, { ...book, streamId: 11, subjectId: 2, title: '其他教材' }])
    const w = await render({ subjectId: 1 }); expect(w.text()).not.toContain('其他教材')
    await button(w, '调整日期').trigger('click')
    expect(w.emitted('schedule')).toEqual([['2026-11-01', 21]]); expect(api.policy).not.toHaveBeenCalled()
  })
})
