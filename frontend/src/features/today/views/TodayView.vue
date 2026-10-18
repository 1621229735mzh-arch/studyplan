<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElAlert, ElButton, ElDatePicker, ElDialog, ElInput, ElInputNumber, ElMessage, ElRadioGroup, ElRadioButton } from 'element-plus'
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
import type { LearningRecordFormSubmission } from '@/features/learning/types'
import type { MasteryLevel } from '@/types/domain'
import { fetchToday, saveLearningProgress, saveReviewProgress, saveDayConfirmation } from '../api'
import ChecklistTask from '../components/ChecklistTask.vue'
import ContinuationBook from '../components/ContinuationBook.vue'
import type { TodayChecklistTask, TodayResponse } from '../types'
const session = useSessionStore()
const catalog = useCatalogStore()
const offline = useOfflineSnapshotStore()
const { online } = useOnlineStatus()
const date = ref(todayIso())
const today = ref<TodayResponse | null>(null)
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const dataFromCache = ref(false)
const readonly = computed(() => !online.value || dataFromCache.value || loading.value)
const lastSyncText = computed(() => offline.syncedAt ? formatDateTime(offline.syncedAt) : '尚未同步')
let loadSequence = 0
async function loadFromCache(): Promise<boolean> {
  await offline.load()
  dataFromCache.value = true
  if (date.value !== todayIso() || offline.today?.date !== date.value) {
    today.value = null
    return false
  }
  today.value = offline.today
  return true
}

async function loadToday(): Promise<void> {
  const requestId = ++loadSequence
  const requestedDate = date.value
  errorMessage.value = null
  if (!online.value) {
    const cached = await loadFromCache()
    if (!cached) {
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
      await offline.save({ userName: session.username, today: response })
    }
  } catch (caught) {
    if (requestId !== loadSequence) return
    const apiError = toApiError(caught)
    const cached = await loadFromCache()
    if (!cached) {
      errorMessage.value = apiError.message
    }
  } finally {
    if (requestId === loadSequence) loading.value = false
  }
}


function onDateChange(value: unknown): void {
  date.value = typeof value === 'string' && value ? value : todayIso()
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
const selected = ref<TodayChecklistTask | null>(null)
const numberedAction = computed(() => selected.value?.sequence?.mode === 'SELECT_BY_NUMBER')
const problemNumber = ref<number | undefined>()
const countedAction = computed(() => selected.value?.activity?.mode === 'COUNT_STUDY' || selected.value?.activity?.mode === 'COUNT_REVIEW')
const timedAction = computed(() => !!selected.value?.activity && !selected.value.continuation)
const dialogVisible = ref(false)
const isComplete = ref(false)
const percent = ref<number | undefined>(0)
const actualAmount = ref<number | undefined>()
const activityNote = ref('')
const mastery = ref<MasteryLevel | undefined>()
const saving = ref(false)
const saveError = ref<string | null>(null)
let actionDate = ''
let attempt: { signature: string; token: string } | null = null
let confirmationAttempt: {signature: string; token: string} | null = null
async function confirmTask(task: TodayChecklistTask, completed: boolean): Promise<void> {
  if (readonly.value || saving.value || !task.actionable) return
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
  if (task.activity?.mode==='CHECK_ONLY') { void confirmTask(task,complete); return }
  selected.value = { ...task }
  actionDate = date.value
  isComplete.value = complete
  percent.value = complete ? 100 : Math.ceil(task.completionPercent)
  actualAmount.value = undefined
  problemNumber.value = task.sequence?.mode === 'SELECT_BY_NUMBER' && task.sequence.dayItem ? Number(task.sequence.dayItem.key) : undefined
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
  if (percent.value == null || percent.value < task.completionPercent || percent.value > 100) {
    saveError.value = '请填写不低于当前进度、且不超过 100 的累计完成百分比。'; return
  }
  if (numberedAction.value && (!Number.isInteger(problemNumber.value) || problemNumber.value! < 1 || problemNumber.value! > 2147483647)) { saveError.value = '请填写题号（正整数）。'; return }
  if (countedAction.value && (!Number.isInteger(actualAmount.value) || actualAmount.value! <= 0 || actualAmount.value! > 99999999)) { saveError.value = '请填写本次实际词数（正整数），未学习无需提交。'; return }
  if (task.activity && actualAmount.value != null && actualAmount.value <= 0) { saveError.value = '新增完成量请填写正数，尚未完成可留空。'; return }
  if (task.kind === 'REVIEW' && percent.value === 100 && !mastery.value) { saveError.value = '请选择本次复习的掌握情况。'; return }
  const signature = JSON.stringify([percent.value, mastery.value, actualAmount.value, activityNote.value, problemNumber.value])
  if (attempt?.signature !== signature) attempt = { signature, token: crypto.randomUUID() }
  const payload = { date: actionDate, completionPercent: percent.value, durationMinutes: null, clientToken: attempt!.token,
    ...(numberedAction.value ? {problemNumber: problemNumber.value} : {}),
    ...(task.activity && !task.continuation ? {actualAmount: task.activity.mode === 'RECAP' ? null : actualAmount.value ?? null, note: activityNote.value || null} : {}) }
  saving.value = true
  saveError.value = null
  try {
    if (task.kind === 'LEARNING') await saveLearningProgress(task.id, task.revision!, payload)
    else await saveReviewProgress(task.id, task.version!, mastery.value ?? null, payload)
    dialogVisible.value = false
    ElMessage.success(percent.value === 100 ? '已完成' : '进度已保存')
    await loadToday()
  } catch (caught) {
    const error = toApiError(caught)
    saveError.value = error.status === 409 ? '计划或进度已在其他设备变化。请关闭此窗口并刷新，再核对后提交。' : error.message
  } finally { saving.value = false }
}
const recordFormRef = ref<InstanceType<typeof RecordForm> | null>(null)
const recording = ref(false)
const extraRecordRef = ref<HTMLDetailsElement | null>(null)
async function openExtraRecord(): Promise<void> {
  if (!extraRecordRef.value || readonly.value) return
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
onMounted(() => { void catalog.load(); void loadToday() })
</script>
<template>
  <div class="page today-page">
    <header class="today-hero">
      <ArchiveArtwork variant="hero" class="today-hero__art" />
      <div class="today-hero__copy"><span class="today-eyebrow">STUDY ROOM</span><h1>今日学习</h1><p>按计划学习，按实际记录。</p>
      </div>
      <div class="today-hero__tools">
        <ElButton class="today-extra-button" :disabled="readonly || saving || !today?.sections" @click="openExtraRecord"><AppIcon name="edit" :size="20" />补记学习</ElButton>
        <div class="page__actions today-controls">
        <ElDatePicker :model-value="date" type="date" value-format="YYYY-MM-DD" :clearable="false" aria-label="查看日期" @update:model-value="onDateChange" />
        <ElButton :loading="loading" @click="loadToday"><AppIcon v-if="!loading" name="refresh" :size="15" />刷新</ElButton>
        </div>
      </div>
    </header>
    <ElAlert v-if="dataFromCache" type="info" :closable="false" show-icon
      :title="`只读快照 · 同步于 ${lastSyncText}`" description="联网后刷新即可记录进度。" />
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
          <ChecklistTask v-for="task in section.tasks.filter(t => !t.continuation && t.completionPercent < 100)" :key="`${task.kind}-${task.id}`"
            :task="task" :readonly="readonly || saving" :show-estimate="section.name === '数学一'" @action="openAction" />
          <ContinuationBook v-for="book in bookGroups(section.tasks)" :key="`book-${book.id}`" :title="book.title" :tasks="book.tasks" :readonly="readonly || saving" :show-estimate="section.name === '数学一'" @action="openAction" />
        </ul>
        <p v-if="!section.tasks.some(t => t.completionPercent < 100)" class="empty-subject">
          {{ section.tasks.length ? '今日任务已完成' : '今天暂无任务' }}
        </p>
        <details v-if="section.tasks.some(t => !t.continuation && t.completionPercent >= 100)" class="completed">
          <summary>已完成 · {{ section.tasks.filter(t => !t.continuation && t.completionPercent >= 100).length }}</summary>
          <ul class="checklist"><ChecklistTask v-for="task in section.tasks.filter(t => !t.continuation && t.completionPercent >= 100)" :key="`${task.kind}-${task.id}`" :task="task" :readonly="readonly || saving" @action="openAction" /></ul>
        </details>
      </section>
      <details ref="extraRecordRef" class="section-card extra-record"><summary>补记学习 / 计划外记录</summary>
        <fieldset :disabled="readonly" class="extra-record-fields"><RecordForm ref="recordFormRef" :tasks="taskOptions" :submitting="recording" @submit="saveExtraRecord" /></fieldset>
      </details>
    </template>
    <ElDialog v-model="dialogVisible" :title="countedAction ? '记录实际数量' : timedAction ? (isComplete ? '结束本次安排' : '记录本次学习') : isComplete ? '完成任务' : selected?.sequence ? '记录本次尝试' : '记录剩余进度'" width="min(440px, 92vw)"
      :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving"
      :before-close="(done: () => void) => { if (!saving) done() }">
      <p class="dialog-task">{{ selected?.title }}</p>
      <p v-if="timedAction && !countedAction && selected?.activity" class="section-note">结束本次安排只表示今天收工，不表示整讲或整章已经学完。{{ selected.activity.mode === 'RECAP' ? '复做与订正不增加首次完成量。' : '只填写本次首次完成的数量；已做过的题不重复计数。' }}</p>
      <template v-if="numberedAction"><label class="field-label">LeetCode 题号</label><ElInputNumber v-model="problemNumber" :min="1" :max="2147483647" :precision="0" :controls="false" :disabled="saving" aria-label="LeetCode题号" /><p class="section-note">填写本次实际做的题号，无需按顺序。完成整题记 1 题，尝试不增加完成量。</p></template>
      <p v-if="selected?.sequence?.dayItem && !numberedAction">{{ selected.sequence.dayItem.title }} · {{ isComplete ? '完成整题后推进' : '记录本次尝试，题目继续保留' }}</p>
      <label v-if="!isComplete && !selected?.sequence && !timedAction" class="field-label">累计已完成 (%)</label>
      <ElInputNumber v-if="!isComplete && !selected?.sequence && !timedAction" v-model="percent" :min="Math.ceil(selected?.completionPercent ?? 0)" :max="100" :precision="0" :disabled="saving" aria-label="累计已完成百分比" />
      <p v-if="!isComplete && !selected?.sequence && !timedAction" class="section-note">填写 30 表示已完成 30%，剩余 70%。</p>
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
      </template>
      <ElAlert v-if="saveError" class="save-error" type="error" :closable="false" :title="saveError" />
      <template #footer><ElButton :disabled="saving" @click="dialogVisible = false">取消</ElButton><ElButton type="primary" :loading="saving" :disabled="readonly" @click="saveAction">保存</ElButton></template>
    </ElDialog>
  </div>
</template>
<style scoped src="@/styles/today.css"></style>
