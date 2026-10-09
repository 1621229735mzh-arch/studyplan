import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus, { ElButton, ElCheckbox, ElSelect } from 'element-plus'
import PlanView from '@/features/plan/views/PlanView.vue'
import ImportDialog from '@/features/plan/components/PlanImportDialog.vue'
import { addDays, monthDates, weekDates, weekStart, moveMonth } from '@/features/plan/workspace'
import { HttpApiError } from '@/api/http'
import { listPlanTasks, listStageGoals } from '@/features/plan/api'

const mocks = vi.hoisted(() => ({ workspace: vi.fn(), days: vi.fn(), reorder: vi.fn(), preview: vi.fn(), confirm: vi.fn(), online: true }))
vi.mock('@/features/plan/api/workspace', () => ({ getWorkspace: mocks.workspace, reorderDay: mocks.reorder, previewImport: mocks.preview, confirmImport: mocks.confirm, quickAddDay: vi.fn() }))
vi.mock('@/features/plan/api', () => ({ getDayPlan: mocks.days, listPlanTasks: vi.fn().mockResolvedValue([]), listStageGoals: vi.fn().mockResolvedValue([]), removeDayPlanItem: vi.fn(), adjustDayPlanItem: vi.fn() }))
vi.mock('@/composables/useOnlineStatus', () => ({ useOnlineStatus: () => ({ online: computed(() => mocks.online) }) }))
vi.mock('@/stores/catalog', () => ({ useCatalogStore: () => ({ load: vi.fn(), error: null, loading: false, subjects: [{ id: 1, name: '数学', enabled: true }], enabledSubjects: [{ id: 1, name: '数学' }], enabledUnits: [{ id: 1, name: '讲' }], subjectNameOf: () => '数学', unitNameOf: () => '讲' }) }))
vi.mock('@/stores/session', () => ({ useSessionStore: () => ({ username: 'test' }) }))
vi.mock('@/offline/useOfflineSnapshot', () => ({ useOfflineSnapshotStore: () => ({ load: vi.fn(), save: vi.fn(), week: { weekStartDate: '2026-10-05', schedule: { syncedAt: '2026-10-08T00:00:00Z', subjects: [{ id: 1, name: '数学' }], entries: [{ date: '2026-10-08', taskId: 1, subjectId: 1, taskTitle: '离线日安排', unitName: '讲', plannedAmount: 2 }] } } }) }))
vi.mock('@/features/plan/components/TaskPanel.vue', () => ({ default: { template: '<div>任务库内容</div>' } }))
vi.mock('@/features/plan/components/StageGoalPanel.vue', () => ({ default: { template: '<div>目标内容</div>' } }))
const wrappers: ReturnType<typeof mount>[] = []
const options = { global: { plugins: [ElementPlus], stubs: { RouterLink: { template: '<a><slot /></a>' }, teleport: true, ElDialog: { props: ['modelValue'], template: '<section v-if="modelValue"><slot /><slot name="footer" /></section>' } } }, attachTo: document.body }
async function render(component: typeof PlanView | typeof ImportDialog, props = {}) { const w = mount(component, { ...options, props }); wrappers.push(w); await flushPromises(); return w }
function button(w: ReturnType<typeof mount>, text: string) { return w.findAllComponents(ElButton).find(b => b.text() === text)! }
function fixture() { return { revision: 0, months: [{ month: '2026-10', scheduledDays: 1, taskCount: 1, amounts: [{ subjectId: 1, unitId: 1, unitName: '讲', plannedAmount: 5, completedAmount: 2 }] }], entries: [{ date: '2026-10-12', taskId: 1, taskTitle: '极限基础', subjectId: 1, unitName: '讲', plannedAmount: 5, completedAmount: 2 }], outlines: [], imports: [] } }
function previewFixture() { return { previewToken: 'preview-token', revision: 0, taskCount: 1, dayCount: 1, itemCount: 1, warnings: ['当天已有同名任务'], document: { schemaVersion: 1, planId: 'test', title: '测试方案', startDate: '2026-10-12', endDate: '2026-10-12', goals: [], months: [], weeks: [], tasks: [{ key: 't', subject: '数学', unit: '讲', title: '极限基础', plannedAmount: 5 }], days: [{ date: '2026-10-12', items: [{ taskKey: 't', amount: 5 }] }] } } }
beforeEach(() => { vi.clearAllMocks(); vi.mocked(listPlanTasks).mockResolvedValue([]); vi.mocked(listStageGoals).mockResolvedValue([]); vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date('2026-10-08T12:00:00+08:00')); mocks.online = true; mocks.workspace.mockResolvedValue(fixture()); mocks.days.mockResolvedValue([]); mocks.reorder.mockResolvedValue([]); mocks.preview.mockResolvedValue(previewFixture()); mocks.confirm.mockResolvedValue(previewFixture()) })
afterEach(() => { wrappers.splice(0).forEach(w => w.unmount()); document.body.innerHTML = ''; vi.useRealTimers() })

describe('计划时间视图', () => {
  it('默认整体时间轴，显示后端按单位返回的进度', async () => { const w = await render(PlanView); expect(w.get('nav button[aria-current="page"]').text()).toBe('整体'); expect(w.text()).toContain('2026 年 10 月'); expect(w.text()).toContain('已学 2 讲'); expect(w.text()).not.toContain('AI 生成'); expect(w.find('.calendar-grid').exists()).toBe(false) })
  it('从月份进入月历，再点击日期进入日安排', async () => { const w = await render(PlanView); await button(w, '查看月计划 ↗').trigger('click'); await flushPromises(); expect(w.findAll('.calendar-cell')).toHaveLength(35); await w.get('[aria-label="查看 2026-10-12 日计划"]').trigger('click'); await flushPromises(); expect(mocks.days).toHaveBeenLastCalledWith('2026-10-12'); expect(w.text()).toContain('这一天还没有学习安排') })
  it('周视图展示同一套日安排，科目过滤交给后端', async () => { const w = await render(PlanView); await w.findAll('nav button')[2].trigger('click'); await flushPromises(); expect(w.findAll('.calendar-cell')).toHaveLength(7); w.findComponent(ElSelect).vm.$emit('update:modelValue', 1); await flushPromises(); expect(mocks.workspace).toHaveBeenLastCalledWith(1) })
  it('离线禁止新增与导入，保留明确的联网提示', async () => { mocks.online = false; const w = await render(PlanView); expect(button(w, '＋ 添加安排').attributes('disabled')).toBeDefined(); expect(button(w, '导入外部方案').attributes('disabled')).toBeDefined(); expect(mocks.workspace).not.toHaveBeenCalled(); expect(w.text()).toContain('当前离线') })
  it('离线本周只读展示已同步的日安排与同步时间', async () => { mocks.online = false; const w = await render(PlanView); await button(w, '查看本周').trigger('click'); await flushPromises(); expect(w.text()).toContain('离线日安排'); expect(w.text()).toContain('同步于'); expect(mocks.workspace).not.toHaveBeenCalled() })
  it('排序提交完整条目及其版本', async () => {
    mocks.days.mockResolvedValue([{ id: 10, taskId: 1, taskTitle: '任务甲', subjectId: 1, unitName: '讲', source: 'MANUAL', version: 2, plannedAmount: 1 }, { id: 11, taskId: 2, taskTitle: '任务乙', subjectId: 1, unitName: '讲', source: 'IMPORT', version: 3, plannedAmount: 2 }]);
    const w = await render(PlanView); await w.findAll('nav button')[3].trigger('click'); await flushPromises(); await w.get('[aria-label="向下移动 任务甲"]').trigger('click'); await flushPromises(); expect(mocks.reorder).toHaveBeenCalledWith('2026-10-08', [{ id: 11, version: 3 }, { id: 10, version: 2 }])
  })
})

describe('外部方案预览与确认', () => {
  it('保留原始 JSON，未经确认不写入', async () => { const w = await render(ImportDialog, { modelValue: true }); const raw = '{"schemaVersion":1,"schemaVersion":1}'; await w.get('textarea').setValue(raw); await button(w, '校验并预览').trigger('click'); await flushPromises(); expect(mocks.preview).toHaveBeenCalledWith(raw); expect(w.text()).toContain('当天已有同名任务'); expect(w.text()).toContain('极限基础'); expect(button(w, '确认追加').attributes('disabled')).toBeDefined(); expect(mocks.confirm).not.toHaveBeenCalled() })
  it('确认失败保留文件并要求重新预览', async () => {
    const w = await render(ImportDialog, { modelValue: true }); const raw = '{"schemaVersion":1}'; await w.get('textarea').setValue(raw); await button(w, '校验并预览').trigger('click'); await flushPromises(); w.findComponent(ElCheckbox).vm.$emit('update:modelValue', true); await flushPromises(); mocks.confirm.mockRejectedValue(new HttpApiError({ code: 'PLAN_PREVIEW_STALE', status: 409, message: '请重新预览' })); await button(w, '确认追加').trigger('click'); await flushPromises(); expect(mocks.confirm).toHaveBeenCalledWith(raw, 'preview-token'); expect(w.get('textarea').element.value).toBe(raw); expect(w.text()).toContain('请重新预览'); expect(w.emitted('imported')).toBeUndefined()
  })
  it('返回修改文件会使旧预览失效', async () => { const w = await render(ImportDialog, { modelValue: true }); await w.get('textarea').setValue('{}'); await button(w, '校验并预览').trigger('click'); await flushPromises(); await button(w, '返回修改').trigger('click'); await w.get('textarea').setValue('{"title":"changed"}'); expect(button(w, '确认追加')).toBeUndefined(); expect(mocks.confirm).not.toHaveBeenCalled() })
})

describe('日历日期边界', () => {
  it('周一基准支持周日与跨年', () => { expect(weekStart('2027-01-03')).toBe('2026-12-28'); expect(weekDates('2027-01-03')).toHaveLength(7); expect(weekDates('2027-01-03')[6]).toBe('2027-01-03') })
  it('闰年和月底跳转不漏日期', () => { expect(monthDates('2028-02-29')).toContain('2028-02-29'); expect(moveMonth('2027-01-31',1)).toBe('2027-02-01'); expect(addDays('2028-02-28',1)).toBe('2028-02-29') })
})
