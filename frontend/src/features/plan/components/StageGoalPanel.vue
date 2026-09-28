<script setup lang="ts">
/**
 * 阶段目标面板。
 *
 * 阶段目标是计划的起点（阶段目标 → 周任务 → 今日安排 → 学习记录 → 进度反馈）。
 *
 * 对应后端 StageGoalCreateRequest / StageGoalUpdateRequest：
 * - 科目（subjectId）必填，标题必填，说明与起止日期可留空；
 * - 结束日期的字段名是 targetDate（不是 endDate）；
 * - status 在后端是自由字符串（默认 ACTIVE），前端不臆造取值集合，原样展示。
 */
import { onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'

import { toApiError } from '@/api/http'
import EmptyState from '@/components/EmptyState.vue'
import { formatIsoDate } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import { createStageGoal, listStageGoals, updateStageGoal } from '../api'
import type { StageGoal } from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const goals = ref<StageGoal[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

const dialogVisible = ref(false)
const editing = ref<StageGoal | null>(null)

const form = reactive<{
  subjectId: number | null
  title: string
  description: string
  startDate: string
  targetDate: string
}>({
  subjectId: null,
  title: '',
  description: '',
  startDate: '',
  targetDate: ''
})

const fieldErrors = ref<{ subjectId?: string; title?: string; targetDate?: string }>({})

async function load(): Promise<void> {
  if (!online.value) {
    errorMessage.value = '离线状态无法读取阶段目标。'
    return
  }
  loading.value = true
  errorMessage.value = null
  try {
    goals.value = await listStageGoals()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  editing.value = null
  form.subjectId = null
  form.title = ''
  form.description = ''
  form.startDate = ''
  form.targetDate = ''
  fieldErrors.value = {}
  dialogVisible.value = true
}

function openEdit(goal: StageGoal): void {
  editing.value = goal
  form.subjectId = goal.subjectId ?? null
  form.title = goal.title
  form.description = goal.description ?? ''
  form.startDate = goal.startDate ?? ''
  form.targetDate = goal.targetDate ?? ''
  fieldErrors.value = {}
  dialogVisible.value = true
}

function onSubjectChange(value: unknown): void {
  form.subjectId = typeof value === 'number' ? value : null
}

function onStartDateChange(value: unknown): void {
  form.startDate = typeof value === 'string' ? value : ''
}

function onTargetDateChange(value: unknown): void {
  form.targetDate = typeof value === 'string' ? value : ''
}

function validate(): boolean {
  const errors: { subjectId?: string; title?: string; targetDate?: string } = {}
  if (form.subjectId === null) {
    errors.subjectId = '请选择科目'
  }
  if (form.title.trim() === '') {
    errors.title = '请填写阶段目标名称'
  }
  if (form.startDate && form.targetDate && form.targetDate < form.startDate) {
    errors.targetDate = '目标日期不能早于开始日期'
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
  if (form.subjectId === null) {
    return
  }
  submitting.value = true
  errorMessage.value = null
  try {
    const payload = {
      subjectId: form.subjectId,
      title: form.title.trim(),
      description: form.description.trim() === '' ? null : form.description,
      startDate: form.startDate === '' ? null : form.startDate,
      targetDate: form.targetDate === '' ? null : form.targetDate
    }
    if (editing.value === null) {
      await createStageGoal(payload)
      ElMessage.success('阶段目标已创建')
    } else {
      await updateStageGoal(editing.value.id, {
        ...payload,
        // 后端 status 为自由字符串：保持原值，不在这里改写。
        status: editing.value.status,
        version: editing.value.version
      })
      ElMessage.success('阶段目标已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (caught) {
    const apiError = toApiError(caught)
    errorMessage.value =
      apiError.status === 409 ? '该目标已在其它设备被修改，请刷新后重试。' : apiError.message
  } finally {
    submitting.value = false
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
      <ElButton type="primary" :disabled="!online" @click="openCreate">新增阶段目标</ElButton>
      <ElButton :loading="loading" @click="load">刷新</ElButton>
      <span class="hint-text">阶段目标只描述方向与时间范围，具体工作量由任务和周计划承担。</span>
    </div>

    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <EmptyState
      v-if="!loading && goals.length === 0"
      description="还没有阶段目标"
      hint="先写下一个阶段的复习范围与截止时间，再往下拆成任务。"
    />

    <ElTable v-else :data="goals" size="small" stripe>
      <ElTableColumn label="目标" min-width="200">
        <template #default="{ row }">
          <div>{{ row.title }}</div>
          <div v-if="row.description" class="hint-text">{{ row.description }}</div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="科目" width="130">
        <template #default="{ row }">
          <ElTag v-if="row.subjectId" size="small" effect="plain">
            {{ catalog.subjectNameOf(row.subjectId) ?? `科目 #${row.subjectId}` }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="起止日期" width="240">
        <template #default="{ row }">
          {{ formatIsoDate(row.startDate) }} ~ {{ formatIsoDate(row.targetDate) }}
        </template>
      </ElTableColumn>
      <ElTableColumn label="状态" width="110">
        <template #default="{ row }">{{ row.status || '—' }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" text type="primary" @click="openEdit(row)">编辑</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElDialog v-model="dialogVisible" :title="editing ? '编辑阶段目标' : '新增阶段目标'" width="min(600px, 92vw)">
      <ElForm label-width="90px" @submit.prevent>
        <ElFormItem label="科目" :error="fieldErrors.subjectId">
          <ElSelect
            :model-value="form.subjectId ?? undefined"
            clearable
            placeholder="选择科目"
            class="stage-goal__select"
            @update:model-value="onSubjectChange"
          >
            <ElOption v-for="subject in catalog.subjects" :key="subject.id" :label="subject.name" :value="subject.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="名称" :error="fieldErrors.title">
          <ElInput v-model="form.title" maxlength="200" placeholder="例如：408 强化阶段" />
        </ElFormItem>
        <ElFormItem label="说明">
          <ElInput v-model="form.description" type="textarea" :rows="2" placeholder="范围与产出（可留空）" />
        </ElFormItem>
        <ElFormItem label="开始日期">
          <ElDatePicker
            :model-value="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
            @update:model-value="onStartDateChange"
          />
        </ElFormItem>
        <ElFormItem label="目标日期" :error="fieldErrors.targetDate">
          <ElDatePicker
            :model-value="form.targetDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="目标日期"
            @update:model-value="onTargetDateChange"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" :disabled="!online" @click="handleSubmit">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.stage-goal__select {
  width: 220px;
}
</style>
