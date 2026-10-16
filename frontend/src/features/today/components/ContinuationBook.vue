<script setup lang="ts">
import ChecklistTask from './ChecklistTask.vue'
import type { TodayChecklistTask } from '../types'
defineProps<{ title: string; tasks: TodayChecklistTask[]; readonly: boolean; showEstimate?: boolean }>()
defineEmits<{ action: [task: TodayChecklistTask, complete: boolean] }>()
</script>
<template>
  <li class="book-task">
    <h3>{{ title }}</h3>
    <ul>
      <li v-for="task in tasks.filter(t => t.completionPercent < 100)" :key="task.id" class="book-content">
        <span class="content-origin">{{ task.continuation?.carried ? '剩余内容' : '新内容' }}</span>
        <ul><ChecklistTask :task="task" :readonly="readonly" :show-estimate="showEstimate" @action="(item,complete) => $emit('action',item,complete)" /></ul>
      </li>
    </ul>
    <details v-if="tasks.some(t => t.completionPercent >= 100)">
      <summary>已完成 · {{ tasks.filter(t => t.completionPercent >= 100).length }}</summary>
      <ul><ChecklistTask v-for="task in tasks.filter(t => t.completionPercent >= 100)" :key="task.id" :task="task" :readonly="readonly" @action="(item,complete) => $emit('action',item,complete)" /></ul>
    </details>
  </li>
</template>
<style scoped>
.book-task{border-top:1px solid var(--kaoyan-border);padding:16px 0}.book-task h3{font-size:15px;margin:0 0 12px;font-weight:600}
ul{list-style:none;padding:0;margin:0}.book-content{padding-left:12px;border-left:2px solid var(--kaoyan-border);margin-top:12px}
.content-origin,summary{font-size:12px;color:var(--kaoyan-text-secondary)}summary{cursor:pointer;padding:10px 0}
.book-content :deep(.task-row){border-top:0;padding:6px 0}
</style>
