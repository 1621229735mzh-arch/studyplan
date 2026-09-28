<script setup lang="ts">
/**
 * 学习记录页：补记、筛选、修改与删除。
 *
 * 约束：
 * - 修改必须回传 version，后端检测到数据已变化时返回冲突错误，
 *   这里提示“已在其它设备修改，请刷新”，不做静默覆盖；
 * - 离线为只读，不排队、不假装成功；
 * - 删除后统计由后端重算，前端只刷新展示。
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
  ElDialog,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect
} from 'element-plus'

import { toApiError } from '@/api/http'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { listPlanTasks } from '@/features/plan/api'
import type { PlanTask } from '@/features/plan/types'
import { useCatalogStore } from '@/stores/catalog'
import { createLearningRecord, deleteLearningRecord, listLearningRecords, updateLearningRecord } from '../api'
import RecordForm from '../components/RecordForm.vue'
import RecordTable from '../components/RecordTable.vue'
import type { LearningRecord, LearningRecordFormSubmission, LearningRecordTaskOption } from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const filters = reactive<{ from: string; to: string; taskId: number | null; subjectId: number | null }>({
  from: '',
  to: '',
  taskId: null,
  subjectId: null
})

const records = ref<LearningRecord[]>([])
const planTasks = ref<PlanTask[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

const createFormRef = ref<InstanceType<typeof RecordForm> | null>(null)
const editFormRef = ref<InstanceType<typeof RecordForm> | null>(null)

const editing = ref<LearningRecord | null>(null)
const editVisible = ref(false)

const taskOptions = computed<LearningRecordTaskOption[]>(() =>
  planTasks.value.map((task) => ({
    id: task.id,
    title: task.title,
    subjectId: task.subjectId ?? null,
    // 任务列表接口只返回 id，名称用“设置”的科目/单位目录按 id 取。
    subjectName: catalog.subjectNameOf(task.subjectId),
    unitId: task.unitId ?? null,
    unitName: catalog.unitNameOf(task.unitId)
  }))
)

async function loadRecords(): Promise<void> {
  if (!online.value) {
    errorMessage.value = '离线状态无法读取学习记录列表（离线只缓存今日、本周与进度概览）。'
    return
  }
  loading.value = true
  errorMessage.value = null
  try {
    records.value = await listLearningRecords({
      from: filters.from || undefined,
      to: filters.to || undefined,
      taskId: filters.taskId ?? undefined,
      subjectId: filters.subjectId ?? undefined
    })
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

async function loadTasks(): Promise<void> {
  try {
    planTasks.value = await listPlanTasks()
  } catch {
    // 任务列表不可用时不影响记录查询；表单里也需要任务来确定单位
    planTasks.value = []
  }
}

function resetFilters(): void {
  filters.from = ''
  filters.to = ''
  filters.taskId = null
  filters.subjectId = null
  void loadRecords()
}

function onFromChange(value: unknown): void {
  filters.from = typeof value === 'string' ? value : ''
}

function onToChange(value: unknown): void {
  filters.to = typeof value === 'string' ? value : ''
}

function onTaskFilterChange(value: unknown): void {
  filters.taskId = typeof value === 'number' ? value : null
}

function onSubjectFilterChange(value: unknown): void {
  filters.subjectId = typeof value === 'number' ? value : null
}

function warnOffline(): void {
  ElMessage.warning('当前处于离线状态，第一版离线为只读，无法提交或修改记录。')
}

async function handleCreate(payload: LearningRecordFormSubmission): Promise<void> {
  if (!online.value) {
    warnOffline()
    return
  }
  submitting.value = true
  try {
    await createLearningRecord({
      taskId: payload.model.taskId,
      subjectId: payload.model.subjectId,
      unitId: payload.model.unitId,
      amount: payload.model.amount,
      // 用时未填时传 null（表示未记录），不写成 0
      durationMinutes: payload.model.durationMinutes ?? null,
      note: payload.model.note.trim() === '' ? null : payload.model.note,
      recordDate: payload.model.recordDate,
      clientToken: payload.clientToken
    })
    ElMessage.success('学习记录已保存')
    createFormRef.value?.reset()
    await loadRecords()
  } catch (caught) {
    createFormRef.value?.setServerError(toApiError(caught).message, toApiError(caught).fieldErrors)
  } finally {
    submitting.value = false
  }
}

async function openEdit(record: LearningRecord): Promise<void> {
  editing.value = record
  editVisible.value = true
  await nextTick()
  editFormRef.value?.fillFrom({
    recordDate: record.recordDate,
    taskId: record.taskId ?? null,
    subjectId: record.subjectId ?? null,
    unitId: record.unitId ?? null,
    amount: record.amount ?? null,
    durationMinutes: record.durationMinutes ?? null,
    note: record.note ?? ''
  })
}

async function handleEditSubmit(payload: LearningRecordFormSubmission): Promise<void> {
  const target = editing.value
  if (target === null) {
    return
  }
  if (!online.value) {
    warnOffline()
    return
  }
  submitting.value = true
  try {
    await updateLearningRecord(target.id, {
      taskId: payload.model.taskId,
      subjectId: payload.model.subjectId,
      unitId: payload.model.unitId,
      amount: payload.model.amount,
      durationMinutes: payload.model.durationMinutes ?? null,
      note: payload.model.note.trim() === '' ? null : payload.model.note,
      recordDate: payload.model.recordDate,
      // 多设备编辑检查：把读取时的版本发回后端
      version: target.version
    })
    ElMessage.success('修改已保存，统计已重算')
    editVisible.value = false
    editing.value = null
    await loadRecords()
  } catch (caught) {
    const apiError = toApiError(caught)
    if (apiError.status === 409) {
      editFormRef.value?.setServerError('这条记录已在其它设备被修改，为避免覆盖，请刷新后重新编辑。')
    } else {
      editFormRef.value?.setServerError(apiError.message, apiError.fieldErrors)
    }
  } finally {
    submitting.value = false
  }
}

async function handleDelete(record: LearningRecord): Promise<void> {
  if (!online.value) {
    warnOffline()
    return
  }
  try {
    await ElMessageBox.confirm('删除后相关统计会重新计算，且无法撤销。确定删除这条学习记录吗？', '删除学习记录', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    // 用户取消
    return
  }
  try {
    // 删除需要携带 version（后端做数据版本校验）
    await deleteLearningRecord(record.id, record.version)
    ElMessage.success('已删除，统计已重算')
    await loadRecords()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  }
}

onMounted(() => {
  void catalog.load()
  void loadTasks()
  void loadRecords()
})
</script>

<template>
  <div class="page">
    <div class="page__header">
      <div>
        <h2 class="page__title">学习记录</h2>
        <p class="page__subtitle">
          记录决定实际完成量；创建计划不会增加完成量。修改或删除记录后，进度与图表由后端重新计算。
        </p>
      </div>
    </div>

    <ElAlert v-if="!online" type="warning" show-icon :closable="false" title="离线状态" description="第一版离线只读，无法新增、修改或删除学习记录。" />
    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <section class="section-card">
      <h3 class="section-card__title">筛选</h3>
      <div class="table-toolbar">
        <ElDatePicker
          :model-value="filters.from"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
          @update:model-value="onFromChange"
        />
        <span class="hint-text">至</span>
        <ElDatePicker
          :model-value="filters.to"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
          @update:model-value="onToChange"
        />
        <ElSelect
          :model-value="filters.taskId ?? undefined"
          class="filter-select"
          clearable
          filterable
          placeholder="按任务筛选"
          @update:model-value="onTaskFilterChange"
        >
          <ElOption v-for="task in planTasks" :key="task.id" :label="task.title" :value="task.id" />
        </ElSelect>
        <ElSelect
          :model-value="filters.subjectId ?? undefined"
          class="filter-select"
          clearable
          placeholder="按科目筛选"
          @update:model-value="onSubjectFilterChange"
        >
          <ElOption v-for="subject in catalog.subjects" :key="subject.id" :label="subject.name" :value="subject.id" />
        </ElSelect>
        <ElButton type="primary" :loading="loading" @click="loadRecords">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>
    </section>

    <section class="section-card">
      <h3 class="section-card__title">记录列表</h3>
      <p class="section-card__hint">完成量按单位展示，用时单独统计，两者不相加。</p>
      <RecordTable :records="records" :loading="loading" @edit="openEdit" @delete="handleDelete" />
    </section>

    <section class="section-card">
      <h3 class="section-card__title">补记学习记录</h3>
      <p class="section-card__hint">补记与当天记录等价，都会计入统计。</p>
      <RecordForm ref="createFormRef" :tasks="taskOptions" :submitting="submitting" @submit="handleCreate" />
    </section>

    <ElDialog v-model="editVisible" title="修改学习记录" width="min(720px, 92vw)" destroy-on-close>
      <p class="hint-text">
        当前记录版本：{{ editing?.version ?? '—' }}。保存时会带上该版本，若数据已被其它设备修改会提示冲突。
      </p>
      <RecordForm
        ref="editFormRef"
        mode="edit"
        :tasks="taskOptions"
        :submitting="submitting"
        @submit="handleEditSubmit"
        @cancel="editVisible = false"
      />
    </ElDialog>
  </div>
</template>

<style scoped>
.filter-select {
  width: 180px;
}
</style>
