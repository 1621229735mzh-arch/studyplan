<script setup lang="ts">
/**
 * 进度看板：各科进度对比 + 学习量与时长趋势。
 *
 * 前端只负责展示：所有数字来自后端。后端按“科目 + 单位”成组返回，
 * 不提供跨单位的合计或总体完成百分比，因此这里也不做汇总、不做减法。
 * 离线时只回退到“进度概览”快照（趋势不在离线范围内）。
 */
import { computed, onMounted, ref } from 'vue'
import { ElAlert, ElButton, ElDatePicker, ElTable, ElTableColumn } from 'element-plus'

import { toApiError } from '@/api/http'
import AmountUnit from '@/components/AmountUnit.vue'
import EmptyState from '@/components/EmptyState.vue'
import { addDays, formatDateTime, formatMinutesOr, todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import { useSessionStore } from '@/stores/session'
import { getProgressOverview, getProgressTrend } from '../api'
import SubjectProgressChart from '../components/SubjectProgressChart.vue'
import TrendChart from '../components/TrendChart.vue'
import type { ProgressOverview, TrendPointView } from '../types'

const session = useSessionStore()
const offline = useOfflineSnapshotStore()
const { online } = useOnlineStatus()

const from = ref(addDays(todayIso(), -29))
const to = ref(todayIso())

const overview = ref<ProgressOverview | null>(null)
const trend = ref<TrendPointView[]>([])
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const dataFromCache = ref(false)

const rows = computed(() => overview.value?.items ?? [])
const trendPoints = computed(() => trend.value)

const lastSyncText = computed(() =>
  offline.syncedAt ? formatDateTime(offline.syncedAt) : '尚无同步记录'
)

async function loadFromCache(): Promise<boolean> {
  await offline.load()
  dataFromCache.value = true
  trend.value = []
  if (offline.progress === null) {
    overview.value = null
    return false
  }
  overview.value = offline.progress
  return true
}

async function load(): Promise<void> {
  errorMessage.value = null
  if (!online.value) {
    const cached = await loadFromCache()
    if (!cached) {
      errorMessage.value = '离线状态且本地没有进度概览快照，请联网后刷新。'
    }
    return
  }
  loading.value = true
  try {
    const [overviewResponse, trendResponse] = await Promise.all([
      getProgressOverview({ from: from.value, to: to.value }),
      getProgressTrend(from.value, to.value)
    ])
    overview.value = overviewResponse
    // /api/progress/trend 返回裸数组
    trend.value = trendResponse
    dataFromCache.value = false
    if (session.username) {
      // 离线范围只包含进度概览（不含趋势）
      await offline.save({ userName: session.username, progress: overviewResponse })
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

function onFromChange(value: unknown): void {
  from.value = typeof value === 'string' && value ? value : addDays(to.value, -29)
}

function onToChange(value: unknown): void {
  to.value = typeof value === 'string' && value ? value : todayIso()
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="page">
    <div class="page__header">
      <div>
        <h2 class="page__title">进度看板</h2>
        <p class="page__subtitle">
          数字全部来自后端统计。不同计量单位不混加，后端不提供跨单位合计，界面也不计算总体百分比。
        </p>
      </div>
      <div class="page__actions">
        <ElDatePicker
          :model-value="from"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
          @update:model-value="onFromChange"
        />
        <span class="hint-text">至</span>
        <ElDatePicker
          :model-value="to"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
          @update:model-value="onToChange"
        />
        <ElButton type="primary" :loading="loading" @click="load">查询</ElButton>
      </div>
    </div>

    <ElAlert
      v-if="dataFromCache"
      type="info"
      show-icon
      :closable="false"
      :title="`数据来自离线快照（同步于 ${lastSyncText}）`"
      description="离线只缓存进度概览，趋势图需要联网查看。"
    />
    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <ElAlert
      v-if="overview"
      type="info"
      show-icon
      :closable="false"
      :title="`统计区间：${overview?.from} ~ ${overview?.to}`"
      description="学习时长与完成量按“科目 + 单位”分别展示；计划量未设置时显示“未设置”，不当作 0。"
    />

    <section class="section-card">
      <h3 class="section-card__title">各科进度（计划 vs 完成，按单位）</h3>
      <p class="section-card__hint">按单位分别对比；切换单位查看，不会把不同单位放在同一张图里。</p>
      <SubjectProgressChart :items="rows" />
    </section>

    <section class="section-card">
      <h3 class="section-card__title">明细</h3>
      <EmptyState v-if="rows.length === 0" description="暂无进度明细" hint="先创建任务并记录学习，进度会在这里汇总。" />
      <ElTable v-else :data="rows" size="small" stripe>
        <ElTableColumn label="科目" prop="subjectName" min-width="120" />
        <ElTableColumn label="单位" prop="unitName" width="90" />
        <ElTableColumn label="计划量" width="140">
          <template #default="{ row }">
            <AmountUnit :amount="row.plannedAmount ?? null" :unit="row.unitName" placeholder="未设置" />
          </template>
        </ElTableColumn>
        <ElTableColumn label="完成量" width="140">
          <template #default="{ row }">
            <AmountUnit :amount="row.completedAmount ?? null" :unit="row.unitName" />
          </template>
        </ElTableColumn>
        <ElTableColumn label="学习时长" width="130">
          <template #default="{ row }">{{ formatMinutesOr(row.studyMinutes, '未记录') }}</template>
        </ElTableColumn>
      </ElTable>
    </section>

    <section class="section-card">
      <h3 class="section-card__title">趋势（{{ from }} ~ {{ to }}）</h3>
      <p class="section-card__hint">时长与完成量分开画；完成量按所选单位展示。</p>
      <TrendChart :points="trendPoints" />
    </section>
  </div>
</template>
