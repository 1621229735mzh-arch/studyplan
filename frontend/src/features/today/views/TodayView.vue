<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElAlert, ElButton, ElDatePicker, ElDialog, ElInput, ElInputNumber, ElMessage, ElRadioGroup, ElRadioButton } from 'element-plus'
import { todayReadFailure } from '../readFailure'
import { toApiError } from '@/api/http'
import AppIcon from '@/components/AppIcon.vue'
import ArchiveArtwork from '@/components/ArchiveArtwork.vue'
import { formatDateTime, todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import { useSessionStore } from '@/stores/session'
import { useCatalogStore } from '@/stores/catalog'
import { createLearningRecord } from '@/features/learning/api'
import RecordForm from '@/features/learning/components/RecordForm.vue'
import LearningDurationInput from '@/features/learning/components/LearningDurationInput.vue'
import { canRecordMathDuration, durationError } from '@/features/learning/duration'
import type { LearningRecordFormSubmission } from '@/features/learning/types'
import type { MasteryLevel } from '@/types/domain'
import { fetchToday, saveLearningProgress, saveReviewProgress, saveDayConfirmation, saveCompletedProblemNumbers } from '../api'
import ChecklistTask from '../components/ChecklistTask.vue'
import ContinuationBook from '../components/ContinuationBook.vue'
import ReviewBook from '../components/ReviewBook.vue'
import type { TodayChecklistTask, TodayResponse } from '../types'
const session = useSessionStore()
const catalog = useCatalogStore()
const offline = useOfflineSnapshotStore()
const { online } = useOnlineStatus()
const date = ref(todayIso())
let lastStudyDay = date.value
let followsToday = true
function syncStudyDay(): boolean {
  const current = todayIso()
  if (current === lastStudyDay) return false
  lastStudyDay = current
  if (!followsToday) return false
  date.value = current
  today.value = null
  return true
}
const today = ref<TodayResponse | null>(null)
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const readFailure = ref<ReturnType<typeof todayReadFailure> | null>(null)
const dataFromCache = ref(false)
const readonly = computed(() => !online.value || dataFromCache.value || loading.value || !!readFailure.value)
const lastSyncText = computed(() => offline.syncedAt ? formatDateTime(offline.syncedAt) : '尚未同步')
let loadSequence = 0
async function loadFromCache(requestId: number, requestedDate: string): Promise<boolean> {
  try { await offline.load() }
  catch {
    if (requestId === loadSequence) { dataFromCache.value = true; today.value = null }
    return false
  }
  if (requestId !== loadSequence || requestedDate !== date.value) return false
  dataFromCache.value = true
  if (date.value !== todayIso() || offline.today?.date !== date.value) {
    today.value = null
    return false
  }
  today.value = offline.today
  return true
}

async function loadToday(): Promise<void> {
  syncStudyDay()
  const requestId = ++loadSequence
  const requestedDate = date.value
  errorMessage.value = null
  readFailure.value = null
  loading.value = false
  if (!online.value) {
    readFailure.value = todayReadFailure(null, false)
    const cached = await loadFromCache(requestId, requestedDate)
    if (requestId === loadSequence && !cached) {
      errorMessage.value = '离线状态且本地没有今天的数据快照，请联网后刷新。'
    }
    return
  }
  loading.value = true
  try {
    const response = await fetchToday(requestedDate)
    if (requestId !== loadSequence) return
    today.value = response
    dataFromCache.value = false
    if (session.username && response.date === todayIso()) {
      // 只缓存今日切片；其它数据不在离线范围内。
      await offline.save({ userName: session.username, today: response }).catch(() => {
        if (requestId === loadSequence) errorMessage.value = '今日数据已读取，但本地快照保存失败。下次离线可能无法查看。'
      })
    }
  } catch (caught) {
    if (requestId !== loadSequence) return
    const apiError = toApiError(caught)
    readFailure.value = todayReadFailure(apiError, online.value)
    if (apiError.status === 401 || apiError.code === 'UNAUTHORIZED' || apiError.status === 403) {
      today.value = null; dataFromCache.value = true
      return
    }
    const cached = await loadFromCache(requestId, requestedDate)
    if (requestId === loadSequence && !cached) errorMessage.value = '本地没有所选学习日的数据快照。'
  } finally {
    if (requestId === loadSequence) loading.value = false
  }
}


function onDateChange(value: unknown): void {
  date.value = typeof value === 'string' && value ? value : todayIso()
  followsToday = date.value === todayIso()
  today.value = null
  void loadToday()
}
function bookGroups(tasks: TodayChecklistTask[]) {
  const groups = new Map<number, {id: number; title: string; tasks: TodayChecklistTask[]}>()
  for (const task of tasks) if (task.continuation) {
    const {streamId,title} = task.continuation
    if (!groups.has(streamId)) groups.set(streamId,{id:streamId,title,tasks:[]})
    groups.get(streamId)!.tasks.push(task)
  }
  return [...groups.values()]
}
function reviewBookGroups(tasks: TodayChecklistTask[]) {
  const groups = new Map<number, { id: number; title: string; tasks: TodayChecklistTask[] }>()
  for (const task of tasks) if (task.automaticReview) {
    const { streamId, title } = task.automaticReview
    if (!groups.has(streamId)) groups.set(streamId, { id: streamId, title, tasks: [] })
    groups.get(streamId)!.tasks.push(task)
  }
  return [...groups.values()]
}
const selected = ref<TodayChecklistTask | null>(null)
const selectedSubjectName = ref('')
const mathDurationAction = computed(() => selected.value?.kind === 'LEARNING' && !numberedAction.value &&
  canRecordMathDuration(selectedSubjectName.value, selected.value?.activity?.mode))
const numberedAction = computed(() => selected.value?.sequence?.mode === 'SELECT_BY_NUMBER')
const problemNumbersText = ref('')
const countedAction = computed(() => selected.value?.activity?.mode === 'COUNT_STUDY' || selected.value?.activity?.mode === 'COUNT_REVIEW')
const timedAction = computed(() => !!selected.value?.activity && !selected.value.continuation)
const dialogVisible = ref(false)
const isComplete = ref(false)
const progressMode = ref<'page' | 'percent'>('percent')
const completedPage = ref<number | undefined>()
const pageRange = computed(() => selected.value?.kind === 'LEARNING' ? selected.value.continuation?.pageRange : null)
const pageAction = computed(() => !isComplete.value && !!pageRange.value && progressMode.value === 'page')
const percent = ref<number | undefined>(0)
const actualAmount = ref<number | undefined>()
const durationMinutes = ref<number | null>(null)
const activityNote = ref('')
const mastery = ref<MasteryLevel | undefined>()
const saving = ref(false)
const saveError = ref<string | null>(null)
let actionDate = ''
let attempt: { signature: string; token: string } | null = null
let confirmationAttempt: {signature: string; token: string} | null = null
async function confirmTask(task: TodayChecklistTask, completed: boolean): Promise<void> {
  if (readonly.value || saving.value || !task.actionable) return
  if (syncStudyDay()) { void loadToday(); return }
  const signature=JSON.stringify([date.value,task.id,completed,task.revision])
  if (confirmationAttempt?.signature!==signature) confirmationAttempt={signature,token:crypto.randomUUID()}
  saving.value=true; errorMessage.value=null
  try {
    await saveDayConfirmation(task.id,date.value,completed,task.revision!,confirmationAttempt.token)
    ElMessage.success(completed ? '已确认完成' : '已取消完成')
    await loadToday()
  } catch (caught) { errorMessage.value=toApiError(caught).message }
  finally { saving.value=false }
}
function openAction(task: TodayChecklistTask, complete: boolean): void {
  if (readonly.value || !task.actionable) return
  if (syncStudyDay()) { void loadToday(); return }
  if (task.activity?.mode==='CHECK_ONLY') { void confirmTask(task,complete); return }
  selected.value = { ...task }
  selectedSubjectName.value = today.value?.sections?.find(s => s.tasks.some(t => t.kind === task.kind && t.id === task.id))?.name ?? ''
  actionDate = date.value
  isComplete.value = complete
  progressMode.value = task.kind === 'LEARNING' && task.continuation?.pageRange ? 'page' : 'percent'
  completedPage.value = undefined
  percent.value = complete ? 100 : task.continuation ? task.completionPercent : Math.ceil(task.completionPercent)
  actualAmount.value = undefined
  durationMinutes.value = null
  problemNumbersText.value = ''
  activityNote.value = ''
  mastery.value = undefined
  saveError.value = null
  attempt = null
  dialogVisible.value = true
}
async function saveAction(): Promise<void> {
  const task = selected.value
  if (!task || saving.value) return
  if (readonly.value) { saveError.value = '请联网并刷新后再保存。'; return }
  const timeError = mathDurationAction.value ? durationError(durationMinutes.value) : null
  if (timeError) { saveError.value = timeError; return }
  if (pageAction.value && (!Number.isInteger(completedPage.value) || completedPage.value! < pageRange.value!.startPage || completedPage.value! > pageRange.value!.endPage)) {
    saveError.value = '请填写本段印刷页码范围内已学完的最后一页。'; return
  }
  if (!pageAction.value && !numberedAction.value && (percent.value == null || percent.value < task.completionPercent || percent.value > 100)) {
    saveError.value = '请填写不低于当前进度、且不超过 100 的累计完成百分比。'; return
  }
  const parts = problemNumbersText.value.trim().split(/[\s,，、;；]+/)
  const problemNumbers = [...new Set(parts.map(Number))]
  if (numberedAction.value && (parts.some(p => !/^\d+$/.test(p)) || problemNumbers.some(n => !Number.isInteger(n) || n < 1 || n > 2147483647))) {
    saveError.value = '请填写完成题号（正整数），用空格、逗号或换行分隔。'; return
  }
  if (numberedAction.value && problemNumbers.length > 200) { saveError.value = '每次最多提交200题，可分次填写。'; return }
  if (countedAction.value && (!Number.isInteger(actualAmount.value) || actualAmount.value! <= 0 || actualAmount.value! > 99999999)) { saveError.value = '请填写本次实际词数（正整数），未学习无需提交。'; return }
  if (task.activity && actualAmount.value != null && actualAmount.value <= 0) { saveError.value = '新增完成量请填写正数，尚未完成可留空。'; return }
  if (task.kind === 'REVIEW' && percent.value === 100 && !mastery.value) { saveError.value = '请选择本次复习的掌握情况。'; return }
  const recordedMinutes = mathDurationAction.value ? durationMinutes.value : null
  const progressPercent = pageAction.value ? null : percent.value!
  const page = pageAction.value ? completedPage.value : undefined
  const signature = JSON.stringify([progressPercent, page, mastery.value, actualAmount.value, activityNote.value, numberedAction.value ? problemNumbers : null, recordedMinutes])
  if (attempt?.signature !== signature) attempt = { signature, token: crypto.randomUUID() }
  const payload = { date: actionDate, completionPercent: progressPercent, ...(pageAction.value ? { completedPage: page } : {}), durationMinutes: recordedMinutes, clientToken: attempt!.token,
    ...(task.activity && !task.continuation ? {actualAmount: task.activity.mode === 'RECAP' ? null : actualAmount.value ?? null, note: activityNote.value || null} : {}) }
  saving.value = true
  saveError.value = null
  try {
    if (numberedAction.value) await saveCompletedProblemNumbers(task.id, actionDate, problemNumbers, task.revision!, attempt!.token)
    else if (task.kind === 'LEARNING') await saveLearningProgress(task.id, task.revision!, payload)
    else await saveReviewProgress(task.id, task.version!, mastery.value ?? null, payload)
    dialogVisible.value = false
    ElMessage.success(numberedAction.value ? `已记录 ${problemNumbers.length} 题` : pageAction.value ? `已记录学完至第 ${page} 页` : percent.value === 100 ? '已完成' : '进度已保存')
    await loadToday()
  } catch (caught) {
    const error = toApiError(caught)
    saveError.value = error.status === 409 && !numberedAction.value ? '计划或进度已在其他设备变化。请关闭此窗口并刷新，再核对后提交。' : error.message
  } finally { saving.value = false }
}
const recordFormRef = ref<InstanceType<typeof RecordForm> | null>(null)
const recording = ref(false)
const extraRecordRef = ref<HTMLDetailsElement | null>(null)
async function openExtraRecord(): Promise<void> {
  if (!extraRecordRef.value || readonly.value) return
  if (syncStudyDay()) { await loadToday(); return }
  extraRecordRef.value.open = true
  await nextTick()
  extraRecordRef.value.scrollIntoView?.({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' })
  extraRecordRef.value.querySelector<HTMLElement>('input, textarea, button')?.focus({ preventScroll: true })
}
function subjectIcon(name: string): string {
  return name.includes('数学') ? 'math' : name.includes('英语') ? 'english' : name.includes('政治') ? 'politics' : name === '408' ? 'computer' : 'review'
}
const taskOptions = computed(() => (today.value?.plannedTasks ?? []).filter(t=>t.activityMode!=='CHECK_ONLY' && t.activityMode!=='CONTINUED_STUDY').map(t => ({
  sequential: !!today.value?.sections?.flatMap(s => s.tasks).find(task => task.kind === 'LEARNING' && task.id === t.taskId)?.sequence,
  activityMode: t.activityMode, id: t.taskId, title: t.taskTitle, subjectId: t.subjectId ?? null, unitId: t.unitId ?? null,
  unitName: t.unitName, subjectName: catalog.subjectNameOf(t.subjectId)
})))
async function saveExtraRecord(payload: LearningRecordFormSubmission): Promise<void> {
  if (readonly.value) return
  recording.value = true
  try {
    await createLearningRecord({ ...payload.model, note: payload.model.note || null, clientToken: payload.clientToken })
    recordFormRef.value?.reset()
    ElMessage.success('学习记录已保存')
    await loadToday()
  } catch (caught) {
    const error = toApiError(caught)
    recordFormRef.value?.setServerError(error.message, error.fieldErrors)
  } finally { recording.value = false }
}
watch(() => online.value, value => { if (!value) readFailure.value = todayReadFailure(null, false) })
let dayTimer: ReturnType<typeof setInterval> | undefined
function checkStudyDay(): void {
  // 正在填写的记录保留原日期与输入；关闭后再切换今日。
  if (dialogVisible.value || saving.value || recording.value || extraRecordRef.value?.open) return
  if (syncStudyDay()) void loadToday()
}
onMounted(() => {
  void catalog.load(); void loadToday()
  dayTimer = setInterval(checkStudyDay, 1000)
  window.addEventListener('focus', checkStudyDay)
  document.addEventListener('visibilitychange', checkStudyDay)
})
onUnmounted(() => {
  loadSequence++
  clearInterval(dayTimer)
  window.removeEventListener('focus', checkStudyDay)
  document.removeEventListener('visibilitychange', checkStudyDay)
})
</script>
<template>
  <div class="page today-page">
    <header class="today-hero">
      <ArchiveArtwork variant="hero" class="today-hero__art" />
      <div class="today-hero__copy"><span class="today-eyebrow">STUDY ROOM</span><h1>今日学习</h1><p>学习日 {{ date }} · 次日早上 9 点截止（北京时间）。</p>
      </div>
      <div class="today-hero__tools">
        <ElButton class="today-extra-button" :disabled="readonly || saving || !today?.sections" @click="openExtraRecord"><AppIcon name="edit" :size="20" />补记学习</ElButton>
        <div class="page__actions today-controls">
        <ElDatePicker :model-value="date" type="date" value-format="YYYY-MM-DD" :clearable="false" aria-label="查看日期" @update:model-value="onDateChange" />
        <ElButton :loading="loading" @click="loadToday"><AppIcon v-if="!loading" name="refresh" :size="15" />刷新</ElButton>
        </div>
      </div>
    </header>
    <ElAlert v-if="readFailure" type="warning" :closable="false" show-icon :title="readFailure.title">
      <p>{{ readFailure.recovery }}</p>
      <p>{{ dataFromCache && today ? `只读快照 · 同步于 ${lastSyncText}` : '尚未读取最新数据，暂时不能记录。' }}</p>
      <ElButton :disabled="loading || !online" @click="loadToday">重新读取今日</ElButton>
    </ElAlert>
    <ElAlert v-else-if="dataFromCache" type="info" :closable="false" show-icon :title="`只读快照 · 同步于 ${lastSyncText}`" description="点击刷新，成功读取服务器数据后即可记录。" />
    <ElAlert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" />
    <ElAlert v-if="today && !today.sections" type="info" :closable="false" title="当前数据是旧版快照，请联网更新今日清单。" />
    <template v-if="today?.sections">
      <section v-if="today.taskCounts" class="today-summary" aria-label="今日任务概览">
        <div class="summary-metric summary-metric--done"><span class="metric-icon"><AppIcon name="check" :size="32" /></span><div><span>已完成</span><strong>{{ today.taskCounts.completed }} <small>项</small></strong></div></div>
        <div class="summary-metric summary-metric--active"><span class="metric-icon"><AppIcon name="partial" :size="30" /></span><div><span>进行中</span><strong>{{ today.taskCounts.inProgress }} <small>项</small></strong></div></div>
        <div class="summary-metric summary-metric--pending"><span class="metric-icon"><AppIcon name="pending" :size="30" /></span><div><span>还剩</span><strong>{{ today.taskCounts.remaining }} <small>项</small></strong></div></div>
      </section>
      <ElAlert v-else type="info" :closable="false" title="当前数据尚无任务项数统计，请联网刷新今日清单。" />
      <section v-for="section in today.sections" :key="section.name" class="section-card subject-section">
        <div class="subject-heading">
          <h2><AppIcon :name="subjectIcon(section.name)" :size="30" />{{ section.name }}</h2>
        </div>
        <ul class="checklist">
          <ChecklistTask v-for="task in section.tasks.filter(t => t.kind !== 'REVIEW' && !t.continuation && (t.completionPercent < 100 || t.sequence?.mode === 'SELECT_BY_NUMBER'))" :key="`${task.kind}-${task.id}`"
            :task="task" :readonly="readonly || saving" :show-estimate="section.name === '数学一'" @action="openAction" />
          <ContinuationBook v-for="book in bookGroups(section.tasks)" :key="`book-${book.id}`" :title="book.title" :tasks="book.tasks" :readonly="readonly || saving" :show-estimate="section.name === '数学一'" @action="openAction" />
          <li v-if="section.tasks.some(t => t.kind === 'REVIEW')" class="today-review-section">
            <h3>复习</h3>
            <ul class="checklist">
              <ChecklistTask v-for="task in section.tasks.filter(t => t.kind === 'REVIEW' && !t.automaticReview && t.completionPercent < 100)" :key="`review-${task.id}`" :task="task" :readonly="readonly || saving" @action="openAction" />
              <ReviewBook v-for="book in reviewBookGroups(section.tasks)" :key="`review-book-${book.id}`" :title="book.title" :tasks="book.tasks" :readonly="readonly || saving" @action="openAction" />
            </ul>
          </li>
        </ul>
        <p v-if="!section.tasks.some(t => t.completionPercent < 100 || t.sequence?.mode === 'SELECT_BY_NUMBER')" class="empty-subject">
          {{ section.tasks.length ? '今日任务已完成' : '今天暂无任务' }}
        </p>
        <details v-if="section.tasks.some(t => !t.continuation && !t.automaticReview && t.completionPercent >= 100 && t.sequence?.mode !== 'SELECT_BY_NUMBER')" class="completed">
          <summary>已完成 · {{ section.tasks.filter(t => !t.continuation && !t.automaticReview && t.completionPercent >= 100 && t.sequence?.mode !== 'SELECT_BY_NUMBER').length }}</summary>
          <ul class="checklist"><ChecklistTask v-for="task in section.tasks.filter(t => !t.continuation && !t.automaticReview && t.completionPercent >= 100 && t.sequence?.mode !== 'SELECT_BY_NUMBER')" :key="`${task.kind}-${task.id}`" :task="task" :readonly="readonly || saving" @action="openAction" /></ul>
        </details>
      </section>
      <details ref="extraRecordRef" class="section-card extra-record"><summary>补记学习 / 计划外记录</summary>
        <fieldset :disabled="readonly" class="extra-record-fields"><RecordForm ref="recordFormRef" :tasks="taskOptions" :submitting="recording" @submit="saveExtraRecord" /></fieldset>
      </details>
    </template>
    <ElDialog v-model="dialogVisible" :title="numberedAction ? '记录完成题号' : countedAction ? '记录实际数量' : timedAction ? (isComplete ? '结束本次安排' : '记录本次学习') : isComplete ? '完成任务' : selected?.sequence ? '记录本次尝试' : '记录剩余进度'" width="min(440px, 92vw)"
      :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving"
      :before-close="(done: () => void) => { if (!saving) done() }">
      <p class="dialog-task">{{ selected?.title }}</p>
      <p v-if="timedAction && !countedAction && selected?.activity" class="section-note">结束本次安排只表示今天收工，不表示整讲或整章已经学完。{{ selected.activity.mode === 'RECAP' ? '复做与订正不增加首次完成量。' : '只填写本次首次完成的数量；已做过的题不重复计数。' }}</p>
      <template v-if="numberedAction"><label class="field-label">LeetCode 完成题号</label><ElInput v-model="problemNumbersText" type="textarea" :rows="3" maxlength="4000" :disabled="saving" aria-label="LeetCode完成题号" placeholder="例如：31、32、49" /><p class="section-note">只填本次新完成的题号，可填多题，用空格、逗号、顿号或换行分隔。同一天可继续追加，已完成的题不重复计数。</p></template>
      <p v-if="selected?.sequence?.dayItem && !numberedAction">{{ selected.sequence.dayItem.title }} · {{ isComplete ? '完成整题后推进' : '记录本次尝试，题目继续保留' }}</p>
      <template v-if="!isComplete && pageRange">
        <ElRadioGroup v-model="progressMode" :disabled="saving" aria-label="进度填写方式"><ElRadioButton value="page">按页码</ElRadioButton><ElRadioButton value="percent">按百分比</ElRadioButton></ElRadioGroup>
        <p class="section-note">本段：印刷第 {{ pageRange.startPage }}—{{ pageRange.endPage }} 页。当前记录 {{ selected?.completionPercent }}%。</p>
      </template>
      <template v-if="pageAction">
        <label class="field-label">已学完到第几页</label>
        <ElInputNumber v-model="completedPage" :min="pageRange!.startPage" :max="pageRange!.endPage" :precision="0" :controls="false" :disabled="saving" aria-label="已学完到第几页" placeholder="填写印刷页码" />
        <p class="section-note">填写书上已学完的最后一页，不填 PDF 页码；包含该页属于本段的内容。系统自动换算进度，页内尚未学完可切换百分比。到达末页表示本段内容全部完成。</p>
      </template>
      <label v-if="!pageAction && !isComplete && !selected?.sequence && !timedAction" class="field-label">累计已完成 (%)</label>
      <ElInputNumber v-if="!pageAction && !isComplete && !selected?.sequence && !timedAction" v-model="percent" :min="selected?.continuation ? selected.completionPercent : Math.ceil(selected?.completionPercent ?? 0)" :max="100" :precision="selected?.continuation ? 2 : 0" :disabled="saving" aria-label="累计已完成百分比" />
      <p v-if="!pageAction && !isComplete && !selected?.sequence && !timedAction" class="section-note">填写 30 表示已完成 30%，剩余 70%。</p>
      <template v-if="mathDurationAction">
        <label class="field-label">本次用时（可选）</label>
        <LearningDurationInput v-model="durationMinutes" :disabled="saving || readonly" />
      </template>
      <template v-if="selected?.activity?.mode === 'TIMED_STUDY' && !countedAction"><label class="field-label">本次新增完成量（{{ selected.unitName }}，可留空）</label><ElInputNumber v-model="actualAmount" :min="0.000001" :precision="6" :controls="false" :disabled="saving" aria-label="本次新增完成量" /></template>
      <template v-if="countedAction">
        <label class="field-label">{{ selected?.activity?.mode === 'COUNT_REVIEW' ? '本次实际复习词数' : '本次实际背诵新词数' }}</label>
        <ElInputNumber v-model="actualAmount" :min="1" :max="99999999" :precision="0" :controls="false" :disabled="saving" aria-label="本次实际词数" />
        <p class="section-note">只填写本次完成的数量；同一天分次记录会累加。{{ selected?.activity?.mode === 'COUNT_REVIEW' ? '复习词数单独记录，不增加新词量。' : '不设置每日固定目标。' }}</p>
      </template>
      <template v-if="timedAction && !countedAction"><label class="field-label">进度位置与卡点</label><ElInput v-model="activityNote" type="textarea" :rows="3" maxlength="500" :disabled="saving" aria-label="进度位置与卡点" placeholder="教材页码、1000题最后题号、未完成内容及一个卡点" /></template>
      <template v-if="selected?.kind === 'REVIEW' && percent === 100">
        <label class="field-label">掌握情况</label>
        <ElRadioGroup v-model="mastery" :disabled="saving"><ElRadioButton value="FORGOT">不会</ElRadioButton><ElRadioButton value="VAGUE">模糊</ElRadioButton><ElRadioButton value="MASTERED">掌握</ElRadioButton></ElRadioGroup>
        <p class="section-note">先独立回忆或做题，再核对答案。不会或需要提示选“不会”；能独立完成但吃力选“模糊”；顺利完成选“掌握”。自动复习内容会据此安排下一次日期。</p>
      </template>
      <ElAlert v-if="saveError" class="save-error" type="error" :closable="false" :title="saveError" />
      <template #footer><ElButton :disabled="saving" @click="dialogVisible = false">取消</ElButton><ElButton type="primary" :loading="saving" :disabled="readonly" @click="saveAction">保存</ElButton></template>
    </ElDialog>
  </div>
</template>
<style scoped src="@/styles/today.css"></style>
