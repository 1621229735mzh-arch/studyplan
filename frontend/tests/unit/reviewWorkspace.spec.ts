import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus, { ElButton, ElDatePicker, ElSelect } from 'element-plus'
import ReviewView from '@/features/review/views/ReviewView.vue'
import ReviewScheduleDialog from '@/features/review/components/ReviewScheduleDialog.vue'
import RecordArchiveTree from '@/features/learning/components/RecordArchiveTree.vue'
import ReviewProgressSection from '@/features/progress/components/ReviewProgressSection.vue'

const mocks = vi.hoisted(() => ({ workspace: vi.fn(), list: vi.fn(), schedule: vi.fn(), online: true }))
vi.mock('@/features/review/api/workspace', () => ({ getReviewWorkspace: mocks.workspace, scheduleReview: mocks.schedule }))
vi.mock('@/features/review/api', () => ({ listReviewItems: mocks.list }))
vi.mock('@/features/review/api/automatic', () => ({ getAutoReviewBooks: vi.fn(async () => []), getAutoReviewSegments: vi.fn(async () => []), setAutoReviewPolicy: vi.fn() }))
vi.mock('@/features/review/components/ReviewItemPanel.vue', () => ({ default: { template: '<div>复习内容库</div>' } }))
vi.mock('@/features/review/components/SuggestionPanel.vue', () => ({ default: { template: '<div>待安排与日期建议</div>' } }))
vi.mock('@/composables/useOnlineStatus', () => ({ useOnlineStatus: () => ({ online: computed(() => mocks.online) }) }))
vi.mock('@/stores/catalog', () => ({ useCatalogStore: () => ({ load: vi.fn(), error: null, subjects: [{ id: 1, name: '数学一' }], subjectNameOf: () => '数学一' }) }))
vi.mock('vue-router', async () => ({ ...await vi.importActual<typeof import('vue-router')>('vue-router'), useRoute: () => ({ query: {} }) }))
const totals = { itemCount: 2, reviewedItems: 1, completedReviews: 1, reviewDays: 1, recordCount: 2 }
function fixture() {
  return { totals, months: [{ month: '2026-10', scheduledDays: 2, entryCount: 2, completedCount: 1 }], entries: [
    { reviewItemId: 1, subjectId: 1, title: '已学内容甲', date: '2026-10-12', completionPercent: 100, result: 'VAGUE', scheduled: false, version: 3 },
    { reviewItemId: 2, subjectId: 1, title: '已学内容乙', date: '2026-10-13', completionPercent: 40, result: null, scheduled: true, version: 4 }
  ] }
}
const options = { global: { plugins: [ElementPlus], stubs: { RouterLink: { template: '<a><slot /></a>' }, teleport: true, ElDialog: { props: ['modelValue'], template: '<section v-if="modelValue"><slot /><slot name="footer" /></section>' } } }, attachTo: document.body }
const wrappers: ReturnType<typeof mount>[] = []
async function render(component: any, props = {}) {
  const w = mount(component, { ...options, props }); wrappers.push(w); await flushPromises(); return w
}
function button(w: ReturnType<typeof mount>, text: string) { return w.findAllComponents(ElButton).find(b => b.text() === text)! }
beforeEach(() => {
  vi.clearAllMocks(); mocks.online = true; mocks.workspace.mockResolvedValue(fixture()); mocks.list.mockResolvedValue([]); mocks.schedule.mockResolvedValue({})
  vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date('2026-10-13T12:00:00+08:00'))
})
afterEach(() => { wrappers.splice(0).forEach(w => w.unmount()); document.body.innerHTML = ''; vi.useRealTimers() })

describe('复习工作区框架', () => {
  it('沿用整体、月、周、日导航，整体按月份展示原复习事实', async () => {
    const w = await render(ReviewView)
    expect(w.findAll('nav button').map(b => b.text())).toEqual(['整体', '月', '周', '日'])
    expect(w.get('nav button[aria-current="page"]').text()).toBe('整体')
    expect(w.text()).toContain('2026 年 10 月'); expect(w.text()).toContain('已完整复习过')
    expect(w.text()).not.toContain('预计用时'); expect(mocks.schedule).not.toHaveBeenCalled()
  })
  it('月份进入月历，日期进入同一份日复习记录', async () => {
    const w = await render(ReviewView); await button(w, '查看月计划 ↗').trigger('click'); await flushPromises()
    expect(w.findAll('.calendar-cell')).toHaveLength(35)
    await w.get('[aria-label="查看 2026-10-12 日复习"]').trigger('click'); await flushPromises()
    expect(w.text()).toContain('已学内容甲'); expect(w.text()).toContain('已完成 · 模糊')
    expect(w.findAll('.daily-controls')).toHaveLength(0)
  })
  it('周视图保留七个日期，科目筛选交给后端', async () => {
    const w = await render(ReviewView); await w.findAll('nav button')[2]!.trigger('click')
    expect(w.findAll('.calendar-cell')).toHaveLength(7)
    w.findComponent(ElSelect).vm.$emit('update:modelValue', 1); await flushPromises()
    expect(mocks.workspace).toHaveBeenLastCalledWith(1); expect(mocks.list).toHaveBeenLastCalledWith({ subjectId: 1 })
  })
  it('没有复习内容时保留四种视图，不生成示例任务', async () => {
    mocks.workspace.mockResolvedValue({ months: [], entries: [], totals: { itemCount: 0, reviewedItems: 0, completedReviews: 0, reviewDays: 0, recordCount: 0 } })
    const w = await render(ReviewView); expect(w.text()).toContain('从第一次复习开始')
    await w.findAll('nav button')[3]!.trigger('click'); expect(w.text()).toContain('这一天还没有复习安排')
    expect(mocks.schedule).not.toHaveBeenCalled()
  })
  it('离线只提供今日快照入口，不读取完整复习数据或开放写入', async () => {
    mocks.online = false; const w = await render(ReviewView)
    expect(w.text()).toContain('联网后查看复习计划'); expect(button(w, '＋ 添加安排').attributes('disabled')).toBeDefined()
    expect(mocks.workspace).not.toHaveBeenCalled(); expect(mocks.list).not.toHaveBeenCalled()
  })
  it('接口失败显示错误和重试，不伪装为空清单', async () => {
    mocks.workspace.mockRejectedValueOnce(new Error('读取失败')); const w = await render(ReviewView)
    expect(w.text()).toContain('读取失败'); expect(w.text()).not.toContain('从第一次复习开始')
    await button(w, '重新加载').trigger('click'); await flushPromises(); expect(w.text()).toContain('2026 年 10 月')
  })
})

describe('复习安排与跨页展示', () => {
  it('确认才提交原复习项与版本，调整日期明确展示原安排', async () => {
    const w = await render(ReviewScheduleDialog, { modelValue: true, date: '2026-10-15', itemId: 2, items: [{ id: 2, title: '已有内容', status: 'SCHEDULED', scheduledDate: '2026-10-13', version: 4 }] })
    expect(w.text()).toContain('目前安排在 2026-10-13'); expect(mocks.schedule).not.toHaveBeenCalled()
    await button(w, '确认安排').trigger('click'); await flushPromises()
    expect(mocks.schedule).toHaveBeenCalledWith(2, '2026-10-15', 4)
    expect(w.emitted('saved')).toEqual([['2026-10-15']])
  })
  it('保存失败保留日期与所选内容，不发出成功事件', async () => {
    mocks.schedule.mockRejectedValueOnce(new Error('版本已变化'))
    const w = await render(ReviewScheduleDialog, { modelValue: true, date: '2026-10-15', itemId: 2, items: [{ id: 2, title: '已有内容', status: 'PENDING', version: 4 }] })
    await button(w, '确认安排').trigger('click'); await flushPromises()
    expect(w.text()).toContain('版本已变化'); expect(w.findComponent(ElDatePicker).props('modelValue')).toBe('2026-10-15')
    expect(w.findComponent(ElSelect).props('modelValue')).toBe(2); expect(w.emitted('saved')).toBeUndefined()
  })
  it('只有复习的日期仍能在记录树看到反馈与备注，不提供学习记录编辑按钮', async () => {
    const stats = { learningDays: 1, recordCount: 0, studyMinutes: null, missingDurationRecords: 0, amounts: [], reviewRecordCount: 1 }
    const w = await render(RecordArchiveTree, { months: [{ month: '2026-10', stats, weeks: [{ weekStart: '2026-10-12', from: '2026-10-12', to: '2026-10-18', stats, days: [{ date: '2026-10-13', stats, groups: [], reviews: [{ id: 7, reviewItemId: 1, title: '已有复习内容', subjectName: '数学一', result: 'VAGUE', progressDelta: 60, note: '需要再巩固' }] }] }] }] })
    expect(w.text()).toContain('复习痕迹'); expect(w.text()).toContain('完成复习 · 模糊'); expect(w.text()).toContain('需要再巩固')
    expect(w.findAll('.record-actions')).toHaveLength(0)
  })
  it('进度展示内容覆盖和次数，不生成总体百分比；空数据保留结构', async () => {
    const w = await render(ReviewProgressSection, { reviews: [{ direction: 'MATH', subjectId: 1, subjectName: '数学一', totals }] })
    expect(w.text()).toContain('已完整复习过'); expect(w.text()).toContain('累计完成复习'); expect(w.text()).not.toContain('%')
    await w.setProps({ reviews: [] }); expect(w.text()).toContain('还没有纳入复习的内容')
    await w.setProps({ available: false }); expect(w.text()).toContain('尚未同步复习概览')
    expect(w.text()).not.toContain('还没有纳入复习的内容')
  })
})
