<script setup lang="ts">
/**
 * 周计划面板。
 *
 * 对应后端 WeeklyPlanController：
 * - `GET /api/plan/weeks/{weekStart}` 一定成功（后端会创建空周计划），所以不需要处理 404；
 * - 安排/调整某任务本周计划量：`PUT /api/plan/weeks/{weekStart}/items`，body `{taskId, plannedAmount}`；
 * - 移出本周：`DELETE /api/plan/weeks/{weekStart}/items/{taskId}`；
 * - 没有“整周替换”接口，也没有单独保存备注的接口，因此备注只读展示。
 *
 * 写操作都返回刷新后的周计划，前端直接采用服务端结果，不自行拼装。
 * 周计划只做“安排”，不产生完成量（完成量来自学习记录）。
 */
import { computed, onMounted, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
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
import { addDays, formatDateTime, formatIsoDate, todayIso, weekStartOf } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import { useCatalogStore } from '@/stores/catalog'
import { useSessionStore } from '@/stores/session'
import { addOrUpdateWeekPlanItem, getWeekPlan, listPlanTasks, removeWeekPlanItem } from '../api'
import type { PlanTask, WeekPlanResponse } from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()
const offline = useOfflineSnapshotStore()
const session = useSessionStore()

const weekStart = ref(weekStartOf(todayIso()))
const weekPlan = ref<WeekPlanResponse | null>(null)
const tasks = ref<PlanTask[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
/** 数据是否来自离线快照（离线只读） */
const dataFromCache = ref(false)
const assignTaskId = ref<number | null>(null)
const assignAmount = ref<number | null>(null)

const weekEnd = computed(() => addDays(weekStart.value, 6))
const items = computed(() => weekPlan.value?.items ?? [])

const lastSyncText = computed(() =>
  offline.syncedAt ? formatDateTime(offline.syncedAt) : '尚无同步记录'
)

const cacheNotice = computed(() => `数据来自离线快照（同步于 ${lastSyncText.value}）`)

async function load(): Promise<void> {
  if (!online.value) {
    // 离线只读：当前周的周计划在离线缓存范围内（今日、本周、进度概览）。
    await offline.load()
    const cached = offline.week
    if (cached !== null && cached.weekStartDate === weekStart.value) {
      weekPlan.value = cached
      dataFromCache.value = true
      errorMessage.value = null
    } else {
      weekPlan.value = null
      dataFromCache.value = true
      errorMessage.value = '离线状态且本地没有这一周的周计划快照，请联网后刷新。'
    }
    return
  }
  loading.value = true
  errorMessage.value = null
  try {
    const [plan, taskList] = await Promise.all([getWeekPlan(weekStart.value), listPlanTasks()])
    weekPlan.value = plan
    tasks.value = taskList
    dataFromCache.value = false
    if (session.username) {
      // 只缓存本周切片，离线时用于只读查看
      await offline.save({ userName: session.username, week: plan })
    }
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function shiftWeek(days: number): void {
  weekStart.value = addDays(weekStart.value, days)
  void load()
}

function onWeekChange(value: unknown): void {
  if (typeof value === 'string' && value) {
    weekStart.value = weekStartOf(value)
    void load()
  }
}

function onAssignTaskChange(value: unknown): void {
  assignTaskId.value = typeof value === 'number' ? value : null
}

function onAssignAmountChange(value: unknown): void {
  assignAmount.value = typeof value === 'number' ? value : null
}

/** 统一处理写操作的结果与错误（后端返回刷新后的周计划）。 */
async function applyWrite(action: () => Promise<WeekPlanResponse>, successText: string): Promise<void> {
  if (!online.value) {
    ElMessage.warning('离线状态无法修改周计划，第一版离线为只读。')
    return
  }
  submitting.value = true
  errorMessage.value = null
  try {
    weekPlan.value = await action()
    ElMessage.success(successText)
  } catch (caught) {
    const apiError = toApiError(caught)
    errorMessage.value =
      apiError.status === 409 ? '周计划已在其它设备被修改，请刷新后重试。' : apiError.message
  } finally {
    submitting.value = false
  }
}

async function assignTaskToWeek(): Promise<void> {
  if (assignTaskId.value === null) {
    ElMessage.warning('请先选择要分配到本周的任务')
    return
  }
  if (assignAmount.value === null || assignAmount.value <= 0) {
    ElMessage.warning('请填写本周计划量（需大于 0）')
    return
  }
  const taskId = assignTaskId.value
  const plannedAmount = assignAmount.value
  await applyWrite(
    () => addOrUpdateWeekPlanItem(weekStart.value, { taskId, plannedAmount }),
    '已把任务分配到本周'
  )
  assignAmount.value = null
}

/** 调整某任务在本周的计划量（同一接口：重复安排即更新计划量）。 */
function onRowAmountChange(row: { taskId: number; plannedAmount: number }, value: unknown): void {
  const amount = typeof value === 'number' ? value : row.plannedAmount
  if (amount === row.plannedAmount) {
    return
  }
  if (amount <= 0) {
    ElMessage.warning('本周计划量需大于 0')
    return
  }
  void applyWrite(
    () => addOrUpdateWeekPlanItem(weekStart.value, { taskId: row.taskId, plannedAmount: amount }),
    '本周计划量已调整'
  )
}

/** 从本周移除任务（不会影响已有学习记录）。 */
async function removeFromWeek(taskId: number): Promise<void> {
  if (!online.value) {
    ElMessage.warning('离线状态无法修改周计划。')
    return
  }
  try {
    await ElMessageBox.confirm('移出本周不会删除任务本身，也不会影响已有学习记录。确定移除吗？', '从本周移除', {
      type: 'warning',
      confirmButtonText: '移除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await applyWrite(() => removeWeekPlanItem(weekStart.value, taskId), '已从本周移除该任务')
}

onMounted(() => {
  void catalog.load()
  void load()
})
</script>

<template>
  <div>
    <div class="table-toolbar">
      <ElButton @click="shiftWeek(-7)">上一周</ElButton>
      <ElDatePicker
        :model-value="weekStart"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="选择该周任意一天"
        @update:model-value="onWeekChange"
      />
      <ElButton @click="shiftWeek(7)">下一周</ElButton>
      <ElButton :loading="loading" @click="load">刷新</ElButton>
      <span class="hint-text">本周：{{ formatIsoDate(weekStart) }} ~ {{ formatIsoDate(weekEnd) }}</span>
    </div>

    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <ElAlert
      v-if="dataFromCache"
      type="info"
      show-icon
      :closable="false"
      :title="cacheNotice"
      description="离线为只读模式，无法修改周计划；联网后刷新即可回到实时数据。"
    />

    <ElAlert
      type="info"
      show-icon
      :closable="false"
      title="周计划只是安排，不产生完成量"
      description="完成量来自学习记录；未完成的任务不会自动顺延到下一周或次日，需要本人手动调整。"
    />

    <div class="week-plan__assign">
      <ElSelect
        :model-value="assignTaskId ?? undefined"
        class="week-plan__select"
        clearable
        filterable
        placeholder="选择任务"
        @update:model-value="onAssignTaskChange"
      >
        <ElOption v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
      </ElSelect>
      <ElInputNumber
        :model-value="assignAmount ?? undefined"
        :min="0"
        :step="1"
        controls-position="right"
        placeholder="本周计划量"
        @update:model-value="onAssignAmountChange"
      />
      <ElButton type="primary" :loading="submitting" :disabled="!online" @click="assignTaskToWeek">
        分配到本周
      </ElButton>
      <span class="hint-text">同一任务重复安排会更新本周计划量；单位沿用任务本身的计量单位。</span>
    </div>

    <p v-if="weekPlan?.note" class="hint-text">本周备注：{{ weekPlan.note }}</p>

    <EmptyState
      v-if="!loading && items.length === 0"
      description="本周还没有分配任务"
      hint="先从上面的下拉框选择任务并填写本周计划量。"
    />

    <ElTable v-else :data="items" size="small" stripe>
      <ElTableColumn label="任务" min-width="200">
        <template #default="{ row }">
          <div>{{ row.taskTitle }}</div>
          <div class="hint-text">
            <ElTag v-if="row.subjectId" size="small" effect="plain">
              {{ catalog.subjectNameOf(row.subjectId) ?? `科目 #${row.subjectId}` }}
            </ElTag>
            <ElTag v-if="row.unitName" size="small" type="info" effect="plain">单位：{{ row.unitName }}</ElTag>
          </div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="本周计划量" width="170">
        <template #default="{ row }">
          <ElInputNumber
            :model-value="row.plannedAmount"
            :min="0"
            :step="1"
            size="small"
            controls-position="right"
            @update:model-value="(value) => onRowAmountChange(row, value)"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn label="本周完成量" width="140">
        <template #default="{ row }">
          <AmountUnit :amount="row.completedAmount ?? null" :unit="row.unitName ?? null" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" text type="danger" @click="removeFromWeek(row.taskId)">移出本周</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <p class="hint-text">
      每日的具体分配量在“每日安排”页调整（可手动修改）；这里看到的是本周总量。
    </p>
  </div>
</template>

<style scoped>
.week-plan__assign {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 12px 0;
}

.week-plan__select {
  width: 260px;
}
</style>
