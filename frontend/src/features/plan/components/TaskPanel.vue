<script setup lang="ts">
/**
 * 任务面板：任务的创建、修改、归档与进度查看。
 *
 * 业务约束：
 * - 每个任务有明确的计量单位（后端返回 unitId，单位名从“设置”的目录里取）；
 * - 计划量可以留空，表示“尚未确定”，界面显示“未设置”，绝不写成 0；
 * - 完成量与剩余量由后端 progress 接口计算（这里只展示，不做减法推算）；
 * - 学习记录决定完成量，创建任务不会增加完成量；
 * - 已有学习记录的任务不能删除（后端返回 409 TASK_HAS_RECORDS），应改为归档。
 */
import { onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'

import { toApiError } from '@/api/http'
import AmountUnit from '@/components/AmountUnit.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import { createPlanTask, deletePlanTask, getTaskProgress, listPlanTasks, listStageGoals, updatePlanTask } from '../api'
import {
  TASK_STATUS_LABELS,
  TASK_STATUS_OPTIONS,
  taskStatusLabel,
  type PlanTask,
  type StageGoal,
  type TaskProgressView,
  type TaskStatus
} from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const tasks = ref<PlanTask[]>([])
const goals = ref<StageGoal[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

const dialogVisible = ref(false)
const editing = ref<PlanTask | null>(null)
const progressVisible = ref(false)
const progress = ref<TaskProgressView | null>(null)
const progressLoading = ref(false)

const form = reactive<{
  title: string
  subjectId: number | null
  unitId: number | null
  plannedAmount: number | null
  stageGoalId: number | null
  status: TaskStatus
}>({
  title: '',
  subjectId: null,
  unitId: null,
  plannedAmount: null,
  stageGoalId: null,
  status: 'ACTIVE'
})

const fieldErrors = ref<{ title?: string; subjectId?: string; unitId?: string }>({})

async function load(): Promise<void> {
  if (!online.value) {
    errorMessage.value = '离线状态无法读取任务列表。'
    return
  }
  loading.value = true
  errorMessage.value = null
  try {
    const [taskList, goalList] = await Promise.all([listPlanTasks(), listStageGoals()])
    tasks.value = taskList
    goals.value = goalList
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function resetForm(): void {
  form.title = ''
  form.subjectId = null
  form.unitId = null
  form.plannedAmount = null
  form.stageGoalId = null
  form.status = 'ACTIVE'
  fieldErrors.value = {}
}

function openCreate(): void {
  editing.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(task: PlanTask): void {
  editing.value = task
  form.title = task.title
  form.subjectId = task.subjectId ?? null
  form.unitId = task.unitId ?? null
  form.plannedAmount = task.plannedAmount ?? null
  form.stageGoalId = task.stageGoalId ?? null
  form.status = task.status
  fieldErrors.value = {}
  dialogVisible.value = true
}

function onSubjectChange(value: unknown): void {
  form.subjectId = typeof value === 'number' ? value : null
}

function onUnitChange(value: unknown): void {
  form.unitId = typeof value === 'number' ? value : null
}

function onPlannedAmountChange(value: unknown): void {
  // 清空输入表示“尚未确定计划量”，存 null，不写成 0。
  form.plannedAmount = typeof value === 'number' ? value : null
}

function onStageGoalChange(value: unknown): void {
  form.stageGoalId = typeof value === 'number' ? value : null
}

function onStatusChange(value: unknown): void {
  form.status = TASK_STATUS_OPTIONS.find((option) => option === value) ?? 'ACTIVE'
}

function validate(): boolean {
  const errors: { title?: string; subjectId?: string; unitId?: string } = {}
  if (form.title.trim() === '') {
    errors.title = '请填写任务名称'
  }
  if (form.subjectId === null) {
    errors.subjectId = '请选择科目'
  }
  if (form.unitId === null) {
    errors.unitId = '请选择计量单位（题、讲、章、页……）'
  }
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

async function handleSubmit(): Promise<void> {
  if (!validate()) {
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法保存，第一版离线为只读。')
    return
  }
  if (form.subjectId === null || form.unitId === null) {
    return
  }
  submitting.value = true
  errorMessage.value = null
  try {
    const payload = {
      title: form.title.trim(),
      subjectId: form.subjectId,
      unitId: form.unitId,
      // 计划量可以为空：后端存 null 表示尚未确定，不在这里补 0。
      plannedAmount: form.plannedAmount,
      stageGoalId: form.stageGoalId
    }
    if (editing.value === null) {
      await createPlanTask(payload)
      ElMessage.success('任务已创建')
    } else {
      await updatePlanTask(editing.value.id, {
        ...payload,
        status: form.status,
        version: editing.value.version
      })
      ElMessage.success('任务已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (caught) {
    const apiError = toApiError(caught)
    errorMessage.value = apiError.status === 409 ? '该任务已在其它设备被修改，请刷新后重试。' : apiError.message
  } finally {
    submitting.value = false
  }
}

async function handleDelete(task: PlanTask): Promise<void> {
  if (!online.value) {
    ElMessage.warning('离线状态无法删除。')
    return
  }
  try {
    await ElMessageBox.confirm('删除任务不会删除已经产生的学习记录统计。确定删除吗？', '删除任务', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deletePlanTask(task.id)
    ElMessage.success('任务已删除')
    await load()
  } catch (caught) {
    const apiError = toApiError(caught)
    // 有学习记录时后端拒绝删除并返回 TASK_HAS_RECORDS，此时应改为归档。
    errorMessage.value =
      apiError.code === 'TASK_HAS_RECORDS' || apiError.status === 409
        ? '该任务已有学习记录，不能删除；请把状态改为“已归档”。'
        : apiError.message
  }
}

async function openProgress(task: PlanTask): Promise<void> {
  progressVisible.value = true
  progressLoading.value = true
  progress.value = null
  try {
    progress.value = await getTaskProgress(task.id)
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    progressLoading.value = false
  }
}

onMounted(() => {
  void catalog.load()
  void load()
})
</script>

<template>
  <div>
    <div class="table-toolbar">
      <ElButton type="primary" :disabled="!online" @click="openCreate">新增任务</ElButton>
      <ElButton :loading="loading" @click="load">刷新</ElButton>
      <span class="hint-text">
        每个任务只有一个计量单位；计划量可以留空表示尚未确定；完成量由学习记录累计。
      </span>
    </div>

    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <EmptyState
      v-if="!loading && tasks.length === 0"
      description="还没有任务"
      hint="先创建任务（科目 + 单位 + 计划量），再分配到某一周和具体某一天。"
    />

    <ElTable v-else :data="tasks" size="small" stripe>
      <ElTableColumn label="任务" min-width="220">
        <template #default="{ row }">
          <div>{{ row.title }}</div>
          <div class="hint-text">
            <ElTag v-if="row.subjectId" size="small" effect="plain">
              {{ catalog.subjectNameOf(row.subjectId) ?? `科目 #${row.subjectId}` }}
            </ElTag>
            <ElTag v-if="row.unitId" size="small" type="info" effect="plain">
              单位：{{ catalog.unitNameOf(row.unitId) ?? `#${row.unitId}` }}
            </ElTag>
          </div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="计划量" width="130">
        <template #default="{ row }">
          <AmountUnit :amount="row.plannedAmount ?? null" :unit="catalog.unitNameOf(row.unitId)" placeholder="未设置" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="状态" width="100">
        <template #default="{ row }">{{ taskStatusLabel(row.status) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" text type="primary" @click="openProgress(row)">进度</ElButton>
          <ElButton size="small" text type="primary" @click="openEdit(row)">编辑</ElButton>
          <ElButton size="small" text type="danger" @click="handleDelete(row)">删除</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <p class="hint-text">
      提示：未完成的任务不会自动顺延到次日。剩余工作量需要本人决定是继续做、调整计划量，还是放弃。
    </p>

    <ElDialog v-model="dialogVisible" :title="editing ? '编辑任务' : '新增任务'" width="min(620px, 92vw)">
      <ElForm label-width="90px" @submit.prevent>
        <ElFormItem label="任务名称" :error="fieldErrors.title">
          <ElInput v-model="form.title" maxlength="200" placeholder="例如：操作系统第 1—3 讲" />
        </ElFormItem>
        <ElFormItem label="科目" :error="fieldErrors.subjectId">
          <ElSelect
            :model-value="form.subjectId ?? undefined"
            clearable
            placeholder="选择科目"
            class="plan-select"
            @update:model-value="onSubjectChange"
          >
            <ElOption v-for="subject in catalog.subjects" :key="subject.id" :label="subject.name" :value="subject.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="计量单位" :error="fieldErrors.unitId">
          <ElSelect
            :model-value="form.unitId ?? undefined"
            clearable
            placeholder="选择单位"
            class="plan-select"
            @update:model-value="onUnitChange"
          >
            <ElOption v-for="unit in catalog.units" :key="unit.id" :label="unit.name" :value="unit.id" />
          </ElSelect>
          <span class="hint-text">单位维护在“设置”页面。</span>
        </ElFormItem>
        <ElFormItem label="计划量">
          <ElInputNumber
            :model-value="form.plannedAmount ?? undefined"
            :min="0"
            :step="1"
            controls-position="right"
            placeholder="未设置"
            @update:model-value="onPlannedAmountChange"
          />
          <span class="hint-text">可以留空：留空表示“尚未确定”，不会被当作 0。</span>
        </ElFormItem>
        <ElFormItem label="阶段目标">
          <ElSelect
            :model-value="form.stageGoalId ?? undefined"
            clearable
            placeholder="可留空"
            class="plan-select"
            @update:model-value="onStageGoalChange"
          >
            <ElOption v-for="goal in goals" :key="goal.id" :label="goal.title" :value="goal.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem v-if="editing" label="状态">
          <ElSelect :model-value="form.status" class="plan-select" @update:model-value="onStatusChange">
            <ElOption
              v-for="status in TASK_STATUS_OPTIONS"
              :key="status"
              :label="TASK_STATUS_LABELS[status]"
              :value="status"
            />
          </ElSelect>
          <span class="hint-text">已有学习记录的任务不能删除，请改为“已归档”。</span>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" :disabled="!online" @click="handleSubmit">保存</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="progressVisible" title="任务进度" width="min(640px, 92vw)">
      <div v-if="progressLoading" class="hint-text">加载中……</div>
      <div v-else-if="progress">
        <div class="metric-grid">
          <div>
            <div class="metric-label">计划量</div>
            <div class="metric-value">
              <AmountUnit :amount="progress.plannedAmount ?? null" :unit="progress.unitName" placeholder="未设置" />
            </div>
          </div>
          <div>
            <div class="metric-label">完成量</div>
            <div class="metric-value">
              <AmountUnit :amount="progress.completedAmount ?? null" :unit="progress.unitName" />
            </div>
          </div>
          <div>
            <div class="metric-label">剩余工作量</div>
            <div class="metric-value">
              <AmountUnit :amount="progress.remainingAmount ?? null" :unit="progress.unitName" />
            </div>
          </div>
        </div>
        <p class="hint-text">
          以上数字由后端统计；编辑或删除学习记录后会重新计算。计划量未设置时，剩余工作量也为空。
        </p>
        <p class="hint-text">
          单次学习明细请到“学习记录”页按该任务筛选查看（本接口只返回计划/完成/剩余量）。
        </p>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
.plan-select {
  width: 220px;
}
</style>
