<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElAlert, ElButton, ElDatePicker, ElDialog, ElInputNumber, ElMessage, ElRadioGroup, ElRadioButton } from 'element-plus'
import { toApiError } from '@/api/http'
import { formatDateTime, todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import { useSessionStore } from '@/stores/session'
import { useCatalogStore } from '@/stores/catalog'
import { createLearningRecord } from '@/features/learning/api'
import RecordForm from '@/features/learning/components/RecordForm.vue'
import type { LearningRecordFormSubmission } from '@/features/learning/types'
import type { MasteryLevel } from '@/types/domain'
import { fetchToday, saveLearningProgress, saveReviewProgress } from '../api'
import ChecklistTask from '../components/ChecklistTask.vue'
import { compactMinutes } from '../time'
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
const selected = ref<TodayChecklistTask | null>(null)
const dialogVisible = ref(false)
const isComplete = ref(false)
const percent = ref<number | undefined>(0)
const duration = ref<number | undefined>()
const mastery = ref<MasteryLevel | undefined>()
const saving = ref(false)
const saveError = ref<string | null>(null)
let actionDate = ''
let attempt: { signature: string; token: string } | null = null
function openAction(task: TodayChecklistTask, complete: boolean): void {
  if (readonly.value || !task.actionable) return
  selected.value = { ...task }
  actionDate = date.value
  isComplete.value = complete
  percent.value = complete ? 100 : Math.ceil(task.completionPercent)
  duration.value = undefined
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
  if (duration.value == null || duration.value < 0) { saveError.value = '请填写本次学习用时（分钟）。'; return }
  if (task.kind === 'REVIEW' && percent.value === 100 && !mastery.value) { saveError.value = '请选择本次复习的掌握情况。'; return }
  const signature = JSON.stringify([percent.value, duration.value, mastery.value])
  if (attempt?.signature !== signature) attempt = { signature, token: crypto.randomUUID() }
  const payload = { date: actionDate, completionPercent: percent.value, durationMinutes: duration.value, clientToken: attempt!.token }
  saving.value = true
  saveError.value = null
  try {
    if (task.kind === 'LEARNING') await saveLearningProgress(task.id, task.revision!, payload)
    else await saveReviewProgress(task.id, task.version!, mastery.value ?? null, payload)
    dialogVisible.value = false
    ElMessage.success(percent.value === 100 ? '已完成，用时已保存' : '进度与用时已保存')
    await loadToday()
  } catch (caught) {
    const error = toApiError(caught)
    saveError.value = error.status === 409 ? '计划或进度已在其他设备变化。请关闭此窗口并刷新，再核对后提交。' : error.message
  } finally { saving.value = false }
}
const recordFormRef = ref<InstanceType<typeof RecordForm> | null>(null)
const recording = ref(false)
const taskOptions = computed(() => (today.value?.plannedTasks ?? []).map(t => ({
  id: t.taskId, title: t.taskTitle, subjectId: t.subjectId ?? null, unitId: t.unitId ?? null,
  unitName: t.unitName, subjectName: catalog.subjectNameOf(t.subjectId)
})))
async function saveExtraRecord(payload: LearningRecordFormSubmission): Promise<void> {
  if (readonly.value) return
  recording.value = true
  try {
    await createLearningRecord({ ...payload.model, note: payload.model.note || null, clientToken: payload.clientToken })
    recordFormRef.value?.reset()
    ElMessage.success('用时与学习记录已保存')
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
    <div class="page__header">
      <div class="page__actions">
        <ElDatePicker :model-value="date" type="date" value-format="YYYY-MM-DD" :clearable="false" aria-label="查看日期" @update:model-value="onDateChange" />
        <ElButton :loading="loading" @click="loadToday">刷新</ElButton>
      </div>
    </div>
    <ElAlert v-if="dataFromCache" type="info" :closable="false" show-icon
      :title="`只读快照 · 同步于 ${lastSyncText}`" description="联网后刷新即可记录进度。" />
    <ElAlert v-if="errorMessage" type="error" :closable="false" :title="errorMessage" />
    <ElAlert v-if="today && !today.sections" type="info" :closable="false" title="当前数据是旧版快照，请联网更新今日清单。" />
    <template v-if="today?.sections">
      <section class="today-summary" aria-label="今日时间概览">
        <div class="summary-metric"><span>已学习</span><strong>{{ compactMinutes(today.actualMinutes ?? 0) }}</strong>
          <small v-if="today.missingDurations">{{ today.missingDurations }} 条记录未填用时</small></div>
        <div class="summary-metric"><span>预计剩余</span><strong>{{ today.missingEstimates ? '至少 ' : '' }}{{ compactMinutes(today.remainingMinutes ?? 0) }}</strong>
          <small v-if="today.missingEstimates">{{ today.missingEstimates }} 项未填预计用时</small></div>
      </section>
      <section v-for="section in today.sections" :key="section.name" class="section-card subject-section">
        <div class="subject-heading">
          <h2>{{ section.name }}</h2>
          <div class="subject-times"><span>已学习 <b>{{ compactMinutes(section.actualMinutes) }}</b></span>
            <span>预计剩余 <b>{{ section.missingEstimates ? '至少 ' : '' }}{{ compactMinutes(section.remainingMinutes) }}</b></span></div>
        </div>
        <p v-if="section.missingEstimates" class="section-note">{{ section.missingEstimates }} 项预计用时待补充，可在计划的每日安排中填写。</p>
        <p v-if="section.missingDurations" class="section-note">{{ section.missingDurations }} 条学习记录尚未填写用时。</p>
        <ul class="checklist">
          <ChecklistTask v-for="task in section.tasks.filter(t => t.completionPercent < 100)" :key="`${task.kind}-${task.id}`"
            :task="task" :readonly="readonly" @action="openAction" />
        </ul>
        <p v-if="!section.tasks.some(t => t.completionPercent < 100)" class="empty-subject">
          {{ section.tasks.length ? '今日任务已完成' : '今天暂无任务' }}
        </p>
        <details v-if="section.tasks.some(t => t.completionPercent >= 100)" class="completed">
          <summary>已完成 · {{ section.tasks.filter(t => t.completionPercent >= 100).length }}</summary>
          <ul class="checklist"><ChecklistTask v-for="task in section.tasks.filter(t => t.completionPercent >= 100)" :key="`${task.kind}-${task.id}`" :task="task" readonly @action="openAction" /></ul>
        </details>
      </section>
      <details class="section-card extra-record"><summary>补记学习 / 计划外记录</summary>
        <fieldset :disabled="readonly" class="extra-record-fields"><RecordForm ref="recordFormRef" :tasks="taskOptions" :submitting="recording" @submit="saveExtraRecord" /></fieldset>
      </details>
    </template>
    <ElDialog v-model="dialogVisible" :title="isComplete ? '完成任务' : '记录剩余进度'" width="min(440px, 92vw)"
      :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving"
      :before-close="(done: () => void) => { if (!saving) done() }">
      <p class="dialog-task">{{ selected?.title }}</p>
      <label v-if="!isComplete" class="field-label">累计已完成 (%)</label>
      <ElInputNumber v-if="!isComplete" v-model="percent" :min="Math.ceil(selected?.completionPercent ?? 0)" :max="100" :precision="0" :disabled="saving" aria-label="累计已完成百分比" />
      <p v-if="!isComplete" class="section-note">填写 30 表示已完成 30%，剩余 70%。</p>
      <label class="field-label">本次用时（分钟）</label>
      <ElInputNumber v-model="duration" :min="0" :precision="0" :disabled="saving" placeholder="填写本次用时" aria-label="本次用时（分钟）" />
      <template v-if="selected?.kind === 'REVIEW' && percent === 100">
        <label class="field-label">掌握情况</label>
        <ElRadioGroup v-model="mastery" :disabled="saving"><ElRadioButton value="FORGOT">不会</ElRadioButton><ElRadioButton value="VAGUE">模糊</ElRadioButton><ElRadioButton value="MASTERED">掌握</ElRadioButton></ElRadioGroup>
      </template>
      <ElAlert v-if="saveError" class="save-error" type="error" :closable="false" :title="saveError" />
      <template #footer><ElButton :disabled="saving" @click="dialogVisible = false">取消</ElButton><ElButton type="primary" :loading="saving" :disabled="readonly" @click="saveAction">保存</ElButton></template>
    </ElDialog>
  </div>
</template>
<style scoped>
.extra-record-fields{border:0;margin:0;padding:0}.today-page{max-width:1000px;margin:0 auto}.page__header{justify-content:flex-end}
.today-summary{display:grid;grid-template-columns:1fr 1fr;gap:24px;padding:25px 30px;background:var(--kaoyan-card,#fff);border:1px solid var(--kaoyan-border);border-radius:14px;margin-bottom:20px}
.summary-metric{display:flex;flex-direction:column;gap:8px}.summary-metric>span{font-size:13px;color:var(--kaoyan-text-secondary)}.summary-metric strong{font-size:26px;font-weight:600;letter-spacing:-.5px}.summary-metric small{font-size:12px;color:var(--kaoyan-text-secondary)}
.subject-section{margin-bottom:16px}.subject-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:12px}.subject-heading h2{font-size:18px;font-weight:600;margin:0}.subject-times{display:flex;gap:22px;font-size:12px;color:var(--kaoyan-text-secondary)}.subject-times b{font-weight:500;color:var(--kaoyan-text)}
.checklist{list-style:none;padding:0;margin:0}.empty-subject{font-size:13px;color:var(--kaoyan-text-secondary);margin:14px 0 4px}.section-note{font-size:12px;color:var(--kaoyan-text-secondary);line-height:1.6}
.completed{margin-top:8px}.completed summary,.extra-record summary{font-size:13px;color:var(--kaoyan-text-secondary);cursor:pointer;padding:10px 0}.extra-record :deep(form){margin-top:16px}.dialog-task{font-size:15px;margin:0 0 20px}.field-label{display:block;margin:18px 0 8px;font-size:13px}.save-error{margin-top:20px}
@media(max-width:600px){.today-summary{padding:20px;gap:12px}.summary-metric strong{font-size:18px}.subject-heading{align-items:flex-start;flex-direction:column;gap:8px}.subject-times{gap:18px}.page__actions :deep(.el-date-editor){width:160px}}
</style>
