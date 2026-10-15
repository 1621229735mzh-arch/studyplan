import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import SequenceItem from '@/features/learning/components/SequenceRecordItem.vue'
const mocks = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('@/api/http', () => ({ get: mocks.get, toApiError: (error: Error) => error }))
const item = { id: 31, key: '31', title: 'LeetCode 31', url: 'https://leetcode.cn/problems/next-permutation/', completed: false }
const wrappers: ReturnType<typeof mount>[] = []
afterEach(() => { wrappers.splice(0).forEach(w => w.unmount()); vi.clearAllMocks() })
describe('接续补记题目', () => {
  it('加载期间阻止提交，成功后绑定后端当前题', async () => {
    let resolve!: (value: unknown) => void
    mocks.get.mockReturnValue(new Promise(r => { resolve = r }))
    const w = mount(SequenceItem, { props: { taskId: 10, date: '2026-10-14', editing: false } }); wrappers.push(w)
    expect(w.emitted('ready')?.[0]).toEqual([false])
    resolve({ items: [item], current: item, dayItem: item, revision: '', actionable: true }); await flushPromises()
    expect(w.emitted('item')?.at(-1)).toEqual([31])
    expect(w.emitted('ready')?.at(-1)).toEqual([true])
    expect(w.get('a').attributes('href')).toBe(item.url)
  })
  it('编辑旧记录保留题目身份，失败保留输入并可重试', async () => {
    mocks.get.mockRejectedValueOnce(new Error('读取失败')).mockResolvedValue({ items: [item], current: null, dayItem: null, revision: '', actionable: false })
    const w = mount(SequenceItem, { props: { taskId: 10, date: '2026-10-14', itemId: 31, editing: true } }); wrappers.push(w)
    await flushPromises(); expect(w.text()).toContain('读取失败'); expect(w.emitted('item')).toBeUndefined()
    await w.get('button').trigger('click'); await flushPromises()
    expect(w.get('a').text()).toContain('LeetCode 31')
    expect(w.emitted('item')).toBeUndefined()
    expect(w.emitted('ready')?.at(-1)).toEqual([true])
  })
})
