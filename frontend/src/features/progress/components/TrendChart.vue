<script setup lang="ts">
/**
 * 趋势图：学习时长趋势 + 内容完成量趋势。
 *
 * 约束：
 * - 时长（分钟）与数量是两种量纲，分别画两张图，绝不放在同一系列里；
 * - 内容完成量按单位筛选后再画，不同单位不混在同一系列；
 * - 只画后端确实返回了数据的日期，不把缺失数据补成 0。
 */
import { computed, ref } from 'vue'
import { ElAlert, ElEmpty, ElOption, ElSelect } from 'element-plus'
import type { EChartsOption } from 'echarts'

import EChart from './EChart.vue'
import type { TrendPointView } from '../types'

const props = defineProps<{
  points: TrendPointView[]
}>()

const units = computed(() => {
  const seen = new Map<number, string>()
  for (const point of props.points) {
    for (const item of point.amounts) {
      if (!seen.has(item.unitId)) {
        seen.set(item.unitId, item.unitName)
      }
    }
  }
  return [...seen.entries()].map(([unitId, unitName]) => ({ unitId, unitName }))
})

const selectedUnitId = ref<number | null>(null)

const effectiveUnitId = computed<number | null>(() => {
  if (selectedUnitId.value !== null && units.value.some((unit) => unit.unitId === selectedUnitId.value)) {
    return selectedUnitId.value
  }
  return units.value.length > 0 ? units.value[0].unitId : null
})

const currentUnitName = computed(
  () => units.value.find((unit) => unit.unitId === effectiveUnitId.value)?.unitName ?? ''
)

/** 只保留当前单位下确实有完成量的日期（缺失不补 0）。 */
const amountPoints = computed(() =>
  props.points
    .map((point) => ({
      date: point.date,
      amount: point.amounts.find((item) => item.unitId === effectiveUnitId.value)?.amount ?? null
    }))
    .filter((point): point is { date: string; amount: number } => point.amount !== null)
)

const durationOption = computed<EChartsOption>(() => ({
  title: { text: '学习时长趋势（分钟）', left: 0, textStyle: { fontSize: 13 } },
  tooltip: { trigger: 'axis' },
  grid: { left: 56, right: 24, top: 48, bottom: 40 },
  xAxis: { type: 'category', data: props.points.map((point) => point.date) },
  yAxis: { type: 'value', name: '分钟' },
  series: [
    {
      name: '学习时长（分钟）',
      type: 'line',
      smooth: true,
      data: props.points.map((point) => point.studyMinutes)
    }
  ]
}))

const amountOption = computed<EChartsOption>(() => ({
  title: { text: `内容完成量趋势（${currentUnitName.value || '按单位'}）`, left: 0, textStyle: { fontSize: 13 } },
  tooltip: { trigger: 'axis' },
  grid: { left: 56, right: 24, top: 48, bottom: 40 },
  xAxis: { type: 'category', data: amountPoints.value.map((point) => point.date) },
  yAxis: { type: 'value', name: currentUnitName.value === '' ? '数量' : `数量（${currentUnitName.value}）` },
  series: [
    {
      name: `完成量（${currentUnitName.value || '未选择单位'}）`,
      type: 'line',
      smooth: true,
      data: amountPoints.value.map((point) => point.amount)
    }
  ]
}))

function onUnitChange(value: unknown): void {
  selectedUnitId.value = typeof value === 'number' ? value : null
}
</script>

<template>
  <div class="trend-chart">
    <ElEmpty v-if="points.length === 0" description="所选区间内没有数据" />

    <template v-else>
      <div class="trend-chart__toolbar">
        <span class="hint-text">完成量单位</span>
        <ElSelect
          :model-value="effectiveUnitId ?? undefined"
          class="trend-chart__select"
          placeholder="选择单位"
          @update:model-value="onUnitChange"
        >
          <ElOption v-for="unit in units" :key="unit.unitId" :label="unit.unitName" :value="unit.unitId" />
        </ElSelect>
        <span class="hint-text">时长与数量分开统计，不相互换算。</span>
      </div>

      <EChart :option="durationOption" height="300px" />
      <EChart v-if="units.length > 0" :option="amountOption" height="300px" />
      <ElAlert
        v-else
        type="info"
        show-icon
        :closable="false"
        title="所选区间内没有内容完成量数据"
        description="只有学习记录里填写了完成数量时，才会有完成量趋势。"
      />
    </template>
  </div>
</template>

<style scoped>
.trend-chart__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.trend-chart__select {
  width: 160px;
}
</style>
