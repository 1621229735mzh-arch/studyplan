<script setup lang="ts">
/**
 * 每日安排面板（手动调整每日计划量）。
 *
 * 对应后端 DailyPlanController：
 * - `GET /api/plan/days/{date}` 返回裸数组 DailyPlanItemView[]；
 * - 加入当天安排：`POST /api/plan/days/{date}/items`，body `{taskId, plannedAmount, source?}`；
 * - 手动调整当天计划量：`PUT /api/plan/days/{date}/items/{itemId}`，body `{plannedAmount, version, reason?}`；
 * - 移除当天安排：`DELETE /api/plan/days/{date}/items/{itemId}`。
 *
 * 三类写操作都返回刷新后的当天条目数组，前端直接采用服务端结果。
 * 计划量只是安排，不产生完成量；未完成不会自动顺延到次日。
 */
import { computed, onMounted, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
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
import { formatIsoDate, planSourceLabel, todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import { addDayPlanItem, adjustDayPlanItem, getDayPlan, listPlanTasks, removeDayPlanItem } from '../api'
import type { DailyPlanItemView, PlanTask } from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const date = ref(todayIso())
const items = ref<DailyPlanItemView[]>([])
const tasks = ref<PlanTask[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const noticeMessage = ref<string | null>(null)

const selectedTaskId = ref<number | null>(null)
const plannedAmount = ref<number | null>(null)
/** 调整原因（可选）：后端会在调整记录里保存它 */
const reason = ref('')

/** 当天已安排的任务（用于把“再次提交”变成“调整”）。 */
const existingItem = computed<DailyPlanItemView | null>(
  () => items.value.find((item) => item.taskId === selectedTaskId.value) ?? null
)

async function load(): Promise<void> {
  if (!online.value) {
    errorMessage.value = '离线状态无法读取每日安排。'
    return
  }
  loading.value = true
  errorMessage.value = null
  noticeMessage.value = null
  try {
    const [dayItems, taskList] = await Promise.all([getDayPlan(date.value), listPlanTasks()])
    items.value = dayItems
    tasks.value = taskList
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function onDateChange(value: unknown): void {
  date.value = typeof value === 'string' && value ? value : todayIso()
  void load()
}

function onTaskChange(value: unknown): void {
  selectedTaskId.value = typeof value === 'number' ? value : null
}

function onAmountChange(value: unknown): void {
  plannedAmount.value = typeof value === 'number' ? value : null
}

/** 写操作统一处理：服务端返回刷新后的当天条目数组。 */
async function applyWrite(action: () => Promise<DailyPlanItemView[]>, successText: string): Promise<void> {
  submitting.value = true
  errorMessage.value = null
  try {
    items.value = await action()
    ElMessage.success(successText)
    noticeMessage.value = '计划量已更新；完成量仍以学习记录为准。'
  } catch (caught) {
    const apiError = toApiError(caught)
    errorMessage.value =
      apiError.status === 409 ? '当天的安排已在其它设备被修改，请刷新后重试。' : apiError.message
  } finally {
    submitting.value = false
  }
}

/** 加入 / 调整当天安排。 */
async function submitItem(): Promise<void> {
  if (selectedTaskId.value === null) {
    ElMessage.warning('请先选择任务')
    return
  }
  if (plannedAmount.value === null || plannedAmount.value <= 0) {
    ElMessage.warning('请填写当天计划量（需大于 0）')
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法修改安排，第一版离线为只读。')
    return
  }

  const taskId = selectedTaskId.value
  const amount = plannedAmount.value
  const existing = existingItem.value

  if (existing !== null) {
    // 手动调整：直接调用后端的调整接口（会带上版本并记录调整），不做“先删后加”。
    try {
      await ElMessageBox.confirm(
        `该任务当天已有安排（计划 ${existing.plannedAmount}${existing.unitName}）。是否调整为 ${amount}${existing.unitName}？`,
        '调整当天计划量',
        { type: 'warning', confirmButtonText: '调整', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
    await applyWrite(
      () =>
        adjustDayPlanItem(date.value, existing.id, {
          plannedAmount: amount,
          version: existing.version,
          reason: reason.value.trim() === '' ? null : reason.value.trim()
        }),
      '当天计划量已调整'
    )
    return
  }

  await applyWrite(
    () => addDayPlanItem(date.value, { taskId, plannedAmount: amount }),
    '已加入当天安排'
  )
}

async function removeItem(item: DailyPlanItemView): Promise<void> {
  if (!online.value) {
    ElMessage.warning('离线状态无法修改安排。')
    return
  }
  try {
    await ElMessageBox.confirm(
      '移除后该任务的剩余工作量不会自动移到其它日期，需要本人重新安排。确定移除吗？',
      '从当天安排移除',
      { type: 'warning', confirmButtonText: '移除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await applyWrite(() => removeDayPlanItem(date.value, item.id), '已移除')
}

onMounted(() => {
  void catalog.load()
  void load()
})
</script>

<template>
  <div>
    <div class="table-toolbar">
      <ElDatePicker
        :model-value="date"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="选择日期"
        @update:model-value="onDateChange"
      />
      <ElButton :loading="loading" @click="load">刷新</ElButton>
      <span class="hint-text">{{ formatIsoDate(date) }}</span>
    </div>

    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />
    <ElAlert v-if="noticeMessage" type="success" show-icon :closable="false" :title="noticeMessage" />
    <ElAlert
      type="info"
      show-icon
      :closable="false"
      title="安排与完成量是两件事"
      description="这里只调整计划量；实际完成量来自学习记录。未完成的任务不会自动顺延到次日。"
    />

    <div class="day-plan__form">
      <ElSelect
        :model-value="selectedTaskId ?? undefined"
        class="day-plan__select"
        clearable
        filterable
        placeholder="选择任务"
        @update:model-value="onTaskChange"
      >
        <ElOption v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
      </ElSelect>
      <ElInputNumber
        :model-value="plannedAmount ?? undefined"
        :min="0"
        :step="1"
        controls-position="right"
        placeholder="当天计划量"
        @update:model-value="onAmountChange"
      />
      <ElInput
        v-model="reason"
        class="day-plan__reason"
        maxlength="500"
        placeholder="调整原因（可选，调整当天计划量时记录）"
      />
      <ElButton type="primary" :loading="submitting" :disabled="!online" @click="submitItem">
        加入 / 调整当天
      </ElButton>
      <span class="hint-text">单位沿用任务本身的计量单位，不需要填写。</span>
    </div>

    <ElAlert
      v-if="existingItem"
      type="warning"
      show-icon
      :closable="false"
      :title="`所选任务当天已有安排（计划 ${existingItem?.plannedAmount}${existingItem?.unitName}）`"
      description="再次提交会用新的计划量调整该条目（后端会记录调整与数据版本）。"
    />

    <EmptyState
      v-if="!loading && items.length === 0"
      description="当天还没有安排任务"
      hint="从上面的下拉框选择任务并填写当天计划量。"
    />

    <ElTable v-else :data="items" size="small" stripe>
      <ElTableColumn label="任务" min-width="200">
        <template #default="{ row }">
          <div>{{ row.taskTitle }}</div>
          <div class="hint-text">
            <ElTag v-if="row.subjectId" size="small" effect="plain">
              {{ catalog.subjectNameOf(row.subjectId) ?? `科目 #${row.subjectId}` }}
            </ElTag>
            <ElTag size="small" type="info" effect="plain">{{ planSourceLabel(row.source) }}</ElTag>
            <ElTag v-if="row.reviewItemId" size="small" type="warning" effect="plain">来自复习</ElTag>
          </div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="当天计划量" width="150">
        <template #default="{ row }">
          <AmountUnit :amount="row.plannedAmount" :unit="row.unitName ?? null" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="当天完成量" width="140">
        <template #default="{ row }">
          <AmountUnit :amount="row.completedAmount ?? null" :unit="row.unitName ?? null" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" text type="danger" @click="removeItem(row)">移除</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
  </div>
</template>

<style scoped>
.day-plan__form {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 12px 0;
}

.day-plan__select {
  width: 260px;
}

.day-plan__reason {
  width: 240px;
}
</style>
