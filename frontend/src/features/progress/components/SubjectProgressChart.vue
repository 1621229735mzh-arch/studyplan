<script setup lang="ts">
/**
 * 各科进度对比（计划 vs 完成，按单位）。
 *
 * 关键约束：不同单位不能画在同一条系列里，也不能相加。
 * 因此这里先按单位筛选，只展示同一种单位的科目对比，并在标题与图例里标明单位。
 * 计划量未设置（后端返回 null）时该科目不画“计划量”柱，而不是当作 0。
 */
import { computed, ref } from 'vue'
import { ElAlert, ElEmpty, ElOption, ElSelect } from 'element-plus'
import type { EChartsOption } from 'echarts'

import EChart from './EChart.vue'
import type { UnitProgressView } from '../types'

const props = defineProps<{
  items: UnitProgressView[]
}>()

/** 出现过的单位（按 unitId 去重）。 */
const units = computed(() => {
  const seen = new Map<number, string>()
  for (const item of props.items) {
    if (!seen.has(item.unitId)) {
      seen.set(item.unitId, item.unitName)
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

const currentItems = computed(() => props.items.filter((item) => item.unitId === effectiveUnitId.value))

/** 该单位下是否有科目还没设置计划量（null 表示未设置，不是 0）。 */
const hasMissingPlan = computed(() =>
  currentItems.value.some((item) => item.plannedAmount === null || item.plannedAmount === undefined)
)

const option = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['计划量', '完成量'] },
  grid: { left: 48, right: 24, top: 48, bottom: 40 },
  xAxis: {
    type: 'category',
    data: currentItems.value.map((item) => item.subjectName)
  },
  yAxis: {
    type: 'value',
    name: currentUnitName.value === '' ? '数量' : `数量（${currentUnitName.value}）`
  },
  series: [
    {
      name: '计划量',
      type: 'bar',
      // 未设置计划量的科目用 ECharts 的空值占位符 '-'（留空），不补 0
      // （补 0 会被误读成“计划为 0”）
      data: currentItems.value.map((item) => item.plannedAmount ?? '-')
    },
    {
      name: '完成量',
      type: 'bar',
      data: currentItems.value.map((item) => item.completedAmount ?? '-')
    }
  ]
}))

function onUnitChange(value: unknown): void {
  selectedUnitId.value = typeof value === 'number' ? value : null
}
</script>

<template>
  <div>
    <div class="subject-progress__toolbar">
      <span class="hint-text">计量单位</span>
      <ElSelect
        :model-value="effectiveUnitId ?? undefined"
        class="subject-progress__select"
        placeholder="选择单位"
        @update:model-value="onUnitChange"
      >
        <ElOption v-for="unit in units" :key="unit.unitId" :label="unit.unitName" :value="unit.unitId" />
      </ElSelect>
      <span class="hint-text">只展示同一种单位的科目对比，不同单位不混在同一张图里。</span>
    </div>

    <ElEmpty v-if="units.length === 0" description="暂无进度数据" />

    <template v-else>
      <EChart :option="option" height="340px" />
      <ElAlert
        type="info"
        show-icon
        :closable="false"
        :title="`当前单位：${currentUnitName}`"
        :description="
          hasMissingPlan
            ? '计划量与完成量由后端统计；部分科目尚未设置计划量，图上留空（未设置不等于 0）。'
            : '计划量与完成量由后端统计；修改或删除学习记录后图表会随之变化。'
        "
      />
    </template>
  </div>
</template>

<style scoped>
.subject-progress__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.subject-progress__select {
  width: 160px;
}
</style>
