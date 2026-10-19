import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import SequenceItem from '@/features/learning/components/SequenceRecordItem.vue'
import ElementPlus, { ElInputNumber } from 'element-plus'
import RecordEditorForm from '@/features/learning/components/RecordEditorForm.vue'
const mocks = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('@/api/http', () => ({ get: mocks.get, toApiError: (error: Error) => error }))
const item = { id: 31, key: '31', title: 'LeetCode 31', url: 'https://leetcode.cn/problems/next-permutation/', completed: false }
const wrappers: ReturnType<typeof mount>[] = []
afterEach(() => { wrappers.splice(0).forEach(w => w.unmount()); vi.clearAllMocks() })
describe('接续补记题目', () => {
  it('同日补记新题时不自动回填已完成的题号',async()=>{
    mocks.get.mockResolvedValue({mode:'SELECT_BY_NUMBER',items:[item],current:null,dayItem:item,revision:'',actionable:true})
    const w=mount(SequenceItem,{props:{taskId:10,date:'2026-10-14',editing:false}});wrappers.push(w);await flushPromises()
    expect(w.emitted('number')?.at(-1)).toEqual([null])
    expect(w.emitted('ready')?.at(-1)).toEqual([false])
    expect(w.text()).toContain('同一天可记录多题');expect(w.text()).not.toContain('每天最多完成 1 题')
  })
  it('记录编辑表单提交更正题号，保留原日期、完成量、内容与复盘', async () => {
    mocks.get.mockResolvedValue({mode:'SELECT_BY_NUMBER',items:[item],current:null,dayItem:item,revision:'',actionable:true})
    const w=mount(RecordEditorForm,{props:{mode:'edit',tasks:[{id:10,title:'LeetCode',unitId:1,unitName:'题',sequential:true}]},global:{plugins:[ElementPlus]}});wrappers.push(w)
    w.vm.fillFrom({recordDate:'2026-10-14',taskId:10,unitId:1,sequenceItemId:31,amount:1,durationMinutes:20,content:'原内容',note:'原复盘'})
    await flushPromises()
    const number=w.findComponent(SequenceItem).findComponent(ElInputNumber)
    expect(number.props('modelValue')).toBe(31)
    number.vm.$emit('update:modelValue',49);await flushPromises();w.vm.submit()
    expect(w.emitted('submit')?.[0]).toEqual([expect.objectContaining({model:expect.objectContaining({recordDate:'2026-10-14',taskId:10,amount:1,durationMinutes:20,content:'原内容',note:'原复盘',problemNumber:49})})])
  })
  it('自行填写模式回填历史题号，可更正为49，清空阻止提交', async () => {
    mocks.get.mockResolvedValue({mode:'SELECT_BY_NUMBER',items:[item],current:null,dayItem:item,revision:'',actionable:true})
    const w=mount(SequenceItem,{props:{taskId:10,date:'2026-10-14',itemId:31,editing:true}});wrappers.push(w);await flushPromises()
    expect(w.emitted('number')?.at(-1)).toEqual([31])
    await w.setProps({problemNumber:31})
    const input=w.findComponent(ElInputNumber);expect(input.props('modelValue')).toBe(31)
    input.vm.$emit('update:modelValue',49);await flushPromises();expect(w.emitted('number')?.at(-1)).toEqual([49]);expect(w.emitted('ready')?.at(-1)).toEqual([true])
    input.vm.$emit('update:modelValue',undefined);await flushPromises();expect(w.emitted('ready')?.at(-1)).toEqual([false])
  })
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
