<script setup lang="ts">
/** 学习记录列表：完成量必带单位，用时单独一列，不与完成量混算。 */
import { ElButton, ElTable, ElTableColumn, ElTag } from 'element-plus'

import AmountUnit from '@/components/AmountUnit.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatIsoDate, formatMinutes } from '@/composables/useFormat'
import type { LearningRecord } from '../types'

defineProps<{
  records: LearningRecord[]
  loading?: boolean
}>()

const emit = defineEmits<{
  (event: 'edit', record: LearningRecord): void
  (event: 'delete', record: LearningRecord): void
}>()
</script>

<template>
  <div>
    <EmptyState
      v-if="!loading && records.length === 0"
      description="没有符合条件的学习记录"
      hint="调整筛选条件，或在下方补记一条学习记录。"
    />
    <ElTable v-else :data="records" size="small" stripe>
      <ElTableColumn label="日期" width="130">
        <template #default="{ row }">{{ formatIsoDate(row.recordDate) }}</template>
      </ElTableColumn>
      <ElTableColumn label="任务 / 科目" min-width="200">
        <template #default="{ row }">
          <div>{{ row.taskTitle ?? '计划外学习' }}</div>
          <ElTag v-if="row.subjectName" size="small" effect="plain">{{ row.subjectName }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="完成量" width="140">
        <template #default="{ row }">
          <AmountUnit :amount="row.amount ?? null" :unit="row.unitName ?? null" />
        </template>
      </ElTableColumn>
      <ElTableColumn label="用时" width="130">
        <template #default="{ row }">{{ formatMinutes(row.durationMinutes) }}</template>
      </ElTableColumn>
      <ElTableColumn label="复盘" min-width="180">
        <template #default="{ row }">
          <span v-if="row.note">{{ row.note }}</span>
          <span v-else class="hint-text">—</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" text type="primary" @click="emit('edit', row)">编辑</ElButton>
          <ElButton size="small" text type="danger" @click="emit('delete', row)">删除</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
  </div>
</template>
