<script setup lang="ts">
/**
 * 备忘录页：随手记、关键字搜索、到期提醒、转为任务。
 *
 * 对应后端 MemoController / dto：
 * - 字段是 `dueDate`（到期日）、`status`（OPEN/DONE）、`convertedTaskId`（已转出的任务）；
 *   备忘更新请求不接受 taskId 字段；
 * - 转任务走后端专用接口 `POST /api/memo/{id}/convert-to-task`，
 *   body `{taskTitle, subjectId, unitId, plannedAmount}`（科目与单位必填）；
 * - 到期提醒走 `GET /api/memo/due`，可以用 `POST /api/memo/{id}/dismiss-reminder` 忽略本次提醒。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
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
import EmptyState from '@/components/EmptyState.vue'
import { formatIsoDate } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import {
  convertMemoToTask,
  createMemo,
  deleteMemo,
  dismissMemoReminder,
  listDueMemos,
  listMemos,
  updateMemo
} from '../api'
import { MEMO_STATUS_LABELS, MEMO_STATUS_OPTIONS, memoStatusLabel, type Memo, type MemoStatus } from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const memos = ref<Memo[]>([])
const dueMemos = ref<Memo[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

const keyword = ref('')
const subjectFilter = ref<number | null>(null)
const statusFilter = ref<MemoStatus | null>(null)

const dialogVisible = ref(false)
const editing = ref<Memo | null>(null)
const form = reactive<{ content: string; subjectId: number | null; dueDate: string; status: MemoStatus }>({
  content: '',
  subjectId: null,
  dueDate: '',
  status: 'OPEN'
})
const fieldErrors = ref<{ content?: string }>({})

const taskDialogVisible = ref(false)
const taskSource = ref<Memo | null>(null)
const taskForm = reactive<{
  taskTitle: string
  subjectId: number | null
  unitId: number | null
  plannedAmount: number | null
}>({
  taskTitle: '',
  subjectId: null,
  unitId: null,
  plannedAmount: null
})

const hasKeyword = computed(() => keyword.value.trim() !== '')

async function load(): Promise<void> {
  if (!online.value) {
    errorMessage.value = '离线状态无法读取备忘录。'
    return
  }
  loading.value = true
  errorMessage.value = null
  try {
    const [list, due] = await Promise.all([
      listMemos({
        keyword: keyword.value.trim() === '' ? undefined : keyword.value.trim(),
        subjectId: subjectFilter.value ?? undefined,
        status: statusFilter.value ?? undefined
      }),
      listDueMemos()
    ])
    memos.value = list
    dueMemos.value = due
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function resetFilters(): void {
  keyword.value = ''
  subjectFilter.value = null
  statusFilter.value = null
  void load()
}

function onSubjectFilterChange(value: unknown): void {
  subjectFilter.value = typeof value === 'number' ? value : null
}

function onStatusFilterChange(value: unknown): void {
  statusFilter.value = MEMO_STATUS_OPTIONS.find((option) => option === value) ?? null
}

function onFormSubjectChange(value: unknown): void {
  form.subjectId = typeof value === 'number' ? value : null
}

function onFormStatusChange(value: unknown): void {
  form.status = MEMO_STATUS_OPTIONS.find((option) => option === value) ?? 'OPEN'
}

function onDueDateChange(value: unknown): void {
  form.dueDate = typeof value === 'string' ? value : ''
}

function openCreate(): void {
  editing.value = null
  form.content = ''
  form.subjectId = subjectFilter.value
  form.dueDate = ''
  form.status = 'OPEN'
  fieldErrors.value = {}
  dialogVisible.value = true
}

function openEdit(memo: Memo): void {
  editing.value = memo
  form.content = memo.content
  form.subjectId = memo.subjectId ?? null
  form.dueDate = memo.dueDate ?? ''
  form.status = memo.status === 'DONE' ? 'DONE' : 'OPEN'
  fieldErrors.value = {}
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  if (form.content.trim() === '') {
    fieldErrors.value = { content: '请填写备忘内容' }
    return
  }
  fieldErrors.value = {}
  if (!online.value) {
    ElMessage.warning('离线状态无法保存备忘录。')
    return
  }
  submitting.value = true
  try {
    const payload = {
      content: form.content.trim(),
      subjectId: form.subjectId,
      dueDate: form.dueDate === '' ? null : form.dueDate,
      status: form.status
    }
    if (editing.value === null) {
      // 新建时 version 传 0（后端 MemoRequest 要求非空）
      await createMemo({ ...payload, version: 0 })
      ElMessage.success('备忘已保存')
    } else {
      await updateMemo(editing.value.id, { ...payload, version: editing.value.version })
      ElMessage.success('备忘已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (caught) {
    const apiError = toApiError(caught)
    errorMessage.value = apiError.status === 409 ? '该备忘已在其它设备被修改，请刷新后重试。' : apiError.message
  } finally {
    submitting.value = false
  }
}

async function handleDelete(memo: Memo): Promise<void> {
  if (!online.value) {
    ElMessage.warning('离线状态无法删除备忘录。')
    return
  }
  try {
    await ElMessageBox.confirm('确定删除这条备忘吗？已转成的任务不会被删除。', '删除备忘', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteMemo(memo.id)
    ElMessage.success('已删除')
    await load()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  }
}

/** 忽略本次到期提醒（备忘本身保留）。 */
async function handleDismiss(memo: Memo): Promise<void> {
  if (!online.value) {
    ElMessage.warning('离线状态无法忽略提醒。')
    return
  }
  try {
    await dismissMemoReminder(memo.id)
    ElMessage.success('已忽略本次提醒')
    await load()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  }
}

function openTaskDialog(memo: Memo): void {
  taskSource.value = memo
  taskForm.taskTitle = memo.content.length > 60 ? memo.content.slice(0, 60) : memo.content
  taskForm.subjectId = memo.subjectId ?? subjectFilter.value
  taskForm.unitId = null
  taskForm.plannedAmount = null
  taskDialogVisible.value = true
}

function onTaskSubjectChange(value: unknown): void {
  taskForm.subjectId = typeof value === 'number' ? value : null
}

function onTaskUnitChange(value: unknown): void {
  taskForm.unitId = typeof value === 'number' ? value : null
}

function onTaskAmountChange(value: unknown): void {
  taskForm.plannedAmount = typeof value === 'number' ? value : null
}

async function handleConvertToTask(): Promise<void> {
  const source = taskSource.value
  if (source === null) {
    return
  }
  if (taskForm.taskTitle.trim() === '') {
    ElMessage.warning('请填写任务名称')
    return
  }
  if (taskForm.subjectId === null) {
    ElMessage.warning('请选择科目（后端要求科目与单位必填）')
    return
  }
  if (taskForm.unitId === null) {
    ElMessage.warning('任务必须有计量单位，请先选择单位')
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法转为任务。')
    return
  }
  submitting.value = true
  try {
    // 转任务由后端在同一个事务里完成，并把 convertedTaskId 写回备忘录。
    const updated = await convertMemoToTask(source.id, {
      taskTitle: taskForm.taskTitle.trim(),
      subjectId: taskForm.subjectId,
      unitId: taskForm.unitId,
      plannedAmount: taskForm.plannedAmount
    })
    ElMessage.success(
      updated.convertedTaskId
        ? `已转为任务 #${updated.convertedTaskId}，可在“计划”页安排到某一天`
        : '已转为任务，可在“计划”页安排到某一天'
    )
    taskDialogVisible.value = false
    taskSource.value = null
    await load()
  } catch (caught) {
    const apiError = toApiError(caught)
    errorMessage.value = apiError.status === 409 ? '该备忘已在其它设备被修改，请刷新后重试。' : apiError.message
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
  <div class="page">
    <div class="page__header">
      <div>
        <h2 class="page__title">备忘录</h2>
        <p class="page__subtitle">随手记下要做的事，设置到期日，需要时转成正式任务再安排到某一天。</p>
      </div>
      <div class="page__actions">
        <ElButton type="primary" :disabled="!online" @click="openCreate">新建备忘</ElButton>
      </div>
    </div>

    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <section class="section-card">
      <h3 class="section-card__title">到期提醒</h3>
      <p class="section-card__hint">
        站内提醒：后端返回仍未完成且已到期的备忘，不会自动变成任务；可以忽略本次提醒。
      </p>
      <EmptyState v-if="dueMemos.length === 0" description="没有到期的备忘" />
      <ul v-else class="memo-due">
        <li v-for="memo in dueMemos" :key="memo.id" class="memo-due__item">
          <div class="memo-due__content">{{ memo.content }}</div>
          <div class="memo-due__meta">
            <ElTag type="danger" size="small" effect="plain">已到期</ElTag>
            <span class="hint-text">到期日：{{ formatIsoDate(memo.dueDate) }}</span>
            <ElButton size="small" text type="primary" @click="openTaskDialog(memo)">转为任务</ElButton>
            <ElButton size="small" text @click="handleDismiss(memo)">忽略本次提醒</ElButton>
          </div>
        </li>
      </ul>
    </section>

    <section class="section-card">
      <h3 class="section-card__title">全部备忘</h3>
      <div class="table-toolbar">
        <ElInput
          v-model="keyword"
          class="memo-search"
          placeholder="按关键字搜索"
          clearable
          @keyup.enter="load"
        />
        <ElSelect
          :model-value="subjectFilter ?? undefined"
          class="memo-select"
          clearable
          placeholder="按科目筛选"
          @update:model-value="onSubjectFilterChange"
        >
          <ElOption v-for="subject in catalog.subjects" :key="subject.id" :label="subject.name" :value="subject.id" />
        </ElSelect>
        <ElSelect
          :model-value="statusFilter ?? undefined"
          class="memo-select"
          clearable
          placeholder="按状态筛选"
          @update:model-value="onStatusFilterChange"
        >
          <ElOption
            v-for="status in MEMO_STATUS_OPTIONS"
            :key="status"
            :label="MEMO_STATUS_LABELS[status]"
            :value="status"
          />
        </ElSelect>
        <ElButton type="primary" :loading="loading" @click="load">搜索</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
        <span v-if="hasKeyword" class="hint-text">当前关键字：{{ keyword }}</span>
      </div>

      <EmptyState v-if="!loading && memos.length === 0" description="没有符合条件的备忘" hint="换个关键字，或新建一条备忘。" />
      <ElTable v-else :data="memos" size="small" stripe>
        <ElTableColumn label="内容" min-width="240">
          <template #default="{ row }">
            <div>{{ row.content }}</div>
            <div class="hint-text">
              <ElTag v-if="row.subjectId" size="small" effect="plain">
                {{ catalog.subjectNameOf(row.subjectId) ?? `科目 #${row.subjectId}` }}
              </ElTag>
              <ElTag size="small" type="info" effect="plain">{{ memoStatusLabel(row.status) }}</ElTag>
              <ElTag v-if="row.convertedTaskId" size="small" type="success" effect="plain">
                已转任务 #{{ row.convertedTaskId }}
              </ElTag>
              <ElTag v-if="row.reminderDismissed" size="small" type="warning" effect="plain">已忽略本次提醒</ElTag>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="到期日" width="150">
          <template #default="{ row }">{{ formatIsoDate(row.dueDate) }}</template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <ElButton
              size="small"
              text
              type="primary"
              :disabled="row.convertedTaskId !== null && row.convertedTaskId !== undefined"
              @click="openTaskDialog(row)"
            >
              转为任务
            </ElButton>
            <ElButton size="small" text type="primary" @click="openEdit(row)">编辑</ElButton>
            <ElButton size="small" text type="danger" @click="handleDelete(row)">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </section>

    <ElDialog v-model="dialogVisible" :title="editing ? '编辑备忘' : '新建备忘'" width="min(560px, 92vw)">
      <ElForm label-width="90px" @submit.prevent>
        <ElFormItem label="内容" :error="fieldErrors.content">
          <ElInput v-model="form.content" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="要记的事情" />
        </ElFormItem>
        <ElFormItem label="科目">
          <ElSelect
            :model-value="form.subjectId ?? undefined"
            clearable
            placeholder="可留空"
            class="memo-select"
            @update:model-value="onFormSubjectChange"
          >
            <ElOption v-for="subject in catalog.subjects" :key="subject.id" :label="subject.name" :value="subject.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="到期日">
          <ElDatePicker
            :model-value="form.dueDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="可留空"
            @update:model-value="onDueDateChange"
          />
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect :model-value="form.status" class="memo-select" @update:model-value="onFormStatusChange">
            <ElOption
              v-for="status in MEMO_STATUS_OPTIONS"
              :key="status"
              :label="MEMO_STATUS_LABELS[status]"
              :value="status"
            />
          </ElSelect>
          <span class="hint-text">只有“未完成”的备忘会出现在到期提醒里。</span>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" :disabled="!online" @click="handleSubmit">保存</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="taskDialogVisible" title="转为任务" width="min(560px, 92vw)">
      <ElAlert
        type="info"
        show-icon
        :closable="false"
        title="转成任务后还需要安排到具体某一天"
        description="任务只是计划；完成量仍然来自学习记录。任务标题、科目与单位由本人确认，不从备忘内容自动推断。"
      />
      <ElForm label-width="90px" @submit.prevent>
        <ElFormItem label="任务名称">
          <ElInput v-model="taskForm.taskTitle" maxlength="200" />
        </ElFormItem>
        <ElFormItem label="科目">
          <ElSelect
            :model-value="taskForm.subjectId ?? undefined"
            clearable
            placeholder="必选"
            class="memo-select"
            @update:model-value="onTaskSubjectChange"
          >
            <ElOption v-for="subject in catalog.subjects" :key="subject.id" :label="subject.name" :value="subject.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="计量单位">
          <ElSelect
            :model-value="taskForm.unitId ?? undefined"
            clearable
            placeholder="必选"
            class="memo-select"
            @update:model-value="onTaskUnitChange"
          >
            <ElOption v-for="unit in catalog.units" :key="unit.id" :label="unit.name" :value="unit.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="计划量">
          <ElInputNumber
            :model-value="taskForm.plannedAmount ?? undefined"
            :min="0"
            :step="1"
            controls-position="right"
            placeholder="未设置"
            @update:model-value="onTaskAmountChange"
          />
          <span class="hint-text">可以留空：留空表示“尚未确定”，不会被当作 0。</span>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="taskDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" :disabled="!online" @click="handleConvertToTask">
          转为任务
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.memo-search {
  width: 240px;
}

.memo-select {
  width: 180px;
}

.memo-due {
  margin: 0;
  padding: 0;
  list-style: none;
}

.memo-due__item {
  padding: 10px 0;
  border-bottom: 1px dashed var(--kaoyan-border);
}

.memo-due__item:last-child {
  border-bottom: none;
}

.memo-due__content {
  font-weight: 500;
}

.memo-due__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 4px;
}
</style>
