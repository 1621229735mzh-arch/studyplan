import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus, { ElButton, ElInputNumber } from 'element-plus'
import { HttpApiError } from '@/api/http'
import TodayView from '@/features/today/views/TodayView.vue'
import type { TodayResponse } from '@/features/today/types'
const mocks = vi.hoisted(() => ({ fetch: vi.fn(), learning: vi.fn(), review: vi.fn(), cache: vi.fn(), online: { value: true }, snapshot: null as TodayResponse | null }))
vi.mock('@/features/today/api', () => ({ fetchToday: mocks.fetch, saveLearningProgress: mocks.learning, saveReviewProgress: mocks.review }))
vi.mock('@/composables/useOnlineStatus', () => ({ useOnlineStatus: () => ({ online: mocks.online }) }))
vi.mock('@/stores/catalog', () => ({ useCatalogStore: () => ({ load: vi.fn(), subjectNameOf: () => '数学一' }) }))
vi.mock('@/stores/session', () => ({ useSessionStore: () => ({ username: 'test' }) }))
vi.mock('@/offline/useOfflineSnapshot', () => ({ useOfflineSnapshotStore: () => ({ save: mocks.cache, load: vi.fn(), today: mocks.snapshot, syncedAt: null }) }))
vi.mock('@/features/learning/components/RecordForm.vue', () => ({ default: { template: '<div />' } }))
function fixture(): TodayResponse {
 return {date: '2026-10-01', plannedTasks: [], records: [], reviewTasks: [], studyMinutes: 25, warnings: [],
  actualMinutes: 25, remainingMinutes: 90, missingEstimates: 0, missingDurations: 0,
  sections: [{name:'数学一', actualMinutes:25, remainingMinutes:70, missingEstimates:0, missingDurations:0, tasks:[{kind:'LEARNING', id:11, title:'高数第三讲', completionPercent:30, estimatedMinutes:100, remainingMinutes:70, revision:'rev1', version:null, actionable:true}]},
   {name:'408', actualMinutes:0, remainingMinutes:20, missingEstimates:0, missingDurations:0, tasks:[{kind:'REVIEW', id:21, title:'复习进程调度', completionPercent:0, estimatedMinutes:20, remainingMinutes:20, revision:null, version:3, actionable:true}]}] }
}
const wrappers: ReturnType<typeof mount>[] = []
async function page() {const wrapper=mount(TodayView,{global:{plugins:[ElementPlus], stubs:{teleport:true, ElDialog:{props:['modelValue'],template:'<div v-if="modelValue"><slot /><slot name="footer" /></div>'}}},attachTo:document.body});wrappers.push(wrapper);await flushPromises();return wrapper}
function button(wrapper: ReturnType<typeof mount>, text: string, index=0) {return wrapper.findAllComponents(ElButton).filter(b=>b.text()===text)[index]}
beforeEach(()=>{vi.useFakeTimers({toFake:['Date']});vi.setSystemTime(new Date('2026-10-01T12:00:00+08:00'));mocks.online.value=true;mocks.snapshot=null;vi.clearAllMocks();mocks.fetch.mockResolvedValue(fixture());mocks.learning.mockResolvedValue({id:1});mocks.review.mockResolvedValue({id:2});mocks.cache.mockResolvedValue(undefined)})
afterEach(()=>{wrappers.splice(0).forEach(w=>w.unmount());document.body.innerHTML='';vi.useRealTimers()})
describe('今日清单操作',()=>{
 it('展示服务端顶端总计与分科清单',async()=>{const w=await page();expect(w.text()).toContain('1 小时 30 分钟');expect(w.text()).toContain('数学一');expect(w.text()).toContain('复习进程调度');expect(w.text()).not.toContain('今日概览');expect(w.text()).not.toContain('每日学习预算')})
 it('剩余提交累计比例和本次用时，失败保留输入及重试令牌',async()=>{
  const w=await page();await button(w,'剩余').trigger('click');await flushPromises();
  const fields=w.findAllComponents(ElInputNumber);fields[0].vm.$emit('update:modelValue',50);fields[1].vm.$emit('update:modelValue',15);await flushPromises();
  mocks.learning.mockRejectedValueOnce(new HttpApiError({code:'FAILED',message:'保存失败',status:500}));await button(w,'保存').trigger('click');await flushPromises();
  expect(w.text()).toContain('保存失败');expect(fields[0].props('modelValue')).toBe(50);expect(fields[1].props('modelValue')).toBe(15);
  const first=mocks.learning.mock.calls[0];expect(first.slice(0,2)).toEqual([11,'rev1']);expect(first[2]).toMatchObject({date:'2026-10-01',completionPercent:50,durationMinutes:15});
  await button(w,'保存').trigger('click');await flushPromises();expect(mocks.learning.mock.calls[1][2].clientToken).toEqual(first[2].clientToken);expect(mocks.fetch).toHaveBeenCalledTimes(2);
 })
 it('完成按钮仍需填写本次用时才可保存',async()=>{const w=await page();await button(w,'完成').trigger('click');await flushPromises();await button(w,'保存').trigger('click');await flushPromises();expect(mocks.learning).not.toHaveBeenCalled();expect(w.text()).toContain('请填写本次学习用时');w.findComponent(ElInputNumber).vm.$emit('update:modelValue',40);await flushPromises();await button(w,'保存').trigger('click');await flushPromises();expect(mocks.learning.mock.calls[0][2]).toMatchObject({completionPercent:100,durationMinutes:40})})
 it('复习完成必须反馈掌握情况',async()=>{const w=await page();await button(w,'完成',1).trigger('click');await flushPromises();w.findComponent(ElInputNumber).vm.$emit('update:modelValue',20);await flushPromises();await button(w,'保存').trigger('click');await flushPromises();expect(mocks.review).not.toHaveBeenCalled();expect(w.text()).toContain('请选择本次复习的掌握情况')})
 it('离线快照可查看但不能提交任务',async()=>{mocks.online.value=false;mocks.snapshot=fixture();const w=await page();expect(w.text()).toContain('只读快照');expect(button(w,'完成').attributes('disabled')).toBeDefined();expect(button(w,'剩余').attributes('disabled')).toBeDefined();expect(mocks.fetch).not.toHaveBeenCalled();expect(mocks.learning).not.toHaveBeenCalled()})
 it('部分复习只提交进度和本次用时，不要求掌握反馈',async()=>{const w=await page();await button(w,'剩余',1).trigger('click');await flushPromises();const fields=w.findAllComponents(ElInputNumber);fields[0].vm.$emit('update:modelValue',30);fields[1].vm.$emit('update:modelValue',12);await flushPromises();await button(w,'保存').trigger('click');await flushPromises();expect(mocks.review.mock.calls[0].slice(0,3)).toEqual([21,3,null]);expect(mocks.review.mock.calls[0][3]).toMatchObject({completionPercent:30,durationMinutes:12})})
 it('读到旧快照时不臆造新总计',async()=>{const old=fixture();delete old.sections;delete old.actualMinutes;mocks.fetch.mockResolvedValue(old);const w=await page();expect(w.text()).toContain('旧版快照');expect(button(w,'完成')).toBeUndefined()})
})
