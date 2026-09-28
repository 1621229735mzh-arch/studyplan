<script setup lang="ts">
/**
 * 今日计划任务表：今日计划量 / 今日完成量 / 来源。
 *
 * 完成量来自后端（学习记录累计），剩余量后端不返回，因此这里不做减法推算。
 */
import { ElTable, ElTableColumn, ElTag } from 'element-plus'

import AmountUnit from '@/components/AmountUnit.vue'
import EmptyState from '@/components/EmptyState.vue'
import { planSourceLabel } from '@/composables/useFormat'
import type { TodayPlanTaskRow } from '../types'

defineProps<{
  tasks: TodayPlanTaskRow[]
  loading?: boolean
}>()
</script>

<template>
  <div>
    <EmptyState
      v-if="!loading && tasks.length === 0"
      description="今天还没有计划安排"
      hint="到“计划”页面把任务分配到今天；未完成的任务不会自动顺延，需要自己决定如何处理。"
    />
    <ElTable v-else :data="tasks" size="small" :border="false" stripe>
      <ElTableColumn label="任务" min-width="200">
        <template #default="{ row }">
          <div class="planned-task__title">{{ row.taskTitle }}</div>
          <div class="planned-task__meta">
            <ElTag v-if="row.subjectName" size="small" effect="plain">{{ row.subjectName }}</ElTag>
            <ElTag size="small" type="info" effect="plain">{{ planSourceLabel(row.source) }}</ElTag>
            <ElTag v-if="row.reviewItemId" size="small" type="warning" effect="plain">来自复习</ElTag>
          </div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="今日计划" width="140">
        <template #default="{ row }">
          <AmountUnit :amount="row.plannedAmount" :unit="row.unitName" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="今日完成" width="140">
        <template #default="{ row }">
          <AmountUnit :amount="row.completedAmount ?? null" :unit="row.unitName" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="记录状态" width="110">
        <template #default="{ row }">
          <ElTag :type="row.hasRecord ? 'success' : 'info'" size="small" effect="plain">
            {{ row.hasRecord ? '已记录' : '未记录' }}
          </ElTag>
        </template>
      </ElTableColumn>
    </ElTable>
  </div>
</template>

<style scoped>
.planned-task__title {
  font-weight: 500;
}

.planned-task__meta {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  margin-top: 2px;
}
</style>
