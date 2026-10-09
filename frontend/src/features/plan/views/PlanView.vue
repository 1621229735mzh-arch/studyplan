<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElAlert, ElButton, ElDatePicker, ElDialog, ElMessage, ElMessageBox, ElOption, ElSelect } from 'element-plus'
import { RouterLink } from 'vue-router'
import { toApiError } from '@/api/http'
import { todayIso, planSourceLabel, formatDateTime } from '@/composables/useFormat'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import { useSessionStore } from '@/stores/session'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import { getDayPlan, listPlanTasks, listStageGoals, removeDayPlanItem } from '../api'
import { getWorkspace, reorderDay } from '../api/workspace'
import { addDays, dateLabel, monthDates, moveMonth, weekDates, weekStart, type PlanViewMode, type PlanWorkspace } from '../workspace'
import type { DailyPlanItemView, PlanTask, StageGoal } from '../types'
import DayScheduleEditor from '../components/DayScheduleEditor.vue'
import PlanImportDialog from '../components/PlanImportDialog.vue'
import TaskPanel from '../components/TaskPanel.vue'
import StageGoalPanel from '../components/StageGoalPanel.vue'

const catalog = useCatalogStore()
const offline = useOfflineSnapshotStore()
const session = useSessionStore()
const { online } = useOnlineStatus()
const mode = ref<PlanViewMode>('overall')
const modes: { key: PlanViewMode; label: string }[] = [{ key: 'overall', label: '整体' }, { key: 'month', label: '月' }, { key: 'week', label: '周' }, { key: 'day', label: '日' }]
const selected = ref(todayIso())
const subjectId = ref<number>()
const data = ref<PlanWorkspace>({ revision: 0, months: [], entries: [], outlines: [], imports: [] })
const tasks = ref<PlanTask[]>([])
const goals = ref<StageGoal[]>([])
const dayItems = ref<DailyPlanItemView[]>([])
const loading = ref(false)
const dayLoading = ref(false)
const writing = ref(false)
const error = ref('')
const editorOpen = ref(false)
const editDate = ref(todayIso())
const editItem = ref<DailyPlanItemView | null>(null)
const importOpen = ref(false)
const library = ref<'tasks' | 'goals' | null>(null)
let loadRequest = 0
let dayRequest = 0
const cells = computed(() => mode.value === 'week' ? weekDates(selected.value) : monthDates(selected.value))
const entriesByDate = computed(() => {
  const result = new Map<string, typeof data.value.entries>()
  for (const entry of data.value.entries) {
    const list = result.get(entry.date) ?? []
    list.push(entry)
    result.set(entry.date, list)
  }
  return result
})
const visibleDayItems = computed(() => dayItems.value.filter(i => i.source !== 'REVIEW' && (!subjectId.value || i.subjectId === subjectId.value)))
const currentMonth = computed(() => data.value.months.find(m => m.month === selected.value.slice(0, 7)))
const filterSubjects = computed(() => catalog.subjects.length ? catalog.subjects : offline.week?.schedule?.subjects ?? [])
const currentOutlines = computed(() => data.value.outlines.filter(o => mode.value === 'week'
  ? o.periodType === 'WEEK' && o.startDate === weekStart(selected.value)
  : o.periodType === 'MONTH' && o.startDate.slice(0, 7) === selected.value.slice(0, 7)))
const viewHeading = computed(() => mode.value === 'overall' ? '你的备考路线' : mode.value === 'month' ? monthLabel(selected.value.slice(0, 7))
  : mode.value === 'week' ? weekStart(selected.value) + ' — ' + addDays(weekStart(selected.value), 6) : dateLabel(selected.value))
function monthLabel(month: string) { return month.slice(0, 4) + ' 年 ' + Number(month.slice(5, 7)) + ' 月' }
function subjectName(id?: number | null) { return catalog.subjectNameOf(id) ?? offline.week?.schedule?.subjects.find(s => s.id === id)?.name ?? '未分类' }
async function loadOfflineWeek() {
  await offline.load()
  const cached = offline.week
  data.value = { revision: 0, months: [], entries: [], outlines: [], imports: [] }
  if (mode.value === 'week' && cached?.weekStartDate === weekStart(selected.value) && cached.schedule) {
    data.value.entries = cached.schedule.entries.filter(e => !subjectId.value || e.subjectId === subjectId.value)
    error.value = ''
  } else error.value = mode.value === 'week' ? '本地没有这一周的日安排快照，请联网后读取。' : '当前离线，可查看已同步的本周计划。完整计划请联网后读取。'
}
function entries(date: string) { return entriesByDate.value.get(date) ?? [] }
function monthGoals(month: string) {
  return goals.value.filter(g => (!subjectId.value || g.subjectId === subjectId.value) && g.startDate && g.targetDate
    && g.startDate.slice(0, 7) <= month && g.targetDate.slice(0, 7) >= month)
}
async function load() {
  const request = ++loadRequest
  if (!online.value) { loading.value = false; await loadOfflineWeek(); return }
  loading.value = true
  error.value = ''
  try {
    const [workspace, taskList, goalList] = await Promise.all([getWorkspace(subjectId.value), listPlanTasks(), listStageGoals()])
    if (request !== loadRequest) return
    data.value = workspace
    tasks.value = taskList
    goals.value = goalList
    if (!subjectId.value && session.username) {
      const start = weekStart(todayIso())
      const end = addDays(start, 6)
      try { await offline.save({ userName: session.username, week: { weekStartDate: start, version: workspace.revision, items: [],
        schedule: { syncedAt: new Date().toISOString(), entries: workspace.entries.filter(e => e.date >= start && e.date <= end),
          subjects: catalog.subjects.map(s => ({ id: s.id, name: s.name })) } } }) }
      catch { ElMessage.warning('计划已读取，本周离线快照暂未保存') }
    }
  } catch (caught) { if (request === loadRequest) error.value = toApiError(caught).message }
  finally { if (request === loadRequest) loading.value = false }
}
async function loadDay() {
  const request = ++dayRequest
  if (mode.value !== 'day' || !online.value) return
  dayLoading.value = true
  dayItems.value = []
  try { const rows = await getDayPlan(selected.value); if (request === dayRequest) dayItems.value = rows }
  catch (caught) { if (request === dayRequest) error.value = toApiError(caught).message }
  finally { if (request === dayRequest) dayLoading.value = false }
}
function enterMonth(month: string) { selected.value = month + '-01'; mode.value = 'month' }
function enterDay(date: string) { selected.value = date; mode.value = 'day' }
function move(direction: number) { selected.value = mode.value === 'month' ? moveMonth(selected.value, direction) : addDays(selected.value, direction * (mode.value === 'week' ? 7 : 1)) }
function openEditor(date = selected.value, item: DailyPlanItemView | null = null) { editDate.value = date; editItem.value = item; editorOpen.value = true }
async function refresh() { await Promise.all([load(), loadDay()]) }
async function saved(date: string) { selected.value = date; await refresh() }
async function remove(item: DailyPlanItemView) {
  if (writing.value || !online.value) return
  const date = selected.value
  try { await ElMessageBox.confirm('从 ' + date + ' 移除“' + item.taskTitle + '”的安排？已有学习记录会保留。', '移除安排', { confirmButtonText: '移除', cancelButtonText: '取消', type: 'warning' }) } catch { return }
  writing.value = true
  error.value = ''
  try { await removeDayPlanItem(date, item.id, item.version); await refresh(); ElMessage.success('已移除安排') }
  catch (caught) { error.value = toApiError(caught).message }
  finally { writing.value = false }
}
async function moveItem(item: DailyPlanItemView, direction: number) {
  if (writing.value || !online.value) return
  const visible = visibleDayItems.value
  const other = visible[visible.findIndex(i => i.id === item.id) + direction]
  if (!other) return
  const ordered = [...dayItems.value]
  const a = ordered.findIndex(i => i.id === item.id)
  const b = ordered.findIndex(i => i.id === other.id)
  ;[ordered[a], ordered[b]] = [ordered[b]!, ordered[a]!]
  writing.value = true
  error.value = ''
  try { await reorderDay(selected.value, ordered.map(i => ({ id: i.id, version: i.version }))); await refresh() }
  catch (caught) { error.value = toApiError(caught).message }
  finally { writing.value = false }
}
watch(subjectId, () => void load())
watch([selected, mode], () => { if (!online.value) void loadOfflineWeek(); void loadDay() })
watch(online, () => void refresh())
onMounted(async () => { if (online.value) await catalog.load(); await load() })
</script>

<template>
  <div class="plan-workspace" :aria-busy="loading">
    <header class="plan-header">
      <div><span class="plan-eyebrow">每一步，都有方向</span><h1>学习计划</h1><p>看清整体节奏，再把目标安排到每一天。</p></div>
      <div class="plan-actions"><ElButton :disabled="!online" @click="importOpen = true">导入外部方案</ElButton><ElButton type="primary" :disabled="!online" @click="openEditor()">＋ 添加安排</ElButton></div>
    </header>
    <div class="plan-navigation">
      <nav aria-label="计划时间视图"><button v-for="view in modes" :key="view.key" :class="{ selected: mode === view.key }" :aria-current="mode === view.key ? 'page' : undefined" @click="mode = view.key">{{ view.label }}</button></nav>
      <ElSelect v-model="subjectId" clearable placeholder="全部科目" aria-label="筛选科目" class="subject-filter"><ElOption v-for="subject in filterSubjects" :key="subject.id" :label="subject.name" :value="subject.id" /></ElSelect>
    </div>
    <ElAlert v-if="error || (online && catalog.error)" :title="error || catalog.error?.message || ''" type="error" :closable="false"><ElButton text @click="if (online) catalog.load(true); refresh()">重新加载</ElButton></ElAlert>
    <div class="plan-view-heading">
      <div><h2>{{ viewHeading }}</h2><p v-if="mode === 'overall'">按月份展开，随时调整未来的安排。</p><p v-else-if="mode === 'month'">{{ currentMonth?.scheduledDays ?? 0 }} 天已安排 · {{ currentMonth?.taskCount ?? 0 }} 个学习任务</p><p v-else-if="mode === 'week'">这一周的日安排，集中在这里。</p><p v-else>安排与调整在这里，学习记录留在今日。</p></div>
      <div v-if="mode !== 'overall'" class="date-navigation"><ElButton aria-label="上一个时间段" :disabled="writing" @click="move(-1)">‹</ElButton><ElDatePicker :model-value="selected" :type="mode === 'month' ? 'month' : 'date'" value-format="YYYY-MM-DD" :clearable="false" :editable="false" :disabled="writing" @update:model-value="value => { if (typeof value === 'string') selected = value }" /><ElButton aria-label="下一个时间段" :disabled="writing" @click="move(1)">›</ElButton></div>
    </div>
    <ElAlert v-if="!online && mode === 'week' && offline.week?.schedule && !error" :title="'本周只读快照 · 同步于 ' + formatDateTime(offline.week.schedule.syncedAt)" type="info" :closable="false" />
    <div v-if="loading" class="plan-loading" role="status">正在读取计划…</div>
    <div v-else-if="!online && mode !== 'week'" class="plan-empty"><h3>离线查看本周计划</h3><p>今日、本周与进度概览支持只读查看。</p><ElButton @click="selected = todayIso(); mode = 'week'">查看本周</ElButton></div>
    <template v-else-if="mode === 'overall'">
      <div v-if="data.imports.length && !subjectId" class="plan-intentions"><article v-for="plan in data.imports" :key="plan.planKey"><span>整体目标</span><strong>{{ plan.title }}</strong><small>{{ plan.startDate }} — {{ plan.endDate }}</small></article></div>
      <div v-if="data.months.length" class="month-timeline">
        <article v-for="month in data.months" :key="month.month" class="timeline-row">
          <div class="timeline-date"><small>{{ month.month.slice(0,4) }}</small><strong>{{ Number(month.month.slice(5)) }}<span>月</span></strong><i /></div>
          <div class="timeline-card">
            <div class="timeline-card-heading"><div><h3>{{ monthLabel(month.month) }}</h3><p>{{ month.scheduledDays }} 天已安排 · {{ month.taskCount }} 个任务</p></div><ElButton text @click="enterMonth(month.month)">查看月计划 ↗</ElButton></div>
            <div class="timeline-goals"><span v-for="goal in monthGoals(month.month)" :key="goal.id">{{ subjectName(goal.subjectId) }} · {{ goal.title }}</span><span v-for="outline in data.outlines.filter(o => o.periodType === 'MONTH' && o.startDate.slice(0,7) === month.month && !subjectId)" :key="outline.title">{{ outline.title }}</span></div>
            <div class="month-amounts"><div v-for="amount in month.amounts" :key="amount.subjectId + '-' + amount.unitId"><span>{{ subjectName(amount.subjectId) }}</span><strong>{{ amount.plannedAmount }}<small>{{ amount.unitName }}</small></strong><span>已学 {{ amount.completedAmount }} {{ amount.unitName }}</span></div></div>
            <p v-if="!month.taskCount" class="quiet">这个月暂未安排学习任务，可以进入月历添加。</p>
          </div>
        </article>
      </div>
      <div v-else class="plan-empty"><span class="empty-symbol">↗</span><h3>从第一天开始，建立你的计划</h3><p>直接添加日安排，或导入已经讨论好的完整方案。</p><ElButton type="primary" :disabled="!online" @click="openEditor()">添加第一项安排</ElButton><ElButton :disabled="!online" @click="importOpen = true">导入方案</ElButton></div>
    </template>
    <template v-else-if="mode === 'month' || mode === 'week'">
      <div v-if="currentOutlines.length && !subjectId" class="period-outlines"><article v-for="outline in currentOutlines" :key="outline.title"><strong>{{ outline.title }}</strong><p>{{ outline.description }}</p></article></div>
      <div :class="['plan-calendar', { 'week-calendar': mode === 'week' }]">
        <div class="calendar-weekdays"><span v-for="weekday in ['周一','周二','周三','周四','周五','周六','周日']" :key="weekday">{{ weekday }}</span></div>
        <div class="calendar-grid">
          <article v-for="date in cells" :key="date" class="calendar-cell" :class="{ outside: mode === 'month' && date.slice(0,7) !== selected.slice(0,7), today: date === todayIso() }">
            <div class="cell-heading"><button :aria-label="'查看 ' + date + ' 日计划'" @click="enterDay(date)"><span class="cell-weekday">{{ dateLabel(date) }}</span><span class="cell-day">{{ Number(date.slice(8)) }}</span><small v-if="date === todayIso()">今天</small></button><button class="cell-add" :disabled="!online" :aria-label="'在 ' + date + ' 添加安排'" @click="openEditor(date)">＋</button></div>
            <button v-if="mode === 'month' && entries(date).length" class="mobile-day-count" :aria-label="date + '，' + entries(date).length + ' 项安排'" @click="enterDay(date)">{{ entries(date).length }}项</button>
            <button v-for="entry in entries(date).slice(0, mode === 'week' ? 6 : 2)" :key="entry.taskId" class="calendar-task" @click="enterDay(date)"><span class="task-subject">{{ subjectName(entry.subjectId) }}</span><strong>{{ entry.taskTitle }}</strong><small>{{ entry.plannedAmount }} {{ entry.unitName }}</small></button>
            <button v-if="entries(date).length > (mode === 'week' ? 6 : 2)" class="cell-more" @click="enterDay(date)">查看全部 {{ entries(date).length }} 项</button><p v-if="!entries(date).length" class="cell-free">未安排</p>
          </article>
        </div>
      </div>
    </template>
    <template v-else>
      <div v-if="dayLoading" class="plan-loading" role="status">正在读取当天安排…</div>
      <div v-else-if="visibleDayItems.length" class="daily-schedule">
        <article v-for="(item,index) in visibleDayItems" :key="item.id" class="daily-schedule-item">
          <span class="daily-number">{{ String(index+1).padStart(2,'0') }}</span>
          <div class="daily-content"><div class="daily-tags"><span>{{ subjectName(item.subjectId) }}</span><small>{{ planSourceLabel(item.source) }}</small></div><h3>{{ item.taskTitle }}</h3><p>计划 {{ item.plannedAmount }} {{ item.unitName }}<span>当日该任务已学 {{ item.completedAmount ?? 0 }} {{ item.unitName }}</span><span v-if="item.estimatedMinutes != null">预计 {{ item.estimatedMinutes }} 分钟</span></p></div>
          <div class="daily-controls"><ElButton text :aria-label="'向上移动 ' + item.taskTitle" :disabled="index === 0 || writing || !online" @click="moveItem(item,-1)">↑</ElButton><ElButton text :aria-label="'向下移动 ' + item.taskTitle" :disabled="index === visibleDayItems.length - 1 || writing || !online" @click="moveItem(item,1)">↓</ElButton><ElButton text :disabled="writing || !online" @click="openEditor(selected,item)">编辑</ElButton><ElButton text type="danger" :disabled="writing || !online" @click="remove(item)">移除</ElButton></div>
        </article>
      </div>
      <div v-else class="plan-empty compact"><h3>这一天还没有学习安排</h3><p>写下准备学习的内容，让下一步更明确。</p><ElButton :disabled="!online" @click="openEditor()">＋ 添加安排</ElButton></div>
<p class="day-footnote">完成量来自学习记录。<RouterLink to="/">前往今日记录进度 ↗</RouterLink></p>
    </template>
    <footer class="plan-footer"><span>按日安排，逐层看清全貌</span><div><ElButton text @click="library = 'tasks'">任务库</ElButton><ElButton text @click="library = 'goals'">阶段目标</ElButton><ElButton text :disabled="loading" @click="refresh">刷新</ElButton></div></footer>
    <DayScheduleEditor v-model="editorOpen" :date="editDate" :item="editItem" :tasks="tasks" @saved="saved" />
    <PlanImportDialog v-model="importOpen" @imported="refresh" />
    <ElDialog :model-value="library !== null" :title="library === 'tasks' ? '任务库' : '阶段目标'" width="min(1100px, calc(100vw - 24px))" @update:model-value="library = null" @closed="refresh"><TaskPanel v-if="library === 'tasks'" /><StageGoalPanel v-if="library === 'goals'" /></ElDialog>
  </div>
</template>
<style scoped src="../workspace.css"></style>
