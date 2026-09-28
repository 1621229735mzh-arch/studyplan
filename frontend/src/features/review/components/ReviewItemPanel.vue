<script setup lang="ts">
/**
 * 复习清单面板：把已学内容加入复习 + 记录复习反馈。
 *
 * 对应后端 ReviewController / dto：
 * - 加入复习：`POST /api/review/items`，body `{taskId?, sourceRecordId?, title?, estimatedMinutes?}`
 *   （关联任务时取任务信息；计划外内容必须选择已有学习记录，科目由记录确定）；
 * - 复习反馈：`POST /api/review/records`，body
 *   `{reviewItemId, reviewDate, result, durationMinutes?, note?, nextReviewDate?, clientToken?}`
 *   —— 掌握反馈字段是 `result`、提交令牌是 `clientToken`；
 * - 响应带 `intervalConfigured`：false 表示该档位的复习间隔还没配置，
 *   此时 `nextReviewDate` 为空，界面必须提示去补充间隔，不能编造日期。
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
  ElInputNumber,
  ElMessage,
  ElOption,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'

import { toApiError } from '@/api/http'
import EmptyState from '@/components/EmptyState.vue'
import { formatIntervalDays, formatIsoDate, formatMinutesOr, masteryLabel, reviewStatusLabel, todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { listPlanTasks } from '@/features/plan/api'
import type { PlanTask } from '@/features/plan/types'
import { listLearningRecords } from '@/features/learning/api'
import type { LearningRecord } from '@/features/learning/types'
import { useCatalogStore } from '@/stores/catalog'
import type { MasteryLevel } from '@/types/domain'
import { createReviewItem, createReviewRecord, listReviewItems } from '../api'
import type { ReviewItem } from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const items = ref<ReviewItem[]>([])
const tasks = ref<PlanTask[]>([])
const sourceRecords = ref<LearningRecord[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const noticeMessage = ref<string | null>(null)

const form = reactive<{
  taskId: number | null
  sourceRecordId: number | null
  title: string
  estimatedMinutes: number | null
}>({
  taskId: null,
  sourceRecordId: null,
  title: '',
  estimatedMinutes: null
})
const fieldErrors = ref<{ sourceRecordId?: string; title?: string }>({})

const feedbackVisible = ref(false)
const feedbackTarget = ref<ReviewItem | null>(null)
const feedback = reactive<{
  result: MasteryLevel
  reviewDate: string
  durationMinutes: number | null
  nextReviewDate: string
  note: string
}>({
  result: 'VAGUE',
  reviewDate: todayIso(),
  durationMinutes: null,
  nextReviewDate: '',
  note: ''
})
/** 反馈提交的幂等令牌：打开对话框时生成一次，重试复用。 */
const feedbackToken = ref('')

const MASTERY_OPTIONS: Array<{ value: MasteryLevel; label: string }> = [
  { value: 'FORGOT', label: '不会' },
  { value: 'VAGUE', label: '模糊' },
  { value: 'MASTERED', label: '掌握' }
]

function newClientToken(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `review-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function load(): Promise<void> {
  if (!online.value) {
    errorMessage.value = '离线状态无法读取复习清单。'
    return
  }
  loading.value = true
  errorMessage.value = null
  try {
    const [reviewItems, taskList, records] = await Promise.all([
      listReviewItems(), listPlanTasks(), listLearningRecords({ to: todayIso() })
    ])
    items.value = reviewItems
    const studied = records.filter((record) => (record.amount ?? 0) > 0 || (record.durationMinutes ?? 0) > 0)
    const studiedTaskIds = new Set(studied.map((record) => record.taskId))
    tasks.value = taskList.filter((task) => studiedTaskIds.has(task.id))
    sourceRecords.value = studied.filter((record) => record.taskId == null && record.subjectId != null)
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function onSourceRecordChange(value: unknown): void {
  form.sourceRecordId = typeof value === 'number' ? value : null
  const source = sourceRecords.value.find((record) => record.id === form.sourceRecordId)
  form.title = source?.content?.slice(0, 200) ?? ''
}

function onTaskChange(value: unknown): void {
  form.taskId = typeof value === 'number' ? value : null
  form.sourceRecordId = null
}

function onEstimatedMinutesChange(value: unknown): void {
  form.estimatedMinutes = typeof value === 'number' ? value : null
}

function onResultChange(value: unknown): void {
  const matched = MASTERY_OPTIONS.find((option) => option.value === value)
  feedback.result = matched?.value ?? 'VAGUE'
}

function onReviewDateChange(value: unknown): void {
  feedback.reviewDate = typeof value === 'string' && value ? value : todayIso()
}

function onNextReviewDateChange(value: unknown): void {
  feedback.nextReviewDate = typeof value === 'string' ? value : ''
}

function onDurationMinutesChange(value: unknown): void {
  feedback.durationMinutes = typeof value === 'number' ? value : null
}

async function handleCreate(): Promise<void> {
  const errors: { sourceRecordId?: string; title?: string } = {}
  if (form.taskId === null) {
    if (form.sourceRecordId === null) {
      errors.sourceRecordId = '请选择已有的计划外学习记录'
    }
    if (form.title.trim() === '') {
      errors.title = '请填写要复习的内容'
    }
  }
  fieldErrors.value = errors
  if (Object.keys(errors).length > 0) {
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法新增复习项。')
    return
  }
  submitting.value = true
  noticeMessage.value = null
  try {
    await createReviewItem(
      form.taskId !== null
        ? { taskId: form.taskId, estimatedMinutes: form.estimatedMinutes }
        : {
            sourceRecordId: form.sourceRecordId,
            title: form.title.trim(),
            estimatedMinutes: form.estimatedMinutes
          }
    )
    ElMessage.success('已加入复习')
    form.taskId = null
    form.sourceRecordId = null
    form.title = ''
    form.estimatedMinutes = null
    await load()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    submitting.value = false
  }
}

function openFeedback(item: ReviewItem): void {
  feedbackTarget.value = item
  feedback.result = 'VAGUE'
  feedback.reviewDate = todayIso()
  feedback.durationMinutes = null
  feedback.nextReviewDate = ''
  feedback.note = ''
  feedbackToken.value = newClientToken()
  feedbackVisible.value = true
}

async function submitFeedback(): Promise<void> {
  const target = feedbackTarget.value
  if (target === null) {
    return
  }
  if (!feedback.reviewDate) {
    ElMessage.warning('请选择复习日期')
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法提交复习反馈。')
    return
  }
  submitting.value = true
  errorMessage.value = null
  noticeMessage.value = null
  try {
    const record = await createReviewRecord({
      reviewItemId: target.id,
      reviewDate: feedback.reviewDate,
      result: feedback.result,
      durationMinutes: feedback.durationMinutes,
      note: feedback.note.trim() === '' ? null : feedback.note,
      nextReviewDate: feedback.nextReviewDate === '' ? null : feedback.nextReviewDate,
      clientToken: feedbackToken.value
    })
    if (!record.intervalConfigured) {
      // 间隔未配置：后端没有推导下次日期，如实提示，不编造。
      noticeMessage.value = record.nextReviewDate
        ? `反馈已记录。下次复习日期：${record.nextReviewDate}。`
        : '反馈已记录，但该掌握档位的复习间隔尚未配置，因此没有生成下次复习日期；请到“设置”里填写复习间隔，或手动指定下次日期。'
      ElMessage.warning('已记录反馈：复习间隔未配置，未生成下次复习日期')
    } else {
      noticeMessage.value = `反馈已记录：间隔 ${record.intervalDays ?? '—'} 天，下次复习日期 ${
        record.nextReviewDate ?? '未给出'
      }。`
      ElMessage.success('复习反馈已记录，下次建议日期由后端重新计算')
    }
    feedbackVisible.value = false
    feedbackTarget.value = null
    await load()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
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
    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />
    <ElAlert v-if="noticeMessage" type="warning" show-icon :closable="false" :title="noticeMessage" />
    <ElAlert
      type="info"
      show-icon
      :closable="false"
      title="复习只针对已经学过并加入复习的内容"
      description="关联任务加入复习时，后端要求该任务已经有学习记录；复习反馈不会重复增加首次学习量。"
    />

    <div class="review-create">
      <ElSelect
        :model-value="form.taskId ?? undefined"
        class="review-create__select"
        clearable
        filterable
        placeholder="关联任务（可选）"
        @update:model-value="onTaskChange"
      >
        <ElOption v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
      </ElSelect>
      <ElSelect
        :model-value="form.sourceRecordId ?? undefined"
        class="review-create__select"
        clearable
        :disabled="form.taskId !== null"
        filterable
        placeholder="或选择计划外学习记录"
        @update:model-value="onSourceRecordChange"
      >
        <ElOption
          v-for="record in sourceRecords"
          :key="record.id"
          :label="`${record.recordDate} · ${record.subjectName ?? '科目'} · ${record.content || `记录 #${record.id}`}`"
          :value="record.id"
        />
      </ElSelect>
      <ElInput
        v-model="form.title"
        class="review-create__title"
        :disabled="form.taskId !== null"
        placeholder="要复习的内容，例如：操作系统第 3 讲要点"
      />
      <ElInputNumber
        :model-value="form.estimatedMinutes ?? undefined"
        :min="1"
        :step="5"
        controls-position="right"
        placeholder="预计用时（分钟，可留空）"
        @update:model-value="onEstimatedMinutesChange"
      />
      <ElButton type="primary" :loading="submitting" :disabled="!online" @click="handleCreate">加入复习</ElButton>
    </div>
    <p v-if="fieldErrors.sourceRecordId || fieldErrors.title" class="warning-text">
      {{ fieldErrors.sourceRecordId ?? fieldErrors.title }}
    </p>
    <p class="hint-text">
      选择已学任务，或选择已有的计划外学习记录并填写复习内容。预计用时留空时，后端无法判断额度。
    </p>

    <EmptyState
      v-if="!loading && items.length === 0"
      description="复习清单还是空的"
      hint="先学习并通过学习记录确认已学内容，再把需要巩固的部分加入复习。"
    />

    <ElTable v-else :data="items" size="small" stripe>
      <ElTableColumn label="内容" min-width="200">
        <template #default="{ row }">
          <div>{{ row.title }}</div>
          <div class="hint-text">
            <ElTag v-if="row.subjectId" size="small" effect="plain">
              {{ catalog.subjectNameOf(row.subjectId) ?? `科目 #${row.subjectId}` }}
            </ElTag>
            <ElTag size="small" type="info" effect="plain">{{ reviewStatusLabel(row.status) }}</ElTag>
          </div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="下次复习日期" width="150">
        <template #default="{ row }">
          {{ formatIsoDate(row.nextReviewDate) }}
          <div v-if="!row.nextReviewDate" class="hint-text">间隔未配置时不生成日期</div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="间隔" width="110">
        <template #default="{ row }">{{ formatIntervalDays(row.intervalDays) }}</template>
      </ElTableColumn>
      <ElTableColumn label="预计用时" width="120">
        <template #default="{ row }">{{ formatMinutesOr(row.estimatedMinutes, '未提供') }}</template>
      </ElTableColumn>
      <ElTableColumn label="上次反馈" width="110">
        <template #default="{ row }">{{ masteryLabel(row.lastResult ?? null) }}</template>
      </ElTableColumn>
      <ElTableColumn label="已安排到" width="130">
        <template #default="{ row }">{{ formatIsoDate(row.scheduledDate) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" text type="primary" @click="openFeedback(row)">记录反馈</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElDialog v-model="feedbackVisible" title="记录复习反馈" width="min(540px, 92vw)">
      <p class="hint-text">{{ feedbackTarget?.title }}</p>
      <ElForm label-width="110px" @submit.prevent>
        <ElFormItem label="掌握情况">
          <ElRadioGroup :model-value="feedback.result" @update:model-value="onResultChange">
            <ElRadioButton v-for="option in MASTERY_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </ElRadioButton>
          </ElRadioGroup>
        </ElFormItem>
        <ElFormItem label="复习日期">
          <ElDatePicker
            :model-value="feedback.reviewDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="复习日期"
            @update:model-value="onReviewDateChange"
          />
        </ElFormItem>
        <ElFormItem label="用时">
          <ElInputNumber
            :model-value="feedback.durationMinutes ?? undefined"
            :min="1"
            :step="5"
            controls-position="right"
            @update:model-value="onDurationMinutesChange"
          />
          <span class="hint-text">分钟（可留空）</span>
        </ElFormItem>
        <ElFormItem label="下次复习日期">
          <ElDatePicker
            :model-value="feedback.nextReviewDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="可留空：留空时按设置中的间隔推导"
            @update:model-value="onNextReviewDateChange"
          />
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="feedback.note" type="textarea" :rows="2" placeholder="卡住的地方（可留空）" />
        </ElFormItem>
      </ElForm>
      <p class="hint-text">
        下次日期优先用你填写的值；留空时按“设置”里该掌握档位的间隔推导。间隔未配置时后端不会给出日期，
        界面会如实提示并保留为空。
      </p>
      <template #footer>
        <ElButton @click="feedbackVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" :disabled="!online" @click="submitFeedback">保存反馈</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.review-create {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 12px 0;
}

.review-create__title {
  width: 300px;
}

.review-create__select {
  width: 180px;
}
</style>
