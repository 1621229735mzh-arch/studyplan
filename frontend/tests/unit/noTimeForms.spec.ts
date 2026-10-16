import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus, { ElButton, ElCheckbox, ElInputNumber } from 'element-plus'
import SuggestionPanel from '@/features/review/components/SuggestionPanel.vue'
import DayScheduleEditor from '@/features/plan/components/DayScheduleEditor.vue'
import StudySettingsPanel from '@/features/settings/components/StudySettingsPanel.vue'

const mocks = vi.hoisted(() => ({ confirm: vi.fn(), adjust: vi.fn(), settings: vi.fn(), update: vi.fn() }))
vi.mock('@/features/review/api', () => ({
  getReviewSuggestions: vi.fn(async () => ({ date: '2026-10-16', dailyReviewMinutes: null, scheduledMinutes: 0,
    remainingMinutes: null, warnings: ['旧接口的缺少用时提示'], items: [{ reviewItemId: 1, title: '极限复习', dueDate: '2026-10-16', estimatedMinutes: null }] })),
  confirmReviewSuggestions: mocks.confirm
}))
vi.mock('@/features/plan/api', () => ({ adjustDayPlanItem: mocks.adjust }))
vi.mock('@/features/plan/api/workspace', () => ({ quickAddDay: vi.fn() }))
vi.mock('@/features/settings/api', () => ({ getStudySettings: mocks.settings, updateStudySettings: mocks.update }))
vi.mock('@/stores/catalog', () => ({ useCatalogStore: () => ({ load: vi.fn(), subjectNameOf: () => '数学', enabledSubjects: [], enabledUnits: [] }) }))
const wrappers: ReturnType<typeof mount>[] = []
const global = { plugins: [ElementPlus], stubs: { teleport: true, ElDialog: { template: '<div><slot /><slot name="footer" /></div>' } } }
function button(w: ReturnType<typeof mount>, label: string) { return w.findAllComponents(ElButton).find(b => b.text() === label)! }
beforeEach(() => {
  vi.clearAllMocks()
  mocks.confirm.mockResolvedValue({ scheduled: [{ id: 1, title: '极限复习' }], rejected: [] })
  mocks.adjust.mockResolvedValue({})
  const settings = { version: 3, examDate: null, dailyStudyMinutes: 120, dailyReviewMinutes: 30,
    reviewPhase: 'MAIN', reviewIntervalForgotDays: 1, reviewIntervalVagueDays: 3, reviewIntervalMasteredDays: 7 }
  mocks.settings.mockResolvedValue(settings); mocks.update.mockResolvedValue({ ...settings, version: 4 })
})
afterEach(() => wrappers.splice(0).forEach(w => w.unmount()))
describe('不计时表单', () => {
  it('没有预算和预计用时也能勾选确认，失败保留选择', async () => {
    const w = mount(SuggestionPanel, { global }); wrappers.push(w); await flushPromises()
    expect(w.findAllComponents(ElInputNumber)).toHaveLength(0)
    expect(w.text()).not.toContain('旧接口的缺少用时提示')
    w.findComponent(ElCheckbox).vm.$emit('update:modelValue', true); await flushPromises()
    mocks.confirm.mockRejectedValueOnce(new Error('暂时失败'))
    await button(w, '确认进入当天安排').trigger('click'); await flushPromises()
    expect(w.findComponent(ElCheckbox).props('modelValue')).toBe(true)
    await button(w, '确认进入当天安排').trigger('click'); await flushPromises()
    expect(mocks.confirm).toHaveBeenLastCalledWith({ date: expect.any(String), manualSelection: true, items: [{ reviewItemId: 1 }] })
    expect(w.text()).toContain('本次确认 1 条')
  })
  it('调整已有接续计划无需输入用时且保留原估算', async () => {
    const w = mount(DayScheduleEditor, { global, props: { modelValue: false, date: '2026-10-16', tasks: [],
      item: { id: 1, taskId: 2, taskTitle: '教材第一段', subjectId: 1, unitId: 1, plannedAmount: null,
        estimatedMinutes: 90, version: 2, activityMode: 'CONTINUED_STUDY', source: 'IMPORT' } as never } })
    wrappers.push(w); await w.setProps({ modelValue: true }); await flushPromises()
    expect(w.findAllComponents(ElInputNumber)).toHaveLength(0)
    await button(w, '保存安排').trigger('click'); await flushPromises()
    expect(mocks.adjust).toHaveBeenCalledWith('2026-10-16', 1, expect.objectContaining({ plannedAmount: null, estimatedMinutes: 90, version: 2 }))
  })
  it('设置不再显示时间预算输入，保存其他设置保留历史预算', async () => {
    const w = mount(StudySettingsPanel, { global }); wrappers.push(w); await flushPromises()
    expect(w.text()).not.toContain('每日学习预算'); expect(w.text()).not.toContain('每日复习额度')
    expect(w.findAllComponents(ElInputNumber)).toHaveLength(3) // 三档复习日期间隔
    await button(w, '保存设置').trigger('click'); await flushPromises()
    expect(mocks.update).toHaveBeenCalledWith(expect.objectContaining({ dailyStudyMinutes: 120, dailyReviewMinutes: 30, version: 3 }))
  })
})
