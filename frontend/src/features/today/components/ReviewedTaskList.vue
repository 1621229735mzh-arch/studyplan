<script setup lang="ts">
/** 今日已确认的复习任务（只有本人确认过的建议才会出现在这里）。 */
import { ElTag } from 'element-plus'

import EmptyState from '@/components/EmptyState.vue'
import { formatIsoDate, formatMinutesOr, masteryLabel, reviewStatusLabel } from '@/composables/useFormat'
import type { TodayReviewTaskRow } from '../types'

defineProps<{
  tasks: TodayReviewTaskRow[]
}>()
</script>

<template>
  <div>
    <EmptyState
      v-if="tasks.length === 0"
      description="今天没有已确认的复习任务"
      hint="复习建议需要在“复习”页面确认后才会进入今日安排，未确认不会自动占用今天的时间。"
    />
    <ul v-else class="reviewed-list">
      <li v-for="task in tasks" :key="task.id" class="reviewed-list__item">
        <div class="reviewed-list__main">
          <span class="reviewed-list__title">{{ task.title }}</span>
          <ElTag v-if="task.subjectName" size="small" effect="plain">{{ task.subjectName }}</ElTag>
          <ElTag size="small" type="info" effect="plain">{{ reviewStatusLabel(task.status) }}</ElTag>
        </div>
        <div class="reviewed-list__meta">
          <span>预计用时：{{ formatMinutesOr(task.estimatedMinutes, '未提供') }}</span>
          <span>上次掌握反馈：{{ masteryLabel(task.lastResult ?? null) }}</span>
          <span v-if="task.intervalDays !== null && task.intervalDays !== undefined">
            间隔：{{ task.intervalDays }} 天
          </span>
          <span>下次复习：{{ formatIsoDate(task.nextReviewDate) }}</span>
          <span v-if="task.scheduledDate">已安排到：{{ formatIsoDate(task.scheduledDate) }}</span>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.reviewed-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.reviewed-list__item {
  padding: 10px 0;
  border-bottom: 1px dashed var(--kaoyan-border);
}

.reviewed-list__item:last-child {
  border-bottom: none;
}

.reviewed-list__main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.reviewed-list__title {
  font-weight: 500;
}

.reviewed-list__meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 4px;
  color: var(--kaoyan-text-secondary);
  font-size: 13px;
}
</style>
