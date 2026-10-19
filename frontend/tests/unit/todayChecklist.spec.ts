import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus, { ElButton, ElDatePicker, ElInputNumber } from 'element-plus'
import { HttpApiError } from '@/api/http'
import TodayView from '@/features/today/views/TodayView.vue'
import type { TodayResponse } from '@/features/today/types'
const mocks = vi.hoisted(() => ({ fetch: vi.fn(), learning: vi.fn(), review: vi.fn(), problems: vi.fn(), confirmation: vi.fn(), cache: vi.fn(), online: { value: true }, snapshot: null as TodayResponse | null }))
vi.mock('@/features/today/api', () => ({ fetchToday: mocks.fetch, saveLearningProgress: mocks.learning, saveCompletedProblemNumbers: mocks.problems, saveReviewProgress: mocks.review, saveDayConfirmation: mocks.confirmation }))
vi.mock('@/composables/useOnlineStatus', () => ({ useOnlineStatus: () => ({ online: mocks.online }) }))
vi.mock('@/stores/catalog', () => ({ useCatalogStore: () => ({ load: vi.fn(), subjectNameOf: () => '数学一' }) }))
vi.mock('@/stores/session', () => ({ useSessionStore: () => ({ username: 'test' }) }))
vi.mock('@/offline/useOfflineSnapshot', () => ({ useOfflineSnapshotStore: () => ({ save: mocks.cache, load: vi.fn(), today: mocks.snapshot, syncedAt: null }) }))
vi.mock('@/features/learning/components/RecordForm.vue', () => ({ default: { template: '<div />' } }))
function fixture(): TodayResponse {
 return {date: '2026-10-01', plannedTasks: [], records: [], reviewTasks: [], studyMinutes: 25, warnings: [],
  actualMinutes: 25, remainingMinutes: 90, missingEstimates: 0, missingDurations: 0,
  taskCounts: {completed: 0, inProgress: 1, remaining: 1},
  sections: [{name:'数学一', actualMinutes:25, remainingMinutes:70, missingEstimates:0, missingDurations:0, tasks:[{kind:'LEARNING', id:11, title:'高数第三讲', completionPercent:30, estimatedMinutes:100, remainingMinutes:70, revision:'rev1', version:null, actionable:true}]},
   {name:'408', actualMinutes:0, remainingMinutes:20, missingEstimates:0, missingDurations:0, tasks:[{kind:'REVIEW', id:21, title:'复习进程调度', completionPercent:0, estimatedMinutes:20, remainingMinutes:20, revision:null, version:3, actionable:true}]}] }
}
const wrappers: ReturnType<typeof mount>[] = []
async function page() {const wrapper=mount(TodayView,{global:{plugins:[ElementPlus], stubs:{teleport:true, ElDialog:{props:['modelValue'],template:'<div v-if="modelValue"><slot /><slot name="footer" /></div>'}}},attachTo:document.body});wrappers.push(wrapper);await flushPromises();return wrapper}
function button(wrapper: ReturnType<typeof mount>, text: string, index=0) {return wrapper.findAllComponents(ElButton).filter(b=>b.text()===text)[index]}
beforeEach(()=>{vi.useFakeTimers({toFake:['Date']});vi.setSystemTime(new Date('2026-10-01T12:00:00+08:00'));mocks.online.value=true;mocks.snapshot=null;vi.clearAllMocks();mocks.fetch.mockResolvedValue(fixture());mocks.learning.mockResolvedValue({id:1});mocks.problems.mockResolvedValue([]);mocks.review.mockResolvedValue({id:2});mocks.cache.mockResolvedValue(undefined)})
afterEach(()=>{wrappers.splice(0).forEach(w=>w.unmount());document.body.innerHTML='';vi.useRealTimers()})
describe('今日清单操作',()=>{
 it('页面保持打开时九点自动刷新，且不会在截止瞬间用旧清单提交',async()=>{
  vi.useRealTimers()
  vi.useFakeTimers({toFake:['Date','setInterval','clearInterval']})
  vi.setSystemTime(new Date('2026-10-02T08:59:59+08:00'))
  const w=await page()
  await vi.advanceTimersByTimeAsync(1000);await flushPromises()
  expect(mocks.fetch).toHaveBeenLastCalledWith('2026-10-02')
  vi.setSystemTime(new Date('2026-10-03T09:00:00+08:00'))
  await button(w,'完成').trigger('click');await flushPromises()
  expect(mocks.fetch).toHaveBeenLastCalledWith('2026-10-03')
  expect(mocks.learning).not.toHaveBeenCalled()
 })
 it('凌晨六点提交仍归前一天，九点回到页面后自动切换',async()=>{
  vi.setSystemTime(new Date('2026-10-02T06:00:00+08:00'))
  const w=await page()
  expect(mocks.fetch).toHaveBeenLastCalledWith('2026-10-01')
  await button(w,'完成').trigger('click');await flushPromises()
  await button(w,'保存').trigger('click');await flushPromises()
  expect(mocks.learning.mock.calls[0][2].date).toBe('2026-10-01')
  vi.setSystemTime(new Date('2026-10-02T09:00:00+08:00'))
  window.dispatchEvent(new Event('focus'));await flushPromises()
  expect(mocks.fetch).toHaveBeenLastCalledWith('2026-10-02')
 })
 it('九点切换保留正在填写的原日期，手动查看历史日期也不跳走',async()=>{
  vi.setSystemTime(new Date('2026-10-02T08:59:59+08:00'))
  const w=await page();await button(w,'完成').trigger('click');await flushPromises()
  vi.setSystemTime(new Date('2026-10-02T09:00:00+08:00'))
  window.dispatchEvent(new Event('focus'));await flushPromises()
  expect(mocks.fetch).toHaveBeenLastCalledWith('2026-10-01')
  await button(w,'保存').trigger('click');await flushPromises()
  expect(mocks.learning.mock.calls[0][2].date).toBe('2026-10-01')
  w.findComponent(ElDatePicker).vm.$emit('update:modelValue','2026-09-29');await flushPromises()
  vi.setSystemTime(new Date('2026-10-03T09:00:00+08:00'))
  window.dispatchEvent(new Event('focus'));await flushPromises()
  expect(mocks.fetch).toHaveBeenLastCalledWith('2026-09-29')
 })
 it('补记快捷入口展开原表单，不直接生成学习记录',async()=>{
  const w=await page();await button(w,'补记学习').trigger('click');await flushPromises()
  expect((w.get('.extra-record').element as HTMLDetailsElement).open).toBe(true)
  expect(mocks.learning).not.toHaveBeenCalled();expect(mocks.review).not.toHaveBeenCalled()
 })
 it('进度条沿用服务端比例，实际数量任务不生成百分比进度条',async()=>{
  const w=await page();expect(w.get('[role="progressbar"]').attributes('aria-valuenow')).toBe('30')
  const data=fixture();Object.assign(data.sections![0].tasks[0],{completionPercent:100,recordedAmount:23,unitName:'词',activity:{mode:'COUNT_STUDY',description:'实际新词数量',flexibleMinutes:null}})
  mocks.fetch.mockResolvedValue(data);await button(w,'刷新').trigger('click');await flushPromises()
  expect(w.find('[role="progressbar"]').exists()).toBe(false);expect(w.text()).toContain('23 词')
 })
 it('顶部依次展示服务端完成、进行中和未开始项数，不显示时间',async()=>{
  const data=fixture();data.taskCounts={completed:2,inProgress:3,remaining:4};data.missingEstimates=2;data.missingDurations=1;mocks.fetch.mockResolvedValue(data)
  const w=await page();const summary=w.get('[aria-label="今日任务概览"]')
  expect(summary.findAll('.summary-metric').map(m=>m.text())).toEqual(['已完成2 项','进行中3 项','还剩4 项'])
  expect(summary.text()).not.toMatch(/分钟|小时|至少|未填/)
  expect(w.text()).toContain('数学一');expect(w.text()).toContain('复习进程调度');expect(w.text()).not.toContain('每日学习预算')
 })
 it('无任务和全部完成时正确显示零项',async()=>{
  const data=fixture();data.sections=[];data.taskCounts={completed:0,inProgress:0,remaining:0};mocks.fetch.mockResolvedValue(data)
  const w=await page();expect(w.get('.today-summary').findAll('strong').map(m=>m.text())).toEqual(['0 项','0 项','0 项'])
  mocks.fetch.mockResolvedValue({...fixture(),taskCounts:{completed:2,inProgress:0,remaining:0}})
  await button(w,'刷新').trigger('click');await flushPromises()
  expect(w.get('.today-summary').findAll('strong').map(m=>m.text())).toEqual(['2 项','0 项','0 项'])
 })
 it('旧离线快照缺少项数时提示联网刷新，继续只读展示原清单',async()=>{
  const data=fixture();delete data.taskCounts;mocks.online.value=false;mocks.snapshot=data
  const w=await page();expect(w.find('.today-summary').exists()).toBe(false)
  expect(w.text()).toContain('当前数据尚无任务项数统计，请联网刷新今日清单。')
  expect(button(w,'完成').attributes('disabled')).toBeDefined()
 })
 it('剩余无需用时提交累计比例，失败保留输入及重试令牌',async()=>{
  const w=await page();await button(w,'剩余').trigger('click');await flushPromises();
  const fields=w.findAllComponents(ElInputNumber);fields[0].vm.$emit('update:modelValue',50);await flushPromises();
  mocks.learning.mockRejectedValueOnce(new HttpApiError({code:'FAILED',message:'保存失败',status:500}));await button(w,'保存').trigger('click');await flushPromises();
  expect(w.text()).toContain('保存失败');expect(fields[0].props('modelValue')).toBe(50);
  const first=mocks.learning.mock.calls[0];expect(first.slice(0,2)).toEqual([11,'rev1']);expect(first[2]).toMatchObject({date:'2026-10-01',completionPercent:50,durationMinutes:null});
  await button(w,'保存').trigger('click');await flushPromises();expect(mocks.learning.mock.calls[1][2].clientToken).toEqual(first[2].clientToken);expect(mocks.fetch).toHaveBeenCalledTimes(2);
 })
 it('完成按钮无需用时即可保存',async()=>{const w=await page();await button(w,'完成').trigger('click');await flushPromises();expect(w.findAllComponents(ElInputNumber)).toHaveLength(0);await button(w,'保存').trigger('click');await flushPromises();expect(mocks.learning.mock.calls[0][2]).toMatchObject({completionPercent:100,durationMinutes:null})})
 it('复习完成必须反馈掌握情况',async()=>{const w=await page();await button(w,'完成',1).trigger('click');await flushPromises();await flushPromises();await button(w,'保存').trigger('click');await flushPromises();expect(mocks.review).not.toHaveBeenCalled();expect(w.text()).toContain('请选择本次复习的掌握情况')})
 it('离线快照可查看任务项数但不能提交任务',async()=>{mocks.online.value=false;mocks.snapshot=fixture();const w=await page();expect(w.text()).toContain('只读快照');expect(w.get('.today-summary').findAll('strong').map(m=>m.text())).toEqual(['0 项','1 项','1 项']);expect(button(w,'完成').attributes('disabled')).toBeDefined();expect(button(w,'剩余').attributes('disabled')).toBeDefined();expect(mocks.fetch).not.toHaveBeenCalled();expect(mocks.learning).not.toHaveBeenCalled()})
 it('部分复习无需用时提交进度，不要求掌握反馈',async()=>{const w=await page();await button(w,'剩余',1).trigger('click');await flushPromises();const fields=w.findAllComponents(ElInputNumber);fields[0].vm.$emit('update:modelValue',30);await flushPromises();await button(w,'保存').trigger('click');await flushPromises();expect(mocks.review.mock.calls[0].slice(0,3)).toEqual([21,3,null]);expect(mocks.review.mock.calls[0][3]).toMatchObject({completionPercent:30,durationMinutes:null})})
 it('读到旧快照时不臆造新总计',async()=>{const old=fixture();delete old.sections;delete old.actualMinutes;mocks.fetch.mockResolvedValue(old);const w=await page();expect(w.text()).toContain('旧版快照');expect(button(w,'完成')).toBeUndefined()})
})


describe('有序题目执行', () => {
 it('批量完成题号支持分隔符和去重，失败保留输入与令牌', async () => {
  const data=fixture()
  Object.assign(data.sections![0].tasks[0],{title:'LeetCode',completionPercent:0,sequence:{mode:'SELECT_BY_NUMBER',items:[],current:null,dayItem:null,revision:'',actionable:true}})
  mocks.fetch.mockResolvedValue(data)
  const w=await page()
  expect(w.text()).toContain('每天不限题数');expect(w.text()).not.toContain('题目清单已全部完成')
  await button(w,'填写完成题号').trigger('click');await flushPromises()
  await button(w,'保存').trigger('click');await flushPromises();expect(mocks.problems).not.toHaveBeenCalled()
  const input=w.get('textarea[aria-label="LeetCode完成题号"]')
  await input.setValue('31、32，49\n128 283;31')
  mocks.problems.mockRejectedValueOnce(new Error('保存失败'))
  await button(w,'保存').trigger('click');await flushPromises()
  expect((input.element as HTMLTextAreaElement).value).toBe('31、32，49\n128 283;31')
  const first=mocks.problems.mock.calls[0]
  expect(first.slice(0,4)).toEqual([11,'2026-10-01',[31,32,49,128,283],'rev1'])
  await button(w,'保存').trigger('click');await flushPromises();expect(mocks.problems.mock.calls[1][4]).toBe(first[4])
  expect(mocks.learning).not.toHaveBeenCalled()
 })
 it('完成后仍直接显示追加入口，打开时不回填已完成题号，九点后保存原日期',async()=>{
  const data=fixture()
  Object.assign(data.sections![0].tasks[0],{title:'LeetCode',completionPercent:100,recordedAmount:5,sequence:{mode:'SELECT_BY_NUMBER',items:[],current:null,dayItem:{id:31,key:'31',title:'LeetCode 31',url:'https://leetcode.cn/',completed:true},revision:'',actionable:true}})
  mocks.fetch.mockResolvedValue(data)
  vi.setSystemTime(new Date('2026-10-02T08:59:59+08:00'))
  const w=await page()
  expect(w.text()).toContain('今日已完成 5 题');expect(w.find('.completed').exists()).toBe(false)
  await button(w,'继续填写').trigger('click');await flushPromises()
  const input=w.get('textarea[aria-label="LeetCode完成题号"]')
  expect((input.element as HTMLTextAreaElement).value).toBe('')
  await input.setValue('3000 3001')
  vi.setSystemTime(new Date('2026-10-02T09:00:00+08:00'));window.dispatchEvent(new Event('focus'))
  await button(w,'保存').trigger('click');await flushPromises()
  expect(mocks.problems.mock.calls[0].slice(0,3)).toEqual([11,'2026-10-01',[3000,3001]])
 })
 it('无效题号不提交，重复题业务错误保留，离线禁写',async()=>{
  const data=fixture()
  Object.assign(data.sections![0].tasks[0],{completionPercent:0,sequence:{mode:'SELECT_BY_NUMBER',items:[],current:null,dayItem:null,revision:'',actionable:true}})
  mocks.fetch.mockResolvedValue(data)
  const w=await page();await button(w,'填写完成题号').trigger('click');await flushPromises()
  const input=w.get('textarea[aria-label="LeetCode完成题号"]')
  for (const invalid of ['31 abc','-1','1.5','0','2147483648','31/32']) {
   await input.setValue(invalid);await button(w,'保存').trigger('click');await flushPromises()
  }
  expect(mocks.problems).not.toHaveBeenCalled()
  await input.setValue('31')
  mocks.problems.mockRejectedValueOnce(new HttpApiError({message:'该题已有完成记录',status:409,code:'SEQUENCE_ALREADY_COMPLETED'}))
  await button(w,'保存').trigger('click');await flushPromises()
  expect(w.text()).toContain('该题已有完成记录')
  mocks.online.value=false
  mocks.snapshot=data
  const offline=await page()
  expect(button(offline,'填写完成题号').attributes('disabled')).toBeDefined()
  await button(offline,'填写完成题号').trigger('click');await flushPromises()
  expect(offline.find('textarea').exists()).toBe(false)
  expect(mocks.problems).toHaveBeenCalledTimes(1)
 })
 it('尝试无需用时，提交零完成比例并保留当前题链接', async () => {
  const data = fixture()
  const item = { id: 31, key: '31', title: 'LeetCode 31 · Next Permutation', url: 'https://leetcode.cn/problems/next-permutation/', completed: false }
  Object.assign(data.sections![0].tasks[0], { completionPercent: 0, sequence: { items: [], current: item, dayItem: item, revision: '', actionable: true } })
  mocks.fetch.mockResolvedValue(data)
  const w = await page()
  expect(w.get('a[href="https://leetcode.cn/problems/next-permutation/"]').text()).toContain('LeetCode 31')
  await button(w, '记尝试').trigger('click'); await flushPromises()
  expect(w.text()).toContain('记录本次尝试，题目继续保留')
  expect(w.findAllComponents(ElInputNumber)).toHaveLength(0)
   await flushPromises()
  await button(w, '保存').trigger('click'); await flushPromises()
  expect(mocks.learning.mock.calls[0][2]).toMatchObject({ completionPercent: 0, durationMinutes:null })
 })
})

describe('时间型学习与复盘', () => {
 it('结束本次无需用时据实提交题量，不以预算自动生成题量', async () => {
  const data=fixture(); Object.assign(data.sections![0].tasks[0],{completionPercent:0,unitName:'题',activity:{mode:'TIMED_STUDY',description:'基础练习',flexibleMinutes:30}})
  mocks.fetch.mockResolvedValue(data); const w=await page(); await button(w,'结束本次').trigger('click'); await flushPromises()
  expect(w.text()).toContain('不表示整讲或整章已经学完'); expect(w.text()).not.toContain('累计已完成 (%)')
  const fields=w.findAllComponents(ElInputNumber); expect(fields).toHaveLength(1)
  fields[0].vm.$emit('update:modelValue',4);  await flushPromises()
  await button(w,'保存').trigger('click'); await flushPromises()
  expect(mocks.learning.mock.calls[0][2]).toMatchObject({completionPercent:100,durationMinutes:null,actualAmount:4})
 })
 it('复盘无需用时，继续记录不增加首次完成量', async () => {
  const data=fixture(); Object.assign(data.sections![0].tasks[0],{completionPercent:0,unitName:'题',activity:{mode:'RECAP',description:'重做代表题',flexibleMinutes:0}})
  mocks.fetch.mockResolvedValue(data); const w=await page(); await button(w,'记一笔').trigger('click'); await flushPromises()
  expect(w.text()).toContain('复做与订正不增加首次完成量'); expect(w.findAllComponents(ElInputNumber)).toHaveLength(0)
   await flushPromises(); await button(w,'保存').trigger('click'); await flushPromises()
  expect(mocks.learning.mock.calls[0][2]).toMatchObject({completionPercent:0,durationMinutes:null,actualAmount:null})
 })
})

describe('每日完成确认',()=>{
 function checkFixture(completed=false) {
  const data=fixture();Object.assign(data.sections![0].tasks[0],{title:'每日单词复习',completionPercent:completed?100:0,estimatedMinutes:null,remainingMinutes:completed?0:null,activity:{mode:'CHECK_ONLY',description:'复习已学单词',flexibleMinutes:0}});return data
 }
 it('确认无需输入数量或时长，失败重试保留令牌',async()=>{
  mocks.fetch.mockResolvedValue(checkFixture());mocks.confirmation.mockRejectedValueOnce(new Error('暂时失败')).mockResolvedValue({});
  const w=await page();expect(w.text()).toContain('无需数量或时长');expect(w.text()).not.toContain('基本  分钟');
  await button(w,'确认完成').trigger('click');await flushPromises();
  expect(w.findAllComponents(ElInputNumber)).toHaveLength(0);expect(mocks.learning).not.toHaveBeenCalled();
  const call=mocks.confirmation.mock.calls[0];expect(call.slice(0,4)).toEqual([11,'2026-10-01',true,'rev1']);
  await button(w,'确认完成').trigger('click');await flushPromises();expect(mocks.confirmation.mock.calls[1][4]).toBe(call[4]);
 })
 it('已完成可取消，仍不打开记录用时弹窗',async()=>{
  mocks.fetch.mockResolvedValue(checkFixture(true));mocks.confirmation.mockResolvedValue({});const w=await page();
  await button(w,'取消完成').trigger('click');await flushPromises();expect(mocks.confirmation.mock.calls[0].slice(0,4)).toEqual([11,'2026-10-01',false,'rev1']);
  expect(w.findAllComponents(ElInputNumber)).toHaveLength(0);expect(mocks.review).not.toHaveBeenCalled();
 })
 it('离线完成确认只读',async()=>{
  mocks.online.value=false;mocks.snapshot=checkFixture();const w=await page();expect(button(w,'确认完成').attributes('disabled')).toBeDefined();expect(mocks.confirmation).not.toHaveBeenCalled();
 })
})

describe('按书接续学习',()=>{
 function continuedFixture() {
  const data=fixture()
  const base=data.sections![0].tasks[0]
  const task=(id:number,title:string,streamId:number,book:string,carried:boolean,progress:number) => ({...base,id,title,completionPercent:progress,
   activity:{mode:'CONTINUED_STUDY' as const,description:title==='函数定义'?'函数定义、定义域与对应关系':'完成第1章第1—3题',flexibleMinutes:0},
   continuation:{streamId,title:book,position:id,referenceDate:'2026-09-30',carried},revision:`rev-${id}`})
  data.sections![0].tasks=[task(11,'函数定义',1,'《基础30讲》',true,40),task(12,'函数性质',1,'《基础30讲》',false,0),task(13,'第1章①',2,'《1000题》基础篇',true,0)]
  return data
 }
 it('按书收纳旧内容和新内容，省去执行说明和教材位置要求',async()=>{
  mocks.fetch.mockResolvedValue(continuedFixture());const w=await page()
  expect(w.findAll('.book-task')).toHaveLength(2)
  expect(w.findAll('.book-task')[0].text()).toContain('函数定义')
  expect(w.findAll('.book-task')[0].text()).toContain('函数性质')
  expect(w.text()).toContain('剩余内容');expect(w.text()).toContain('新内容')
  expect(w.text()).toContain('已完成 40%');expect(w.text()).not.toContain('完成量据实记录')
  expect(button(w,'结束本次')).toBeUndefined()
 })
 it('数学显示服务端预计剩余时间，空估算和已完成不显示，仍不要求填写用时',async()=>{
  const data=continuedFixture()
  data.sections![0].tasks[0].remainingMinutes=90
  data.sections![0].tasks[1].remainingMinutes=null
  data.sections![0].tasks[2].completionPercent=100
  data.sections![0].tasks[2].remainingMinutes=0
  mocks.fetch.mockResolvedValue(data);const w=await page()
  expect(w.findAll('.task-estimate').map(item=>item.text())).toEqual(['预计剩余 1 小时 30 分钟'])
  expect(w.text()).not.toContain('预计用时待补充')
  await button(w,'剩余',0).trigger('click');await flushPromises()
  expect(w.findAllComponents(ElInputNumber)).toHaveLength(1)
  expect(w.text()).not.toContain('本次用时')
  w.findComponent(ElInputNumber).vm.$emit('update:modelValue',70);await flushPromises()
  await button(w,'保存').trigger('click');await flushPromises()
  expect(mocks.learning.mock.calls[0][2]).toMatchObject({completionPercent:70,durationMinutes:null})
 })
 it('旧内容和新内容分别提交百分比，接续任务不要求填写新增题量或位置',async()=>{
  mocks.fetch.mockResolvedValue(continuedFixture());const w=await page()
  await button(w,'剩余',0).trigger('click');await flushPromises()
  expect(w.text()).not.toContain('进度位置与卡点');expect(w.text()).not.toContain('不表示整讲')
  const fields=w.findAllComponents(ElInputNumber);expect(fields).toHaveLength(1)
  fields[0].vm.$emit('update:modelValue',70);await flushPromises()
  await button(w,'保存').trigger('click');await flushPromises()
  expect(mocks.learning.mock.calls[0].slice(0,2)).toEqual([11,'rev-11'])
  expect(mocks.learning.mock.calls[0][2]).toMatchObject({completionPercent:70,durationMinutes:null})
  expect(mocks.learning.mock.calls[0][2]).not.toHaveProperty('actualAmount')
  await button(w,'剩余',1).trigger('click');await flushPromises()
  expect(w.findAllComponents(ElInputNumber)[0].props('modelValue')).toBe(0)
 })
 it('离线快照的两本书及各段百分比均只读',async()=>{
  mocks.online.value=false;mocks.snapshot=continuedFixture();const w=await page()
  expect(w.findAll('.book-task')).toHaveLength(2)
  w.findAllComponents(ElButton).filter(b=>['完成','剩余'].includes(b.text())).forEach(b=>expect(b.attributes('disabled')).toBeDefined())
  expect(mocks.learning).not.toHaveBeenCalled()
 })
})

describe('自主填写每日实际词数',()=>{
 function countedFixture(mode: 'COUNT_STUDY' | 'COUNT_REVIEW', completed=false) {
  const data=fixture();Object.assign(data.sections![0].tasks[0],{title:mode==='COUNT_REVIEW'?'每日单词复习':'每日背诵新词',completionPercent:completed?100:0,estimatedMinutes:null,remainingMinutes:null,unitName:'词',recordedAmount:completed?37:0,activity:{mode,description:'学习后填写实际词数',flexibleMinutes:0}});return data
 }
 it.each(['COUNT_STUDY','COUNT_REVIEW'] as const)('%s 不预填目标，只提交用户填写的实际数量',async(mode)=>{
  mocks.fetch.mockResolvedValue(countedFixture(mode));const w=await page();await button(w,'填写数量').trigger('click');await flushPromises()
  expect(w.text()).not.toContain('累计已完成 (%)');expect(w.text()).not.toContain('整讲或整章')
  const fields=w.findAllComponents(ElInputNumber);expect(fields).toHaveLength(1);expect(fields[0].props('modelValue')).toBeUndefined()
  await button(w,'保存').trigger('click');await flushPromises();expect(mocks.learning).not.toHaveBeenCalled();expect(w.text()).toContain('请填写本次实际词数')
  fields[0].vm.$emit('update:modelValue',73);await flushPromises()
  mocks.learning.mockRejectedValueOnce(new Error('保存失败'));await button(w,'保存').trigger('click');await flushPromises()
  const first=mocks.learning.mock.calls[0][2];expect(first).toMatchObject({actualAmount:73,durationMinutes:null,completionPercent:100})
  expect(fields[0].props('modelValue')).toBe(73)
  await button(w,'保存').trigger('click');await flushPromises();expect(mocks.learning.mock.calls[1][2].clientToken).toBe(first.clientToken)
  expect(mocks.confirmation).not.toHaveBeenCalled();expect(mocks.review).not.toHaveBeenCalled()
 })
 it('已完成后可分次补记，展示服务端当日复习合计',async()=>{
  mocks.fetch.mockResolvedValue(countedFixture('COUNT_REVIEW',true));const w=await page()
  expect(w.text()).toContain('今日已复习 37 词');await button(w,'再记一笔').trigger('click');await flushPromises()
  expect(w.findComponent(ElInputNumber).props('modelValue')).toBeUndefined();expect(w.text()).toContain('复习词数单独记录，不增加新词量')
 })
 it('历史确认不显示虚构数量，离线无法填写数量',async()=>{
  const data=countedFixture('COUNT_REVIEW',true);data.sections![0].tasks[0].recordedAmount=null;mocks.online.value=false;mocks.snapshot=data
  const w=await page();expect(w.text()).toContain('数量未记录');expect(button(w,'再记一笔').attributes('disabled')).toBeDefined()
 })
})
