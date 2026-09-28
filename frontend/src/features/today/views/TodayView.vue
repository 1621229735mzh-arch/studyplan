<script setup lang="ts">
/**
 * 今日页面：今天做什么、完成多少。
 *
 * 业务约束：
 * - 学习时长、每日预算/额度与后端提示分别展示，不混加、不生成跨单位合计；
 * - 计划量来自今日安排，完成量以后端返回的 completedAmount 为准（来自学习记录）；
 * - 后端不返回“未记录条目”，这里的“尚未记录”只是按当天学习记录做的呈现，不是新的统计口径；
 * - 离线时退回本地快照（只缓存今天），并明确标注数据来源与最后同步时间；
 * - 保存失败不提示成功，且保留表单输入供重试。
 */
import { computed, onMounted, ref } from 'vue'
import { ElAlert, ElButton, ElDatePicker, ElMessage, ElTag } from 'element-plus'

import { toApiError } from '@/api/http'
import AmountUnit from '@/components/AmountUnit.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatBudget, formatDateTime, formatMinutes, todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { createLearningRecord } from '@/features/learning/api'
import RecordForm from '@/features/learning/components/RecordForm.vue'
import type {
  LearningRecordFormModel,
  LearningRecordFormSubmission,
  LearningRecordTaskOption
} from '@/features/learning/types'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import { useCatalogStore } from '@/stores/catalog'
import { useSessionStore } from '@/stores/session'
import { fetchToday } from '../api'
import PlannedTaskTable from '../components/PlannedTaskTable.vue'
import ReviewedTaskList from '../components/ReviewedTaskList.vue'
import type { TodayPlanTaskRow, TodayResponse, TodayReviewTaskRow } from '../types'

const session = useSessionStore()
const catalog = useCatalogStore()
const offline = useOfflineSnapshotStore()
const { online } = useOnlineStatus()

const date = ref(todayIso())
const today = ref<TodayResponse | null>(null)
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const dataFromCache = ref(false)
const recordFormRef = ref<InstanceType<typeof RecordForm> | null>(null)

const plannedTasks = computed(() => today.value?.plannedTasks ?? [])
const records = computed(() => today.value?.records ?? [])
const reviewTasks = computed(() => today.value?.reviewTasks ?? [])
/** 后端给出的缺失输入说明（例如未设置预算/额度），原样展示。 */
const warnings = computed(() => today.value?.warnings ?? [])
const studyMinutes = computed(() => today.value?.studyMinutes ?? null)
const dailyStudyMinutes = computed(() => today.value?.dailyStudyMinutes ?? null)
const dailyReviewMinutes = computed(() => today.value?.dailyReviewMinutes ?? null)

/** 当天已有学习记录的任务 id（仅用于展示“已记录/未记录”）。 */
const recordedTaskIds = computed(() => {
  const ids = new Set<number>()
  for (const record of records.value) {
    if (record.taskId !== null && record.taskId !== undefined) {
      ids.add(record.taskId)
    }
  }
  return ids
})

/** 计划任务展示行：科目名用科目目录按 subjectId 补出（后端不返回 subjectName）。 */
const plannedRows = computed<TodayPlanTaskRow[]>(() =>
  plannedTasks.value.map((item) => ({
    ...item,
    subjectName: catalog.subjectNameOf(item.subjectId),
    hasRecord: recordedTaskIds.value.has(item.taskId)
  }))
)

const reviewRows = computed<TodayReviewTaskRow[]>(() =>
  reviewTasks.value.map((item) => ({
    ...item,
    subjectName: catalog.subjectNameOf(item.subjectId)
  }))
)

/** 当天还没有对应学习记录的计划条目（由后端返回的当天记录推得，不是后端字段）。 */
const unrecordedRows = computed(() => plannedRows.value.filter((item) => !item.hasRecord))

/** 记录表单的任务选项：来自今日安排，单位随任务带出。 */
const taskOptions = computed<LearningRecordTaskOption[]>(() =>
  plannedTasks.value.map((task) => ({
    id: task.taskId,
    title: task.taskTitle,
    subjectId: task.subjectId ?? null,
    subjectName: catalog.subjectNameOf(task.subjectId),
    unitId: task.unitId ?? null,
    unitName: task.unitName ?? null
  }))
)

const lastSyncText = computed(() =>
  offline.syncedAt ? formatDateTime(offline.syncedAt) : '尚无同步记录'
)

const cacheNotice = computed(() => `数据来自离线快照（同步于 ${lastSyncText.value}）`)

/** 离线或请求失败时使用快照；快照只覆盖“今天”。 */
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
    const response = await fetchToday(date.value)
    today.value = response
    dataFromCache.value = false
    if (session.username && response.date === todayIso()) {
      // 只缓存今日切片；其它数据不在离线范围内。
      await offline.save({ userName: session.username, today: response })
    }
  } catch (caught) {
    const apiError = toApiError(caught)
    const cached = await loadFromCache()
    if (!cached) {
      errorMessage.value = apiError.message
    }
  } finally {
    loading.value = false
  }
}

function onDateChange(value: unknown): void {
  date.value = typeof value === 'string' && value ? value : todayIso()
  void loadToday()
}

/** 从“尚未记录”的条目带入任务，减少填写步骤。 */
function focusRecord(item: TodayPlanTaskRow): void {
  const model: Partial<LearningRecordFormModel> = {
    recordDate: date.value,
    taskId: item.taskId,
    subjectId: item.subjectId ?? null,
    unitId: item.unitId ?? null
  }
  recordFormRef.value?.fillFrom(model)
  ElMessage.info('已带入该任务，请填写完成量与用时')
}

async function handleRecordSubmit(payload: LearningRecordFormSubmission): Promise<void> {
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
    // 只有后端确认成功后才提示成功
    ElMessage.success('已记录，进度已更新')
    recordFormRef.value?.reset()
    await loadToday()
  } catch (caught) {
    const apiError = toApiError(caught)
    recordFormRef.value?.setServerError(apiError.message, apiError.fieldErrors)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void catalog.load()
  void loadToday()
})
</script>

<template>
  <div class="page">
    <div class="page__header">
      <div>
        <h2 class="page__title">今天做什么、完成多少</h2>
        <p class="page__subtitle">
          学习时长与每日预算/额度分别展示；不同单位不换算、不相加，也不生成总体完成百分比。
        </p>
      </div>
      <div class="page__actions">
        <ElDatePicker
          :model-value="date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          @update:model-value="onDateChange"
        />
        <ElButton :loading="loading" @click="loadToday">刷新</ElButton>
      </div>
    </div>

    <ElAlert
      v-if="dataFromCache"
      type="info"
      show-icon
      :closable="false"
      :title="cacheNotice"
      description="离线为只读模式，无法提交学习记录；联网后刷新即可回到实时数据。"
    />
    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <ElAlert
      v-if="warnings.length > 0"
      type="warning"
      show-icon
      :closable="false"
      title="后端提示（缺少的输入）"
    >
      <ul class="warning-list">
        <li v-for="warning in warnings" :key="warning">{{ warning }}</li>
      </ul>
    </ElAlert>

    <section class="section-card">
      <h3 class="section-card__title">今日概览</h3>
      <p class="section-card__hint">下面各项分别统计，互不折算；“未设置”表示待定项还没确定，不是 0。</p>
      <div class="metric-grid">
        <div>
          <div class="metric-label">学习时长</div>
          <div class="metric-value">{{ formatMinutes(studyMinutes) }}</div>
        </div>
        <div>
          <div class="metric-label">每日学习预算</div>
          <div class="metric-value">{{ formatBudget(dailyStudyMinutes) }}</div>
        </div>
        <div>
          <div class="metric-label">每日复习额度</div>
          <div class="metric-value">{{ formatBudget(dailyReviewMinutes) }}</div>
        </div>
      </div>
    </section>

    <section class="section-card">
      <h3 class="section-card__title">今日计划任务</h3>
      <p class="section-card__hint">
        计划量来自今日安排，完成量来自学习记录；未完成任务不会自动顺延到次日。
      </p>
      <PlannedTaskTable :tasks="plannedRows" :loading="loading" />
    </section>

    <section class="section-card">
      <h3 class="section-card__title">尚未记录的计划条目</h3>
      <p class="section-card__hint">
        按后端返回的当天学习记录判断这些条目今天还没有对应记录，点“去记录”可直接带入任务。
      </p>
      <EmptyState
        v-if="unrecordedRows.length === 0"
        description="今日计划条目都已有记录"
        hint="如果还有计划外学习，可以直接在下方快速记录里填写。"
      />
      <ul v-else class="unrecorded-list">
        <li v-for="item in unrecordedRows" :key="item.id" class="unrecorded-list__item">
          <div class="unrecorded-list__main">
            <span class="unrecorded-list__title">{{ item.taskTitle }}</span>
            <ElTag v-if="item.subjectName" size="small" effect="plain">{{ item.subjectName }}</ElTag>
            <span class="hint-text">
              计划 <AmountUnit :amount="item.plannedAmount" :unit="item.unitName" />
            </span>
          </div>
          <ElButton size="small" type="primary" plain @click="focusRecord(item)">去记录</ElButton>
        </li>
      </ul>
    </section>

    <section class="section-card">
      <h3 class="section-card__title">今日复习任务</h3>
      <p class="section-card__hint">只有经本人确认的复习建议才会进入今日安排。</p>
      <ReviewedTaskList :tasks="reviewRows" />
    </section>

    <section class="section-card">
      <h3 class="section-card__title">快速记录（目标 3—5 分钟完成）</h3>
      <p class="section-card__hint">
        日期默认今天；完成量必须带单位（单位跟随任务）；复盘可选。保存失败会保留已填内容。
      </p>
      <RecordForm
        ref="recordFormRef"
        :tasks="taskOptions"
        :submitting="submitting"
        @submit="handleRecordSubmit"
      />
    </section>
  </div>
</template>

<style scoped>
.unrecorded-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.unrecorded-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--kaoyan-border);
}

.unrecorded-list__item:last-child {
  border-bottom: none;
}

.unrecorded-list__main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.unrecorded-list__title {
  font-weight: 500;
}

.warning-list {
  margin: 0;
  padding-left: 18px;
}
</style>
