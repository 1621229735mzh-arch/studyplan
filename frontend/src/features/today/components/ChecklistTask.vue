<script setup lang="ts">
import { ElButton } from 'element-plus'
import { compactMinutes } from '../time'
import type { TodayChecklistTask } from '../types'
defineProps<{ task: TodayChecklistTask; readonly: boolean }>()
defineEmits<{ action: [task: TodayChecklistTask, complete: boolean] }>()
</script>
<template>
  <li class="task-row" :class="{ 'task-row--done': task.completionPercent >= 100 }">
    <button v-if="task.completionPercent < 100" class="task-check" :disabled="readonly || !task.actionable"
      :aria-label="`完成：${task.title}`" @click="$emit('action', task, true)"></button>
    <span v-else class="task-check task-check--done" aria-label="已完成">✓</span>
    <div class="task-main">
      <div class="task-title">{{ task.title }}</div>
      <div class="task-meta">
        <span class="task-kind" :class="{ 'task-kind--review': task.kind === 'REVIEW' }">{{ task.kind === 'REVIEW' ? '复习' : '学习' }}</span>
        <span v-if="task.completionPercent > 0">已完成 {{ Math.round(task.completionPercent * 10) / 10 }}%</span>
        <span v-if="task.completionPercent < 100">{{ task.remainingMinutes == null ? '预计用时待补充' : `预计剩余 ${compactMinutes(task.remainingMinutes)}` }}</span>
        <span v-if="!task.actionable && task.completionPercent < 100">安排已取消或调整</span>
      </div>
    </div>
    <div v-if="task.completionPercent < 100" class="task-actions">
      <ElButton size="small" type="primary" plain :disabled="readonly || !task.actionable" @click="$emit('action', task, true)">完成</ElButton>
      <ElButton size="small" :disabled="readonly || !task.actionable" @click="$emit('action', task, false)">剩余</ElButton>
    </div>
  </li>
</template>
<style scoped>
.task-row{display:flex;align-items:center;gap:14px;padding:17px 0;border-top:1px solid var(--kaoyan-border)}
.task-check{flex-shrink:0;display:grid;place-items:center;width:21px;height:21px;border:1.5px solid #a9b4ab;background:transparent;border-radius:6px;cursor:pointer;color:#fff}
.task-check:disabled{cursor:default;opacity:.5}.task-check--done{background:var(--kaoyan-primary,#527d65);border-color:transparent}
.task-main{min-width:0;flex:1}.task-title{font-size:15px;line-height:1.6;overflow-wrap:anywhere}.task-meta{display:flex;flex-wrap:wrap;align-items:center;gap:10px;margin-top:6px;font-size:12px;color:var(--kaoyan-text-secondary)}
.task-kind{font-size:11px;border-radius:4px;padding:1px 6px;background:#edf2ee;color:#53745d}.task-kind--review{background:#f3edf8;color:#826199}
.task-row--done .task-title{color:var(--kaoyan-text-secondary)}.task-actions{display:flex;gap:6px}.task-actions :deep(.el-button+.el-button){margin-left:0}
@media(max-width:600px){.task-row{gap:10px;flex-wrap:wrap}.task-actions{padding-left:31px;width:100%;justify-content:flex-end}.task-title{font-size:14px}}
</style>
