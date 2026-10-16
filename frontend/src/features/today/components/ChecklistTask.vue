<script setup lang="ts">
import { ElButton } from 'element-plus'
import { compactMinutes } from '../time'
import type { TodayChecklistTask } from '../types'
defineProps<{ task: TodayChecklistTask; readonly: boolean; showEstimate?: boolean }>()
defineEmits<{ action: [task: TodayChecklistTask, complete: boolean] }>()
</script>
<template>
  <li class="task-row" :class="{ 'task-row--done': task.completionPercent >= 100 }">
    <button v-if="task.activity?.mode === 'CHECK_ONLY'" class="task-check" :class="{'task-check--done':task.completionPercent >= 100}" role="checkbox" :aria-checked="task.completionPercent >= 100" :aria-label="task.title" :disabled="readonly || !task.actionable" @click="$emit('action',task,task.completionPercent < 100)">{{ task.completionPercent >= 100 ? '✓' : '' }}</button>
    <button v-else-if="task.completionPercent < 100" class="task-check" :disabled="readonly || !task.actionable"
      :aria-label="`完成：${task.title}`" @click="$emit('action', task, true)"></button>
    <span v-else class="task-check task-check--done" aria-label="已完成">✓</span>
    <div class="task-main">
      <div class="task-title">{{ task.title }}</div>
      <p v-if="task.activity?.mode === 'CHECK_ONLY'" class="sequence-item">{{ task.activity.description }}<small>只确认是否完成 · 无需数量或时长 · 不增加新学量</small></p>
      <p v-else-if="task.activity && !task.continuation" class="sequence-item">{{ task.activity.description }}<small>{{ task.activity.mode === 'RECAP' ? '不重复增加完成量' : '完成量据实记录' }}</small></p>
      <p v-if="task.continuation && task.activity?.description" class="sequence-item">{{ task.activity.description }}</p>
      <p v-if="task.sequence" class="sequence-item">
        <a v-if="task.sequence.dayItem" :href="task.sequence.dayItem.url" target="_blank" rel="noopener noreferrer">{{ task.sequence.dayItem.title }} ↗</a>
        <span v-else>题目清单已全部完成</span>
        <small>每天 1 题 · 完成后推进 · 漏做不积压</small>
      </p>
      <div class="task-meta">
        <span class="task-kind" :class="{ 'task-kind--review': task.kind === 'REVIEW' || task.activity?.mode === 'RECAP' || task.activity?.mode === 'CHECK_ONLY' }">{{ task.activity?.mode === 'CHECK_ONLY' ? '完成确认' : task.activity?.mode === 'RECAP' ? '复盘' : task.kind === 'REVIEW' ? '复习' : '学习' }}</span>
        <span v-if="task.completionPercent > 0">{{ task.activity?.mode === 'CHECK_ONLY' ? '已完成' : task.activity && !task.continuation ? '本次已结束' : `已完成 ${Math.round(task.completionPercent * 10) / 10}%` }}</span>
        <span v-if="showEstimate && task.kind === 'LEARNING' && task.activity?.mode !== 'CHECK_ONLY' && task.completionPercent < 100 && task.remainingMinutes != null" class="task-estimate">预计剩余 {{ compactMinutes(task.remainingMinutes) }}</span>
        <span v-if="!task.actionable && task.completionPercent < 100">{{ task.sequence ? '当前题目已完成或不在计划日期内' : '安排已取消或调整' }}</span>
      </div>
    </div>
    <div v-if="task.activity?.mode === 'CHECK_ONLY'" class="task-actions"><ElButton size="small" :disabled="readonly || !task.actionable" @click="$emit('action',task,task.completionPercent < 100)">{{ task.completionPercent >= 100 ? '取消完成' : '确认完成' }}</ElButton></div>
    <div v-else-if="task.completionPercent < 100" class="task-actions">
      <ElButton size="small" type="primary" plain :disabled="readonly || !task.actionable" @click="$emit('action', task, true)">{{ task.activity && !task.continuation ? '结束本次' : '完成' }}</ElButton>
      <ElButton size="small" :disabled="readonly || !task.actionable" @click="$emit('action', task, false)">{{ task.activity && !task.continuation ? '记一笔' : task.sequence ? '记尝试' : '剩余' }}</ElButton>
    </div>
  </li>
</template>
<style scoped>
.sequence-item{margin:6px 0;font-size:13px}.sequence-item small{display:block;margin-top:4px;color:var(--kaoyan-text-secondary)}
.task-row{display:flex;align-items:center;gap:14px;padding:17px 0;border-top:1px solid var(--kaoyan-border)}
.task-check{flex-shrink:0;display:grid;place-items:center;width:21px;height:21px;border:1.5px solid #a9b4ab;background:transparent;border-radius:6px;cursor:pointer;color:#fff}
.task-check:disabled{cursor:default;opacity:.5}.task-check--done{background:var(--kaoyan-primary,#527d65);border-color:transparent}
.task-main{min-width:0;flex:1}.task-title{font-size:15px;line-height:1.6;overflow-wrap:anywhere}.task-meta{display:flex;flex-wrap:wrap;align-items:center;gap:10px;margin-top:6px;font-size:12px;color:var(--kaoyan-text-secondary)}
.task-kind{font-size:11px;border-radius:4px;padding:1px 6px;background:#edf2ee;color:#53745d}.task-kind--review{background:#f3edf8;color:#826199}
.task-row--done .task-title{color:var(--kaoyan-text-secondary)}.task-actions{display:flex;gap:6px}.task-actions :deep(.el-button+.el-button){margin-left:0}
@media(max-width:600px){.task-row{gap:10px;flex-wrap:wrap}.task-actions{padding-left:31px;width:100%;justify-content:flex-end}.task-title{font-size:14px}}
</style>
