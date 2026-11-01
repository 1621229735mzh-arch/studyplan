<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ElAlert, ElButton, ElDatePicker, ElDialog, ElMessage, ElMessageBox, ElOption, ElSelect } from 'element-plus'
import { toApiError } from '@/api/http'
import { masteryLabel, todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import { addDays, dateLabel, monthDates, moveMonth, weekDates, weekStart, type PlanViewMode } from '@/features/plan/workspace'
import { listReviewItems } from '../api'
import { getReviewWorkspace, scheduleReview } from '../api/workspace'
import type { ReviewItem } from '../types'
import type { ReviewEntry, ReviewWorkspace } from '../workspace'
import ReviewItemPanel from '../components/ReviewItemPanel.vue'
import SuggestionPanel from '../components/SuggestionPanel.vue'
import ReviewScheduleDialog from '../components/ReviewScheduleDialog.vue'
import ReviewTotals from '../components/ReviewTotals.vue'
import AutoReviewBooks from '../components/AutoReviewBooks.vue'

const route = useRoute(); const catalog = useCatalogStore(); const { online } = useOnlineStatus()
const mode = ref<PlanViewMode>('overall')
const modes: { key: PlanViewMode; label: string }[] = [{ key: 'overall', label: '整体' }, { key: 'month', label: '月' }, { key: 'week', label: '周' }, { key: 'day', label: '日' }]
const selected = ref(todayIso()); const subjectId = ref<number>()
const data = ref<ReviewWorkspace | null>(null); const items = ref<ReviewItem[]>([])
const loading = ref(false); const writing = ref(false); const error = ref('')
const library = ref<'items' | 'suggestions' | null>(null)
const editorOpen = ref(false); const editDate = ref(todayIso()); const editItemId = ref<number>()
let request = 0
const cells = computed(() => mode.value === 'week' ? weekDates(selected.value) : monthDates(selected.value))
const entriesByDate = computed(() => {
  const result = new Map<string, ReviewEntry[]>()
  for (const entry of data.value?.entries ?? []) {
    const list = result.get(entry.date) ?? []; list.push(entry); result.set(entry.date, list)
  }
  return result
})
const currentMonth = computed(() => data.value?.months.find(m => m.month === selected.value.slice(0, 7)))
const viewHeading = computed(() => mode.value === 'overall' ? '你的复习路线' : mode.value === 'month' ? monthLabel(selected.value.slice(0, 7))
  : mode.value === 'week' ? weekStart(selected.value) + ' — ' + addDays(weekStart(selected.value), 6) : dateLabel(selected.value))
function monthLabel(month: string) { return month.slice(0, 4) + ' 年 ' + Number(month.slice(5, 7)) + ' 月' }
function entries(date: string) { return entriesByDate.value.get(date) ?? [] }
function subjectName(id: number) { return catalog.subjectNameOf(id) ?? '未分类' }
function status(entry: ReviewEntry) { return entry.result ? `已完成 · ${masteryLabel(entry.result)}` : entry.completionPercent > 0 ? `已复习 ${entry.completionPercent}%` : '待复习' }
async function load() {
  const id = ++request; error.value = ''; data.value = null; items.value = []; loading.value = false
  if (!online.value) return
  loading.value = true
  try {
    const [workspace, list] = await Promise.all([getReviewWorkspace(subjectId.value), listReviewItems({ subjectId: subjectId.value })])
    if (id !== request) return
    data.value = workspace; items.value = list
  } catch (caught) { if (id === request) error.value = toApiError(caught).message }
  finally { if (id === request) loading.value = false }
}
function enterMonth(month: string) { selected.value = month + '-01'; mode.value = 'month' }
function enterDay(date: string) { selected.value = date; mode.value = 'day' }
function move(direction: number) { selected.value = mode.value === 'month' ? moveMonth(selected.value, direction) : addDays(selected.value, direction * (mode.value === 'week' ? 7 : 1)) }
function openEditor(date = selected.value, itemId?: number) { editDate.value = date; editItemId.value = itemId; editorOpen.value = true }
async function saved(date: string) { selected.value = date; await load(); ElMessage.success('复习安排已保存') }
async function remove(entry: ReviewEntry) {
  if (writing.value || !online.value) return
  try { await ElMessageBox.confirm(`移除 ${entry.date} 的“${entry.title}”复习安排？已有复习记录会保留。`, '移除复习安排', { confirmButtonText: '移除', cancelButtonText: '取消' }) } catch { return }
  if (writing.value || !online.value) return
  writing.value = true; error.value = ''
  try { await scheduleReview(entry.reviewItemId, null, entry.version); await load(); ElMessage.success('已移除安排，历史记录保留') }
  catch (caught) { error.value = toApiError(caught).message }
  finally { writing.value = false }
}
watch(() => route.query.subject, value => { const id = Number(value); subjectId.value = Number.isSafeInteger(id) && id > 0 ? id : undefined }, { immediate: true })
watch([subjectId, online], () => void load())
watch(online, value => { if (value) void catalog.load() })
onMounted(() => { if (online.value) void catalog.load(); void load() })
onBeforeUnmount(() => { request++ })
</script>

<template>
  <div class="plan-workspace review-workspace" :aria-busy="loading">
    <header class="plan-header">
      <div><span class="plan-eyebrow">把学过的内容，慢慢巩固</span><h1>复习计划</h1><p>沿着整体、月、周、日安排复习，在今日完成并留下记录。</p></div>
      <div class="plan-actions"><ElButton :disabled="!online || loading || writing" @click="library = 'items'">复习内容库</ElButton><ElButton type="primary" :disabled="!online || loading || writing || !data" @click="openEditor()">＋ 添加安排</ElButton></div>
    </header>
    <div class="plan-navigation">
      <nav aria-label="复习时间视图"><button v-for="view in modes" :key="view.key" :class="{ selected: mode === view.key }" :aria-current="mode === view.key ? 'page' : undefined" @click="mode = view.key">{{ view.label }}</button></nav>
      <ElSelect v-model="subjectId" clearable placeholder="全部科目" aria-label="筛选复习科目" class="subject-filter" :disabled="writing"><ElOption v-for="subject in catalog.subjects" :key="subject.id" :label="subject.name" :value="subject.id" /></ElSelect>
    </div>
    <AutoReviewBooks :online="online" :subject-id="subjectId" :workspace="data" @changed="load" @schedule="openEditor" />
    <ElAlert v-if="error || catalog.error" :title="error || catalog.error?.message || ''" type="error" :closable="false"><ElButton text :disabled="loading || !online" @click="catalog.load(true); load()">重新加载</ElButton></ElAlert>
    <div class="plan-view-heading">
      <div><h2>{{ viewHeading }}</h2><p v-if="mode === 'overall'">按月份展开，查看已安排的复习与实际复习痕迹。</p><p v-else-if="mode === 'month'">{{ currentMonth?.scheduledDays ?? 0 }} 天有复习 · {{ currentMonth?.entryCount ?? 0 }} 项 · 已完成 {{ currentMonth?.completedCount ?? 0 }} 项</p><p v-else-if="mode === 'week'">这一周的复习安排，集中在这里。</p><p v-else>安排与调整在这里，完成反馈留在今日。</p></div>
      <div v-if="mode !== 'overall'" class="date-navigation"><ElButton aria-label="上一个复习时间段" @click="move(-1)">‹</ElButton><ElDatePicker :model-value="selected" :type="mode === 'month' ? 'month' : 'date'" value-format="YYYY-MM-DD" :clearable="false" :editable="false" @update:model-value="value => { if (typeof value === 'string') selected = value }" /><ElButton aria-label="下一个复习时间段" @click="move(1)">›</ElButton></div>
    </div>
    <div v-if="!online" class="plan-empty"><h3>联网后查看复习计划</h3><p>离线可在今日查看已同步的复习任务，完整复习安排与历史记录需联网。</p><RouterLink to="/">查看今日快照 ↗</RouterLink></div>
    <div v-else-if="loading" class="plan-loading" role="status">正在读取复习计划…</div>
    <template v-else-if="data">
      <template v-if="mode === 'overall'">
        <ReviewTotals :totals="data.totals" />
        <div v-if="data.months.length" class="month-timeline">
          <article v-for="month in data.months" :key="month.month" class="timeline-row">
            <div class="timeline-date"><small>{{ month.month.slice(0,4) }}</small><strong>{{ Number(month.month.slice(5)) }}<span>月</span></strong><i /></div>
            <div class="timeline-card"><div class="timeline-card-heading"><div><h3>{{ monthLabel(month.month) }}</h3><p>{{ month.scheduledDays }} 天有复习 · {{ month.entryCount }} 项复习</p></div><ElButton text @click="enterMonth(month.month)">查看月计划 ↗</ElButton></div><div class="month-amounts"><div><span>复习安排与记录</span><strong>{{ month.completedCount }}<small>/ {{ month.entryCount }} 项已完成</small></strong><span>完成情况来自实际复习反馈</span></div></div></div>
          </article>
        </div>
        <div v-else class="plan-empty"><span class="empty-symbol">↗</span><h3>从第一次复习开始</h3><p>等确定内容后，再选择已学内容安排到具体日期。</p><ElButton :disabled="!online" @click="openEditor()">添加复习安排</ElButton></div>
      </template>
      <template v-else-if="mode === 'month' || mode === 'week'">
        <div :class="['plan-calendar', { 'week-calendar': mode === 'week' }]">
          <div class="calendar-weekdays"><span v-for="weekday in ['周一','周二','周三','周四','周五','周六','周日']" :key="weekday">{{ weekday }}</span></div>
          <div class="calendar-grid">
            <article v-for="date in cells" :key="date" class="calendar-cell" :class="{ outside: mode === 'month' && date.slice(0,7) !== selected.slice(0,7), today: date === todayIso() }">
              <div class="cell-heading"><button :aria-label="'查看 ' + date + ' 日复习'" @click="enterDay(date)"><span class="cell-weekday">{{ dateLabel(date) }}</span><span class="cell-day">{{ Number(date.slice(8)) }}</span><small v-if="date === todayIso()">今天</small></button><button class="cell-add" :disabled="writing" :aria-label="'在 ' + date + ' 添加复习'" @click="openEditor(date)">＋</button></div>
              <button v-if="mode === 'month' && entries(date).length" class="mobile-day-count" :aria-label="date + '，' + entries(date).length + ' 项复习'" @click="enterDay(date)">{{ entries(date).length }} 项</button>
              <button v-for="entry in entries(date).slice(0, mode === 'week' ? 6 : 2)" :key="entry.reviewItemId" class="calendar-task" @click="enterDay(date)"><span class="task-subject">{{ subjectName(entry.subjectId) }} · 复习</span><strong>{{ entry.title }}</strong><small v-if="entry.bookTitle">{{ entry.bookTitle }} · 第{{ entry.round }}次复习</small><small>{{ status(entry) }}</small></button>
              <button v-if="entries(date).length > (mode === 'week' ? 6 : 2)" class="cell-more" @click="enterDay(date)">查看全部 {{ entries(date).length }} 项</button><p v-if="!entries(date).length" class="cell-free">未安排</p>
            </article>
          </div>
        </div>
      </template>
      <template v-else>
        <div v-if="entries(selected).length" class="daily-schedule">
          <article v-for="(entry,index) in entries(selected)" :key="entry.reviewItemId" class="daily-schedule-item"><span class="daily-number">{{ String(index+1).padStart(2,'0') }}</span><div class="daily-content"><div class="daily-tags"><span>{{ subjectName(entry.subjectId) }}</span><small>复习</small></div><h3>{{ entry.title }}</h3><p v-if="entry.bookTitle">{{ entry.bookTitle }} · 学完于 {{ entry.learnedOn }} · 第{{ entry.round }}次复习</p><p>{{ status(entry) }}<span v-if="!entry.scheduled && !entry.result">历史复习记录 · 当前未安排在这一天</span></p></div><div v-if="entry.scheduled" class="daily-controls"><ElButton text :disabled="writing" @click="openEditor(selected, entry.reviewItemId)">调整日期</ElButton><ElButton text type="danger" :disabled="writing" @click="remove(entry)">移除</ElButton></div></article>
        </div>
        <div v-else class="plan-empty compact"><h3>这一天还没有复习安排</h3><p>确定内容后，可以从已有复习清单选择。</p><ElButton @click="openEditor()">＋ 添加安排</ElButton></div>
        <p class="day-footnote">复习独立记录，不增加首次学习量。<RouterLink to="/">前往今日 ↗</RouterLink></p>
      </template>
    </template>
    <footer class="plan-footer"><span>先安排，再复习，留下每次巩固的痕迹</span><div><ElButton text :disabled="!online" @click="library = 'suggestions'">待安排与日期建议</ElButton><RouterLink to="/learning">复习记录 ↗</RouterLink><ElButton text :disabled="loading || writing || !online" @click="load">刷新</ElButton></div></footer>
    <ReviewScheduleDialog v-model="editorOpen" :date="editDate" :items="items" :item-id="editItemId" @saved="saved" @library="library = 'items'" />
    <ElDialog :model-value="library !== null" :title="library === 'items' ? '复习内容库' : '待安排与日期建议'" width="min(1100px, calc(100vw - 24px))" destroy-on-close @update:model-value="library = null" @closed="load"><ReviewItemPanel v-if="library === 'items'" /><SuggestionPanel v-if="library === 'suggestions'" /></ElDialog>
  </div>
</template>
<style scoped src="@/features/plan/workspace.css"></style>
<style scoped>.plan-footer a { color:var(--kaoyan-primary); font-size:12px; text-decoration:none; margin:0 12px; }</style>
