import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus, { ElButton } from 'element-plus'
import Dialog from '@/features/plan/components/PlanningContextExportDialog.vue'

const mocks = vi.hoisted(() => ({ read: vi.fn(), download: vi.fn() }))
const online = ref(true)
vi.mock('@/features/plan/api/contextExport', () => ({ getPlanningContextExport: mocks.read }))
vi.mock('@/features/plan/workspace', () => ({ downloadText: mocks.download }))
vi.mock('@/composables/useOnlineStatus', () => ({ useOnlineStatus: () => ({ online }) }))
const snapshot = { schemaVersion: 1, documentType: '408doing.planning-context', exportedAt: '2026-11-01T20:00:00+08:00', dataVersion: 'abcdef0123456789', studyDate: '2026-11-01', recentFrom: '2026-10-03', recentTo: '2026-11-01',
  facts: { progress: { courses: [{ id: 1 }] }, tasks: [{ id: 31 }], studyRecords: [{ id: 11 }], reviewRecords: [], openMemos: [] },
  feedback: { learningDays: 1, studyRecordCount: 1, completedReviews: 0, weaknesses: [] }, rules: [] }
const wrappers: ReturnType<typeof mount>[] = []
async function render() {
  const w = mount(Dialog, { props: { modelValue: true }, global: { plugins: [ElementPlus] } })
  wrappers.push(w); await flushPromises(); return w
}
function button(w: ReturnType<typeof mount>, text: string) { return w.findAllComponents(ElButton).find(b => b.text() === text)! }
beforeEach(() => { vi.clearAllMocks(); online.value = true; mocks.read.mockResolvedValue({ snapshot, markdown: '# 同一份快照' }) })
afterEach(() => { wrappers.splice(0).forEach(w => w.unmount()); document.body.innerHTML = '' })

describe('规划上下文导出', () => {
  it('两种格式共享一次读取，JSON文件只包含可移植快照', async () => {
    const w = await render()
    await button(w, '下载 Markdown').trigger('click'); await button(w, '下载 JSON').trigger('click')
    expect(mocks.read).toHaveBeenCalledTimes(1)
    expect(mocks.download).toHaveBeenNthCalledWith(1, '规划上下文-20261101200000-abcdef01.md', '# 同一份快照', 'text/markdown')
    expect(JSON.parse(mocks.download.mock.calls[1][1])).toEqual(snapshot)
    expect(w.text()).toContain('1 门课程 · 1 个任务 · 1 条学习记录')
  })
  it('请求失败时禁用下载，重试成功后才允许导出', async () => {
    mocks.read.mockRejectedValueOnce(new Error('读取失败'))
    const w = await render(); expect(w.text()).toContain('读取失败')
    expect(button(w, '下载 JSON').attributes('disabled')).toBeDefined()
    await button(w, '重新读取').trigger('click'); await flushPromises()
    expect(button(w, '下载 JSON').attributes('disabled')).toBeUndefined()
  })
  it('离线不读取或导出旧快照', async () => {
    online.value = false
    const w = await render(); expect(mocks.read).not.toHaveBeenCalled()
    expect(w.text()).toContain('请联网后导出')
    expect(button(w, '下载 Markdown').attributes('disabled')).toBeDefined()
  })
  it('关闭对话框后丢弃晚到的响应，重新打开重新读取', async () => {
    let resolve!: (data: unknown) => void
    mocks.read.mockImplementationOnce(() => new Promise(r => { resolve = r }))
    const w = await render(); await w.setProps({ modelValue: false })
    resolve({ snapshot, markdown: '# 过期快照' }); await flushPromises()
    await w.setProps({ modelValue: true }); await flushPromises()
    await button(w, '下载 Markdown').trigger('click')
    expect(mocks.read).toHaveBeenCalledTimes(2)
    expect(mocks.download.mock.calls[0][1]).toBe('# 同一份快照')
  })
  it('断网后清除已读取快照，恢复网络仍需重新读取', async () => {
    const w = await render(); online.value = false; await flushPromises()
    online.value = true; await flushPromises()
    expect(button(w, '下载 JSON').attributes('disabled')).toBeDefined()
    await button(w, '重新读取').trigger('click'); await flushPromises()
    expect(button(w, '下载 JSON').attributes('disabled')).toBeUndefined()
  })
})
