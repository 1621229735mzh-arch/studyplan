<script setup lang="ts">
import { computed, nextTick, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ElAlert, ElButton, ElDialog, ElMessage, ElMessageBox, ElOption, ElSelect } from 'element-plus'
import { toApiError } from '@/api/http'
import PeriodFilter from '@/components/PeriodFilter.vue'
import { formatMinutes } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { listPlanTasks } from '@/features/plan/api'
import type { PlanTask } from '@/features/plan/types'
import { useCatalogStore } from '@/stores/catalog'
import { createLearningRecord, deleteLearningRecord, getRecordArchive, updateLearningRecord } from '../api'
import RecordEditorForm from '../components/RecordEditorForm.vue'
import RecordArchiveTree from '../components/RecordArchiveTree.vue'
import RecordClassificationDialog from '../components/RecordClassificationDialog.vue'
import type { LearningRecord, LearningRecordFormSubmission, LearningRecordTaskOption, RecordArchive } from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()
const filters = reactive({ from: '', to: '', taskId: null as number | null, subjectId: null as number | null })
const archive = ref<RecordArchive | null>(null)
const planTasks = ref<PlanTask[]>([])
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const taskError = ref('')
const createVisible = ref(false)
const editVisible = ref(false)
const editing = ref<LearningRecord | null>(null)
const classifying = ref<LearningRecord | null>(null)
const createForm = ref<InstanceType<typeof RecordEditorForm> | null>(null)
const editForm = ref<InstanceType<typeof RecordEditorForm> | null>(null)
const rangeLabel = computed(() => filters.from ? `${filters.from} — ${filters.to}` : '全部日期')
const taskOptions = computed<LearningRecordTaskOption[]>(() => {
  const options: LearningRecordTaskOption[] = planTasks.value.filter(t=>t.activityMode!=='CHECK_ONLY').map(task => ({ sequential: task.sequential, id: task.id, title: task.title, subjectId: task.subjectId, subjectName: catalog.subjectNameOf(task.subjectId), unitId: task.unitId, unitName: catalog.unitNameOf(task.unitId) }))
  const current = editing.value
  // 已归档任务可能不在候选列表中，编辑时仍保留原来的关联及单位。
  if (current?.taskId && !options.some(task => task.id === current.taskId)) options.push({ sequential: !!current.sequenceItemId, id: current.taskId, title: current.taskTitle || '原关联任务', subjectId: current.subjectId ?? null, subjectName: current.subjectName ?? null, unitId: current.unitId ?? null, unitName: current.unitName ?? null })
  return options
})
let request = 0
async function loadRecords() {
  const id = ++request
  archive.value = null
  error.value = ''
  loading.value = false
  if (!online.value) return
  loading.value = true
  try {
    const response = await getRecordArchive({ from: filters.from || undefined, to: filters.to || undefined, taskId: filters.taskId ?? undefined, subjectId: filters.subjectId ?? undefined })
    if (id === request) archive.value = response
  } catch (caught) { if (id === request) error.value = toApiError(caught).message }
  finally { if (id === request) loading.value = false }
}
async function loadTasks() {
  if (!online.value) return
  taskError.value = ''
  await catalog.load()
  try { planTasks.value = await listPlanTasks() }
  catch { taskError.value = '任务列表暂时无法读取，请重试；已填写的内容会保留。' }
}
function changeRange(range: { from: string; to: string }) { Object.assign(filters, range); void loadRecords() }
async function openCreate() { createVisible.value = true; await loadTasks() }
async function openEdit(record: LearningRecord) {
  if (!online.value || submitting.value) return
  editing.value = record
  editVisible.value = true
  await nextTick()
  editForm.value?.fillFrom({ sequenceItemId: record.sequenceItemId ?? null, recordDate: record.recordDate, taskId: record.taskId ?? null, subjectId: record.subjectId ?? null, unitId: record.unitId ?? null, amount: record.amount ?? null, durationMinutes: record.durationMinutes ?? null, content: record.content ?? '', note: record.note ?? '' })
}
async function save(payload: LearningRecordFormSubmission, edit = false) {
  if (!online.value || submitting.value) return
  submitting.value = true
  const form = edit ? editForm.value : createForm.value
  try {
    const model = { ...payload.model, content: payload.model.content?.trim() || null, note: payload.model.note.trim() || null }
    if (edit && editing.value) await updateLearningRecord(editing.value.id, { ...model, version: editing.value.version })
    else await createLearningRecord({ ...model, clientToken: payload.clientToken })
    if (edit) editVisible.value = false
    else { createForm.value?.reset(); createVisible.value = false }
    const outside = (filters.from && (payload.model.recordDate < filters.from || payload.model.recordDate > filters.to)) || (filters.subjectId && filters.subjectId !== payload.model.subjectId) || (filters.taskId && filters.taskId !== payload.model.taskId)
    ElMessage.success(outside ? '已保存；该记录不在当前筛选范围内，可切换日期或清除筛选查看。' : '记录已保存，进度已更新')
    await loadRecords()
  } catch (caught) {
    const apiError = toApiError(caught)
    form?.setServerError(apiError.status === 409 ? '记录或关联任务已发生变化，请关闭窗口并刷新后重新核对。当前输入已保留。' : apiError.message, apiError.fieldErrors)
  } finally { submitting.value = false }
}
async function remove(record: LearningRecord) {
  if (!online.value || submitting.value) return
  try { await ElMessageBox.confirm(`删除 ${record.recordDate} 的“${record.taskTitle || record.content || '学习记录'}”？相关进度会同步更新，此操作无法撤销。`, '删除学习记录', { type: 'warning', confirmButtonText: '删除记录', cancelButtonText: '保留' }) } catch { return }
  if (!online.value || submitting.value) return
  submitting.value = true
  try { await deleteLearningRecord(record.id, record.version); ElMessage.success('记录已删除，进度已更新'); await loadRecords() }
  catch (caught) { error.value = toApiError(caught).message }
  finally { submitting.value = false }
}
watch(online, () => { void loadRecords(); if (online.value) void loadTasks() })
onMounted(() => { void loadTasks(); void loadRecords() })
onBeforeUnmount(() => { request++ })
</script>

<template>
  <div class="study-workspace" :aria-busy="loading">
    <header class="workspace-header">
      <div><span class="workspace-eyebrow">每一次学习，都留下足迹</span><h1>学习记录</h1><p>从月份到每一天，回看实际学过的内容。</p></div>
      <div class="workspace-actions"><RouterLink to="/">去今日学习 ↗</RouterLink><ElButton type="primary" :disabled="!online || submitting" @click="openCreate">＋ 补记学习</ElButton></div>
    </header>
    <div class="workspace-toolbar">
      <PeriodFilter allow-all initial-period="all" :disabled="!online" @change="changeRange" />
      <div class="workspace-filters">
        <ElSelect :model-value="filters.subjectId ?? undefined" @update:model-value="value => { filters.subjectId = typeof value === 'number' ? value : null }" clearable placeholder="全部科目" aria-label="筛选科目" :disabled="!online" @change="loadRecords"><ElOption v-for="subject in catalog.subjects" :key="subject.id" :label="subject.name" :value="subject.id" /></ElSelect>
        <ElSelect :model-value="filters.taskId ?? undefined" @update:model-value="value => { filters.taskId = typeof value === 'number' ? value : null }" clearable filterable placeholder="全部任务" aria-label="筛选任务" :disabled="!online" @change="loadRecords"><ElOption v-for="task in planTasks" :key="task.id" :label="task.title" :value="task.id" /></ElSelect>
      </div>
    </div>
    <ElAlert v-if="error" :title="error" type="error" :closable="false"><ElButton text @click="loadRecords">重新加载</ElButton></ElAlert>
    <ElAlert v-if="taskError || catalog.error" :title="taskError || catalog.error?.message || ''" type="warning" :closable="false"><ElButton text @click="loadTasks">重试</ElButton></ElAlert>
    <div class="section-heading"><div><h2>月份足迹</h2><p>{{ rangeLabel }}</p></div><span v-if="archive && !loading && !error && online" class="workspace-note">{{ archive.stats.learningDays }} 天 · {{ archive.stats.recordCount }} 次学习 · {{ formatMinutes(archive.stats.studyMinutes) }}<template v-if="archive.stats.missingDurationRecords">（{{ archive.stats.missingDurationRecords }} 次未填用时）</template></span></div>
    <div v-if="!online" class="workspace-empty"><h3>联网后查看完整记录</h3><p>离线时可以查看已同步的今日、本周计划和进度概览。</p><RouterLink to="/progress">查看进度快照 ↗</RouterLink></div>
    <div v-else-if="loading" class="workspace-loading" role="status">正在整理学习记录…</div>
    <RecordArchiveTree v-else-if="archive?.months.length" :months="archive.months" :readonly="submitting || !online" @edit="openEdit" @delete="remove" @classify="classifying = $event" />
    <div v-else-if="!error" class="workspace-empty"><h3>这段时间还没有学习记录</h3><p>补记实际学习后，会自动汇入对应月份、周和日期。</p><ElButton type="primary" @click="openCreate">补记学习</ElButton></div>
    <footer class="workspace-footer"><span>学习记录决定实际进度，修改后会同步更新。</span><div><RouterLink to="/progress">查看学习进度 ↗</RouterLink><ElButton text :disabled="!online || loading" @click="loadRecords">刷新</ElButton></div></footer>
    <RecordClassificationDialog :record="classifying" @close="classifying = null" @saved="loadRecords" />
    <ElDialog v-model="createVisible" title="补记学习" width="min(700px, calc(100vw - 24px))" :close-on-click-modal="false" :close-on-press-escape="!submitting" :show-close="!submitting">
      <p class="workspace-note">选择学习日期，记下实际完成的内容。</p>
      <RecordEditorForm ref="createForm" :tasks="taskOptions" :subjects="catalog.subjects" :units="catalog.units" :submitting="submitting" @submit="save($event)" />
    </ElDialog>
    <ElDialog v-model="editVisible" title="修改学习记录" width="min(700px, calc(100vw - 24px))" :close-on-click-modal="false" :close-on-press-escape="!submitting" :show-close="!submitting" destroy-on-close>
      <p class="workspace-note">修改会同步反映到今日和进度中。</p>
      <RecordEditorForm ref="editForm" mode="edit" :tasks="taskOptions" :subjects="catalog.subjects" :units="catalog.units" :submitting="submitting" @submit="save($event, true)" @cancel="editVisible = false" />
    </ElDialog>
  </div>
</template>
<style scoped src="@/styles/retrospective.css"></style>
