import { computed } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus, { ElButton, ElSelect } from 'element-plus'
import { HttpApiError } from '@/api/http'
import LearningView from '@/features/learning/views/LearningView.vue'
import ProgressView from '@/features/progress/views/ProgressView.vue'
import DirectionView from '@/features/progress/views/DirectionProgressView.vue'
import RecordForm from '@/features/learning/components/RecordEditorForm.vue'
import CatalogImportDialog from '@/features/progress/components/CatalogImportDialog.vue'
import PeriodFilter from '@/components/PeriodFilter.vue'
import { listPlanTasks } from '@/features/plan/api'
import { todayIso, weekStartOf } from '@/composables/useFormat'
import type { RecordArchive, LearningRecord } from '@/features/learning/types'
import type { CatalogLibrary } from '@/features/progress/catalog'
import { learningRecordFixture, subjectsFixture, unitsFixture } from '../fixtures/learning'

const mocks = vi.hoisted(() => ({ get: vi.fn(), archive: vi.fn(), update: vi.fn(), create: vi.fn(), remove: vi.fn(), classify: vi.fn(), preview: vi.fn(), confirm: vi.fn(), candidates: vi.fn(), link: vi.fn(), online: true, cache: null as unknown, save: vi.fn(), cacheLoad: vi.fn() }))
vi.mock('@/features/progress/api/catalog', () => ({ getCatalog: mocks.get, getTaskCandidates: mocks.candidates, classifyRecord: mocks.classify, previewCatalog: mocks.preview, confirmCatalog: mocks.confirm, linkNodeTasks: mocks.link }))
vi.mock('@/features/learning/api', () => ({ getRecordArchive: mocks.archive, updateLearningRecord: mocks.update, createLearningRecord: mocks.create, deleteLearningRecord: mocks.remove }))
vi.mock('@/features/plan/api', () => ({ listPlanTasks: vi.fn() }))
vi.mock('@/composables/useOnlineStatus', () => ({ useOnlineStatus: () => ({ online: computed(() => mocks.online) }) }))
vi.mock('@/stores/session', () => ({ useSessionStore: () => ({ username: 'test' }) }))
vi.mock('@/stores/catalog', () => ({ useCatalogStore: () => ({ load: vi.fn(), error: null, subjects: subjectsFixture, units: unitsFixture, subjectNameOf: () => '操作系统', unitNameOf: () => '讲' }) }))
vi.mock('@/offline/useOfflineSnapshot', () => ({ useOfflineSnapshotStore: () => ({ load: mocks.cacheLoad, save: mocks.save, get progress() { return mocks.cache } }) }))
function archiveFixture(record: LearningRecord = learningRecordFixture): RecordArchive {
  const r = { ...record, recordDate: todayIso() }
  const stats = { learningDays: 1, recordCount: 1, studyMinutes: r.durationMinutes ?? null, missingDurationRecords: r.durationMinutes == null ? 1 : 0, amounts: [{ subjectId: 2, subjectName: '操作系统', unitId: 3, unitName: '讲', amount: r.amount ?? 0 }] }
  return { stats, months: [{ month: todayIso().slice(0,7), stats, weeks: [{ weekStart: weekStartOf(todayIso()), from: todayIso(), to: todayIso(), stats, days: [{ date: todayIso(), stats, groups: [{ key: 'group', subjectId: 2, subjectName: '操作系统', courseId: 1, title: '操作系统课程', unitId: 3, unitName: '讲', amount: r.amount ?? null, studyMinutes: r.durationMinutes ?? null, missingDurationRecords: stats.missingDurationRecords, records: [r] }] }] }] }] }
}
const library: CatalogLibrary = { revision: 3, courses: [{ id: 1, key: 'os', direction: 'CS408', branch: '操作系统', title: '操作系统课程', subjectId: 2, unitId: 3, unitName: '讲', totalAmount: 3, completedAmount: 2, recordedAmount: 2, nodes: [{ id: 1, key: 'lesson1', parentKey: null, title: '第一讲', totalAmount: 3, completedAmount: 2, taskIds: [11], sortOrder: 0 }] }], cumulative: [{ direction: 'CS408', subjectId: 2, subjectName: '操作系统', unitId: 3, unitName: '讲', amount: 2 }], unclassified: [] }
const wrappers: ReturnType<typeof mount>[] = []
async function render(component: object, path = '/learning') {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/learning', component: { template: '<div />' } }, { path: '/progress', component: { template: '<div />' } }, { path: '/progress/:direction', component: { template: '<div />' } }, { path: '/', component: { template: '<div />' } }] })
  await router.push(path); await router.isReady()
  const w = mount(component, { global: { plugins: [ElementPlus, router], stubs: { teleport: true, ElDialog: { props: ['modelValue'], template: '<section v-if="modelValue"><slot /></section>' } } } })
  wrappers.push(w); await flushPromises(); return w
}
function button(w: ReturnType<typeof mount>, label: string) { return w.findAllComponents(ElButton).find(b => b.text() === label)! }
beforeEach(() => { vi.clearAllMocks(); vi.mocked(listPlanTasks).mockResolvedValue([]); mocks.online = true; mocks.cache = null; mocks.get.mockResolvedValue(structuredClone(library)); mocks.archive.mockResolvedValue(archiveFixture()); mocks.update.mockResolvedValue(learningRecordFixture); mocks.create.mockResolvedValue(learningRecordFixture); mocks.save.mockResolvedValue(undefined); mocks.candidates.mockResolvedValue([]); mocks.cacheLoad.mockResolvedValue(undefined) })
afterEach(() => { wrappers.splice(0).forEach(w => w.unmount()) })

describe('月周日学习记录与目录进度', () => {
  it('内容目录显示原百分比与内容项数，不转换为讲数或生成整体环形百分比',async()=>{
    const value=structuredClone(library)
    const course=value.courses[0]
    Object.assign(course,{direction:'MATH',branch:'高等数学',title:'高数教材',progressMode:'CONTENT_PERCENT',totalAmount:null,completedAmount:null,recordedAmount:null,contentCounts:{total:2,completed:0,inProgress:1,remaining:1}})
    course.nodes=[{id:1,key:'chapter',parentKey:null,title:'第1讲',totalAmount:null,completedAmount:0,taskIds:[],sortOrder:0,contentCounts:course.contentCounts},
      {id:2,key:'part1',parentKey:'chapter',title:'函数与极限',totalAmount:null,completedAmount:0,taskIds:[11],sortOrder:1,completionPercent:40},
      {id:3,key:'part2',parentKey:'chapter',title:'连续性',totalAmount:null,completedAmount:0,taskIds:[12],sortOrder:2,completionPercent:0}]
    value.cumulative=[];mocks.get.mockResolvedValue(value)
    const w=await render(DirectionView,'/progress/MATH?course=1')
    expect(w.find('.course-ring').exists()).toBe(false)
    expect(w.get('[aria-label="内容学习进度"]').text()).toContain('进行中 1 项')
    expect(w.text()).toContain('已完成 40%');expect(w.text()).not.toContain('40 /');expect(w.text()).not.toContain('0 / 2 讲')
    expect(w.findAll('.catalog-leaf')).toHaveLength(2)
    expect(w.get('progress[aria-label="函数与极限完成进度"]').attributes('value')).toBe('40')
    const saved=mocks.save.mock.calls[0][0].progress.catalog.courses[0]
    expect(saved.contentCounts).toEqual({total:2,completed:0,inProgress:1,remaining:1});expect(saved).not.toHaveProperty('nodes')
    Object.assign(course.contentCounts!,{completed:1,inProgress:0});course.nodes[1].completionPercent=100
    await button(w,'刷新进度').trigger('click');await flushPromises()
    expect(w.text()).toContain('已完成 100%')
  })
  it('内容百分比目录的离线概览只保留项数，完整条目和归属需联网',async()=>{
    mocks.online=false
    const course={...library.courses[0],direction:'MATH',progressMode:'CONTENT_PERCENT',contentCounts:{total:126,completed:1,inProgress:1,remaining:124}}
    mocks.cache={planningBasis:'catalog',catalog:{...library,courses:[course]}}
    const w=await render(DirectionView,'/progress/MATH?course=1')
    expect(w.get('[aria-label="内容学习进度"]').text()).toContain('完整目录 126 项内容')
    expect(w.find('.course-ring').exists()).toBe(false);expect(w.find('.catalog-tree').exists()).toBe(false)
    expect(button(w,'维护目录').attributes('disabled')).toBeDefined()
  })
  it('复习数量单独展示，修改时回填实际词数并保留原任务与版本', async () => {
    const record: LearningRecord = { ...learningRecordFixture, activityMode: 'COUNT_REVIEW', taskTitle: '每日单词复习', unitName: '词', amount: null, reviewAmount: 120, durationMinutes: null }
    const value = archiveFixture(record)
    const group = value.months[0].weeks[0].days[0].groups[0]
    group.courseId = null; group.title = '每日单词复习'; group.reviewAmount = 120; group.unitName = '词'
    mocks.archive.mockResolvedValue(value)
    const w = await render(LearningView)
    expect(w.text()).toContain('复习 120 词'); expect(w.text()).toContain('不增加新词量')
    await button(w,'修改').trigger('click'); await flushPromises()
    const form = w.findComponent(RecordForm).vm as unknown as { form: { amount: number }; submit: () => void }
    expect(form.form.amount).toBe(120)
    expect(w.findComponent(RecordForm).text()).toContain('实际复习量（不增加新词量）')
    expect(w.findComponent(RecordForm).findAllComponents(ElSelect).find(s => s.props('modelValue') === record.taskId)?.props('disabled')).toBe(true)
    form.form.amount = 80; form.submit(); await flushPromises()
    expect(mocks.update).toHaveBeenCalledWith(record.id,expect.objectContaining({ amount: 80, taskId: record.taskId, version: record.version, durationMinutes: null }))
  })
  it('记录默认读取全部月份，仅本月、本周展开，日内先合并再看单次明细', async () => {
    const value = archiveFixture(); value.months.push({ month: '2000-01', stats: value.stats, weeks: [] }); mocks.archive.mockResolvedValue(value)
    const w = await render(LearningView)
    expect(mocks.archive).toHaveBeenCalledWith({ from: undefined, to: undefined, taskId: undefined, subjectId: undefined })
    expect(w.find('.archive-month').attributes('open')).toBeDefined(); expect(w.findAll('.archive-month')[1].attributes('open')).toBeUndefined()
    expect(w.find('.archive-week').attributes('open')).toBeDefined()
    expect(w.text()).toContain('操作系统课程'); expect(w.findAll('.archive-record')).toHaveLength(1)
  })
  it('自选日期不发送倒置区间，修正后才查询', async () => {
    const w = await render(LearningView)
    await w.findAll('nav button').find(b => b.text() === '自选日期')!.trigger('click')
    const dates = w.findAll('input[type="date"]'); await dates[0].setValue('2030-02-10'); await dates[1].setValue('2030-02-01')
    expect(button(w,'查看').attributes('disabled')).toBeDefined(); expect(mocks.archive).toHaveBeenCalledTimes(1)
    await dates[1].setValue('2030-02-28'); await button(w,'查看').trigger('click'); await flushPromises()
    expect(mocks.archive).toHaveBeenLastCalledWith(expect.objectContaining({ from: '2030-02-10', to: '2030-02-28' }))
  })
  it('版本冲突保留修改输入，不误报保存成功', async () => {
    mocks.update.mockRejectedValue(new HttpApiError({ code: 'STALE', status: 409, message: '版本冲突' }))
    const w = await render(LearningView); await button(w,'修改').trigger('click'); await flushPromises()
    const form = w.findComponent(RecordForm).vm as unknown as { form: { note: string }; submit: () => void }
    form.form.note = '保留这次复盘'; form.submit(); await flushPromises()
    expect(w.text()).toContain('当前输入已保留'); expect(form.form.note).toBe('保留这次复盘'); expect(mocks.archive).toHaveBeenCalledTimes(1)
  })
  it('无任务列表也保留原内容、数量精度和版本', async () => {
    mocks.archive.mockResolvedValue(archiveFixture({ ...learningRecordFixture, amount: 1.123456, content: '原学习内容', durationMinutes: 0 }))
    const w = await render(LearningView); await button(w,'修改').trigger('click'); await flushPromises()
    ;(w.findComponent(RecordForm).vm as unknown as { submit: () => void }).submit(); await flushPromises()
    expect(mocks.update).toHaveBeenCalledWith(901,expect.objectContaining({ content: '原学习内容', amount: 1.123456, version: 2, durationMinutes: 0, taskId: 11 }))
  })
  it('补记失败保留输入与令牌，成功后关闭窗口并刷新归档', async () => {
    mocks.create.mockRejectedValueOnce(new Error('暂时失败')).mockResolvedValueOnce(learningRecordFixture)
    const w = await render(LearningView); await button(w,'＋ 补记学习').trigger('click'); await flushPromises()
    const form = w.findComponent(RecordForm).vm as unknown as { fillFrom: (value: object) => void; submit: () => void }
    form.fillFrom({ taskId: null, subjectId: 2, unitId: 3, amount: 2, durationMinutes: 30, content: '独立补记' }); form.submit(); await flushPromises(); expect(w.text()).toContain('暂时失败')
    form.submit(); await flushPromises(); expect(mocks.create.mock.calls[0][0]).toEqual(mocks.create.mock.calls[1][0]); expect(w.findComponent(RecordForm).exists()).toBe(false); expect(mocks.archive).toHaveBeenCalledTimes(2)
  })
  it('后返回的旧归档不覆盖新筛选', async () => {
    let resolveOld!: (value: RecordArchive) => void
    mocks.archive.mockReturnValueOnce(new Promise(resolve => { resolveOld = resolve }))
    const w = await render(LearningView); w.findComponent(PeriodFilter).vm.$emit('change',{ from:'2030-01-01',to:'2030-01-31' }); await flushPromises()
    const old = archiveFixture(); old.months[0].weeks[0].days[0].groups[0].title = '过期结果'; resolveOld(old); await flushPromises()
    expect(w.text()).not.toContain('过期结果'); expect(w.text()).toContain('操作系统课程')
  })
  it('离线记录不显示原始历史或写入入口', async () => {
    mocks.online = false; const w = await render(LearningView)
    expect(mocks.archive).not.toHaveBeenCalled(); expect(w.find('.record-archive').exists()).toBe(false); expect(button(w,'＋ 补记学习').attributes('disabled')).toBeDefined()
  })
  it('进度首页仅有四个跳转入口，不展示课程内容', async () => {
    const w = await render(ProgressView,'/progress')
    expect(w.findAll('.direction-entry')).toHaveLength(4); expect(w.text()).not.toContain('操作系统课程'); expect(mocks.get).not.toHaveBeenCalled()
    expect(w.findAll('.direction-entry')[3].attributes('href')).toBe('/progress/CS408')
  })
  it('方向到分科再到课程目录，详情不显示学习日期', async () => {
    const w = await render(DirectionView,'/progress/CS408?branch=操作系统&course=1')
    expect(w.find('.course-ring').exists()).toBe(true); expect(w.text()).toContain('2 / 3 讲'); expect(w.text()).toContain('第一讲'); expect(w.text()).not.toContain(learningRecordFixture.recordDate)
    expect(mocks.save).toHaveBeenCalledWith(expect.objectContaining({ progress: expect.objectContaining({ planningBasis: 'catalog', trend: [], catalog: expect.objectContaining({ courses: [expect.not.objectContaining({ nodes: expect.anything() })] }) }) }))
  })
  it('课程详情仅展示完成题号数字，刷新后更新且不缓存题号明细', async () => {
    const value = structuredClone(library)
    value.courses[0].completedProblemNumbers = [31,32,49,128,283]
    mocks.get.mockResolvedValue(value)
    const w = await render(DirectionView,'/progress/CS408?course=1')
    expect(w.find('[aria-label="已完成题号"]').text()).toBe('31、32、49、128、283')
    expect(mocks.save.mock.calls[0][0].progress.catalog.courses[0]).not.toHaveProperty('completedProblemNumbers')
    mocks.get.mockResolvedValue({ ...value,courses:[{ ...value.courses[0],completedProblemNumbers:[31,32,49,283] }] })
    await button(w,'刷新进度').trigger('click'); await flushPromises()
    expect(w.find('[aria-label="已完成题号"]').text()).toBe('31、32、49、283')
    mocks.get.mockResolvedValue(library)
    await button(w,'刷新进度').trigger('click'); await flushPromises()
    expect(w.find('[aria-label="已完成题号"]').exists()).toBe(false)
  })
  it('离线课程概览不显示完成题号明细', async () => {
    mocks.online = false
    mocks.cache = { planningBasis:'catalog',catalog:{ ...library,courses:library.courses.map(({nodes:_nodes,...course})=>({ ...course,completedProblemNumbers:[31,49] })) } }
    const w = await render(DirectionView,'/progress/CS408?course=1')
    expect(w.find('[aria-label="已完成题号"]').exists()).toBe(false)
  })
  it('英语直接展示背词与复习的独立进度，没有总目标或完成百分比', async () => {
    const activities = [
      { taskId:510,title:'每日背诵新词',direction:'ENGLISH',mode:'COUNT_STUDY',subjectId:5,subjectName:'英语一',unitId:4,unitName:'词',recordedAmount:100,recordCount:2,learningDays:2,unknownQuantityRecords:0 },
      { taskId:511,title:'每日单词复习',direction:'ENGLISH',mode:'COUNT_REVIEW',subjectId:5,subjectName:'英语一',unitId:4,unitName:'词',recordCount:2,learningDays:2,unknownQuantityRecords:2 }
    ]
    mocks.get.mockResolvedValue({ ...library,courses:[],cumulative:[{direction:'ENGLISH',subjectId:5,subjectName:'英语一',unitId:4,unitName:'词',amount:100}],activities })
    const w=await render(DirectionView,'/progress/ENGLISH')
    const cards=w.findAll('.activity-card'); expect(cards).toHaveLength(2)
    expect(cards[0].text()).toContain('累计新学 100 词'); expect(cards[0].text()).toContain('2 天 · 2 次记录')
    expect(cards[1].text()).toContain('复习数量未记录'); expect(cards[1].text()).toContain('2 次历史记录未填写数量'); expect(cards[1].text()).toContain('不增加新词量')
    expect(w.find('.course-ring').exists()).toBe(false); expect(w.text()).not.toContain('还没有课程或资料目录'); expect(w.text()).not.toContain('6150')
    expect(mocks.save.mock.calls[0][0].progress.catalog.activities).toEqual(activities)
    const other=await render(DirectionView,'/progress/CS408'); expect(other.findAll('.activity-card')).toHaveLength(0)
  })
  it('离线只读显示已同步的词数进度，兼容没有数量进度的旧快照', async () => {
    mocks.online=false
    mocks.cache={planningBasis:'catalog',syncedAt:'2026-10-17T04:00:00Z',catalog:{revision:3,courses:[],cumulative:[],activities:[{taskId:511,title:'每日单词复习',direction:'ENGLISH',mode:'COUNT_REVIEW',subjectId:5,subjectName:'英语一',unitId:4,unitName:'词',recordedAmount:120,recordCount:3,learningDays:2,unknownQuantityRecords:1}]}}
    const w=await render(DirectionView,'/progress/ENGLISH')
    expect(mocks.get).not.toHaveBeenCalled(); expect(w.text()).toContain('已记录复习 120 词'); expect(w.text()).toContain('1 次历史记录未填写数量'); expect(w.text()).toContain('概览同步时间')
    expect(button(w,'整理课程／资料').attributes('disabled')).toBeDefined()
    mocks.cache={planningBasis:'catalog',catalog:{revision:2,courses:[],cumulative:[]}}
    const old=await render(DirectionView,'/progress/ENGLISH'); expect(old.findAll('.activity-card')).toHaveLength(0)
  })
  it('身份错误不回退快照', async () => {
    mocks.cache = { planningBasis:'catalog',catalog:library }; mocks.get.mockRejectedValue(new HttpApiError({ code:'UNAUTHORIZED',status:401,message:'请重新登录' }))
    const w = await render(DirectionView,'/progress/CS408'); expect(w.text()).toContain('请重新登录'); expect(w.find('.branch-card').exists()).toBe(false)
  })
  it('离线只读课程概览，不暴露目录或待归类记录', async () => {
    mocks.online = false; mocks.cache = { planningBasis:'catalog',syncedAt:'2026-10-08T12:00:00Z',catalog:{ ...library,courses:library.courses.map(({nodes:_nodes,...course})=>course) } }
    const w = await render(DirectionView,'/progress/CS408?course=1')
    expect(mocks.get).not.toHaveBeenCalled(); expect(w.find('.course-ring').exists()).toBe(true); expect(w.find('.catalog-tree').exists()).toBe(false); expect(w.text()).toContain('联网后查看完整目录'); expect(button(w,'维护目录').attributes('disabled')).toBeDefined()
  })
  it('旧口径快照不伪装成完整目录，快照保存失败保留在线结果', async () => {
    mocks.online=false; mocks.cache={planningBasis:'daily'}; const old=await render(DirectionView,'/progress/CS408'); expect(old.text()).toContain('请联网刷新')
    mocks.online=true; mocks.cache=null; mocks.save.mockRejectedValue(new Error('quota exceeded')); const w=await render(DirectionView,'/progress/CS408'); expect(w.find('.branch-card').exists()).toBe(true); expect(w.text()).toContain('离线快照未保存')
  })
  it('离线快照读取失败有明确提示，不产生未处理错误', async () => {
    mocks.online=false; mocks.cacheLoad.mockRejectedValue(new Error('storage unavailable'))
    const w=await render(DirectionView,'/progress/CS408'); expect(w.text()).toContain('无法读取本机进度快照'); expect(w.find('.branch-card').exists()).toBe(false)
  })
  it('待归类记录用原记录版本和目录版本归类，数量不再新增', async () => {
    mocks.get.mockResolvedValue({ ...library,unclassified:[learningRecordFixture] }); mocks.classify.mockResolvedValue(library)
    const w=await render(DirectionView,'/progress/CS408'); const select=w.find('.pending-target').findComponent(ElSelect); select.vm.$emit('update:modelValue',1); await flushPromises(); await button(w,'确认归类').trigger('click'); await flushPromises()
    expect(mocks.classify).toHaveBeenCalledWith(901,1,2,3); expect(mocks.create).not.toHaveBeenCalled(); expect(w.find('.pending-records').exists()).toBe(false)
  })
  it('已归类记录也能在原始明细中更正课程归属并刷新汇总', async () => {
    mocks.classify.mockResolvedValue(library)
    const w=await render(LearningView); await button(w,'课程归属').trigger('click'); await flushPromises()
    w.find('.classification-select').findComponent(ElSelect).vm.$emit('update:modelValue',1); await flushPromises(); await button(w,'确认归属').trigger('click'); await flushPromises()
    expect(mocks.classify).toHaveBeenCalledWith(901,1,2,3); expect(mocks.archive).toHaveBeenCalledTimes(2); expect(mocks.create).not.toHaveBeenCalled()
  })
  it('退出方向页后，慢请求不写入新页面的快照', async () => {
    let resolveOld!: (value: CatalogLibrary) => void
    mocks.get.mockReturnValueOnce(new Promise(resolve => { resolveOld=resolve }))
    const old=await render(DirectionView,'/progress/CS408'); old.unmount()
    await render(DirectionView,'/progress/CS408?course=1'); resolveOld({ ...library,revision:1 }); await flushPromises()
    expect(mocks.save).toHaveBeenCalledTimes(1); expect(mocks.save.mock.calls[0][0].progress.catalog.revision).toBe(3)
  })
  it('目录预览确认失败保留文件及重试令牌，不误报保存', async () => {
    const preview={ previewToken:'token',revision:3,expiresAt:'2030-01-01T12:00:00',document:{schemaVersion:1,courses:[]},changes:[{title:'测试课程',previousTotal:80,nextTotal:100,unitName:'讲',addedNodes:1}] }
    mocks.preview.mockResolvedValue(preview); mocks.confirm.mockRejectedValue(new Error('保存失败'))
    const w=mount(CatalogImportDialog,{props:{modelValue:false},global:{plugins:[ElementPlus],stubs:{teleport:true,ElDialog:{props:['modelValue'],template:'<section v-if="modelValue"><slot /></section>'}}}}); wrappers.push(w); await w.setProps({modelValue:true,initialSource:'{"schemaVersion":1,"courses":[]}'}); await flushPromises()
    await button(w,'校验并预览').trigger('click'); await flushPromises(); w.findComponent({name:'ElCheckbox'}).vm.$emit('update:modelValue',true); await flushPromises(); await button(w,'确认保存目录').trigger('click'); await flushPromises()
    expect(w.text()).toContain('保存失败'); expect(w.find('textarea').element.value).toContain('schemaVersion'); expect(w.emitted('applied')).toBeUndefined(); expect(mocks.confirm).toHaveBeenCalledWith('token')
  })
})
