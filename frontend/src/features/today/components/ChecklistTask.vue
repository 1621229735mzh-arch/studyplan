<script setup lang="ts">
import { computed } from 'vue'
import { ElButton } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { compactMinutes } from '../time'
import type { TodayChecklistTask } from '../types'
const props = defineProps<{ task: TodayChecklistTask; readonly: boolean; showEstimate?: boolean }>()
const counted = computed(() => props.task.activity?.mode === 'COUNT_STUDY' || props.task.activity?.mode === 'COUNT_REVIEW')
const numbered = computed(() => props.task.sequence?.mode === 'SELECT_BY_NUMBER')
defineEmits<{ action: [task: TodayChecklistTask, complete: boolean] }>()
</script>
<template>
  <li class="task-row" :class="{ 'task-row--done': task.completionPercent >= 100 }">
    <button v-if="task.activity?.mode === 'CHECK_ONLY'" class="task-check" :class="{'task-check--done':task.completionPercent >= 100}" role="checkbox" :aria-checked="task.completionPercent >= 100" :aria-label="task.title" :disabled="readonly || !task.actionable" @click="$emit('action',task,task.completionPercent < 100)">{{ task.completionPercent >= 100 ? '✓' : '' }}</button>
    <button v-else-if="task.completionPercent < 100" class="task-check" :disabled="readonly || !task.actionable"
      :class="{ 'task-check--partial': task.completionPercent > 0 }"
      :aria-label="`完成：${task.title}`" @click="$emit('action', task, true)"></button>
    <span v-else class="task-check task-check--done" aria-label="已完成">✓</span>
    <div class="task-main">
      <div class="task-title">{{ task.title }}</div>
      <p v-if="task.activity?.mode === 'CHECK_ONLY'" class="sequence-item">{{ task.activity.description }}<small>只确认是否完成 · 无需数量或时长 · 不增加新学量</small></p>
      <p v-else-if="task.activity && !task.continuation" class="sequence-item">{{ task.activity.description }}<small>{{ task.activity.mode === 'COUNT_REVIEW' ? '复习量单独记录，不增加新词量' : task.activity.mode === 'RECAP' ? '不重复增加完成量' : '完成量据实记录' }}</small></p>
      <p v-if="task.continuation && task.activity?.description" class="sequence-item">{{ task.activity.description }}</p>
      <p v-if="task.sequence" class="sequence-item">
        <a v-if="task.sequence.dayItem && !numbered" :href="task.sequence.dayItem.url" target="_blank" rel="noopener noreferrer">{{ task.sequence.dayItem.title }} ↗</a>
        <span v-else>{{ task.sequence.mode === 'SELECT_BY_NUMBER' ? '填写今天实际做的题号' : '题目清单已全部完成' }}</span>
        <small>{{ numbered ? '自选题号 · 每天不限题数 · 可分次填写' : '每天 1 题 · 完成后推进 · 漏做不积压' }}</small>
      </p>
      <div class="task-meta">
        <span class="task-kind" :class="{ 'task-kind--review': task.kind === 'REVIEW' || task.activity?.mode === 'COUNT_REVIEW' || task.activity?.mode === 'RECAP' || task.activity?.mode === 'CHECK_ONLY' }">{{ task.activity?.mode === 'COUNT_REVIEW' ? '复习' : task.activity?.mode === 'CHECK_ONLY' ? '完成确认' : task.activity?.mode === 'RECAP' ? '复盘' : task.kind === 'REVIEW' ? '复习' : '学习' }}</span>
        <span v-if="counted">{{ task.activity?.mode === 'COUNT_REVIEW' ? '今日已复习' : '今日新学' }} {{ task.recordedAmount == null && task.completionPercent >= 100 ? '数量未记录' : `${task.recordedAmount ?? 0} ${task.unitName}` }}</span>
        <span v-if="numbered">今日已完成 {{ task.recordedAmount ?? 0 }} 题</span>
        <span v-if="task.completionPercent > 0 && !numbered">{{ counted ? '已记录' : task.activity?.mode === 'CHECK_ONLY' ? '已完成' : task.activity && !task.continuation ? '本次已结束' : `已完成 ${Math.round(task.completionPercent * 10) / 10}%` }}</span>
        <span v-if="showEstimate && task.kind === 'LEARNING' && task.activity?.mode !== 'CHECK_ONLY' && task.completionPercent < 100 && task.remainingMinutes != null" class="task-estimate">预计剩余 {{ compactMinutes(task.remainingMinutes) }}</span>
        <span v-if="!task.actionable && task.completionPercent < 100">{{ task.sequence ? '当前题目已完成或不在计划日期内' : '安排已取消或调整' }}</span>
      </div>
      <div v-if="!counted && (!task.activity || task.continuation) && !task.sequence && task.completionPercent > 0 && task.completionPercent < 100" class="task-progress" role="progressbar" :aria-label="`${task.title}完成进度`" :aria-valuenow="task.completionPercent" :aria-valuemin="0" :aria-valuemax="100"><span :style="{ width: `${Math.max(0, Math.min(100, task.completionPercent))}%` }"></span></div>
    </div>
    <div v-if="task.activity?.mode === 'CHECK_ONLY'" class="task-actions"><ElButton size="small" :disabled="readonly || !task.actionable" @click="$emit('action',task,task.completionPercent < 100)">{{ task.completionPercent >= 100 ? '取消完成' : '确认完成' }}</ElButton></div>
    <div v-else-if="counted" class="task-actions"><ElButton size="small" type="primary" plain :disabled="readonly || !task.actionable" @click="$emit('action',task,true)"><AppIcon name="edit" :size="20" />{{ task.completionPercent >= 100 ? '再记一笔' : '填写数量' }}</ElButton></div>
    <div v-else-if="numbered" class="task-actions"><ElButton size="small" type="primary" plain :disabled="readonly || !task.actionable" @click="$emit('action',task,true)"><AppIcon name="edit" :size="20" />{{ task.completionPercent >= 100 ? '继续填写' : '填写完成题号' }}</ElButton></div>
    <div v-else-if="task.completionPercent < 100" class="task-actions">
      <ElButton size="small" type="primary" plain :disabled="readonly || !task.actionable" @click="$emit('action', task, true)"><AppIcon name="check" :size="20" />{{ task.activity && !task.continuation ? '结束本次' : task.sequence?.mode === 'SELECT_BY_NUMBER' ? '填写题号' : '完成' }}</ElButton>
      <ElButton size="small" :disabled="readonly || !task.actionable" @click="$emit('action', task, false)"><AppIcon name="list" :size="18" />{{ task.activity && !task.continuation ? '记一笔' : task.sequence ? '记尝试' : '剩余' }}</ElButton>
    </div>
  </li>
</template>
<style scoped>
.sequence-item{margin:7px 0;font-size:13px;line-height:1.7}.sequence-item small{display:block;margin-top:4px;color:var(--kaoyan-text-secondary);font-size:11px}
.task-row{display:grid;grid-template-columns:44px minmax(0,1fr) auto;align-items:center;gap:18px;padding:26px 0;border-top:1px solid var(--kaoyan-border)}
.task-check{position:relative;isolation:isolate;display:grid;place-items:center;width:44px;height:44px;padding:0;border:0;background:transparent;color:var(--kaoyan-on-primary);font:inherit;font-size:19px;cursor:pointer}
.task-check::before{content:'';position:absolute;inset:3px;border:2.5px solid #9f7cbd;border-radius:50%;z-index:-1}
.task-check:hover:not(:disabled)::before{border-color:var(--kaoyan-primary)}.task-check:disabled{cursor:default;opacity:.45}.task-check--done::before{background:var(--kaoyan-primary);border-color:transparent}.task-check--partial::before{border-color:var(--kaoyan-primary)}.task-check--partial::after{content:'';width:7px;height:7px;border-radius:50%;background:var(--kaoyan-primary)}
.task-main{min-width:0}.task-title{font-size:clamp(21px,1.4vw,28px);line-height:1.6;font-weight:600;overflow-wrap:anywhere}.task-meta{display:flex;flex-wrap:wrap;align-items:center;gap:6px 10px;margin-top:8px;font-size:14px;color:#bdb7d2}
.task-kind{font-size:10px;border-radius:4px;padding:1px 6px;background:var(--kaoyan-surface);color:var(--kaoyan-text-secondary)}.task-kind--review{background:var(--kaoyan-primary-soft);color:var(--kaoyan-primary)}
.task-row--done .task-title{color:var(--kaoyan-text-secondary)}.task-actions{display:flex;gap:12px;flex-wrap:wrap;justify-content:flex-end;max-width:320px}.task-actions :deep(.el-button+.el-button){margin-left:0}.task-actions :deep(.el-button){min-height:clamp(50px,3.5vw,64px);min-width:clamp(112px,7.5vw,150px);padding:12px 18px;font-size:clamp(17px,1.15vw,22px);border-color:#8d73b0}.task-actions :deep(.el-button:not(.el-button--primary)){background:linear-gradient(130deg,#28232e99,#111119cc);color:#f3eafa}
.task-progress{height:10px;max-width:360px;margin-top:10px;border-radius:8px;background:#3e394f;overflow:hidden}.task-progress span{display:block;height:100%;background:linear-gradient(110deg,#d5a9ff,#a28ae5);border-radius:inherit;box-shadow:inset 0 1px #f1dcff99}
@media(min-width:761px) and (max-width:1439px){.task-actions{max-width:215px;gap:8px}.task-actions :deep(.el-button){min-width:96px;padding:10px 14px;font-size:15px}.task-title{font-size:19px}.task-row{gap:12px}}
@media(max-width:760px){.task-row{grid-template-columns:36px minmax(0,1fr);gap:10px;padding:22px 0}.task-actions{grid-column:2;width:100%;max-width:none;justify-content:stretch;margin-top:4px;gap:8px}.task-actions :deep(.el-button){flex:1;min-width:0;min-height:46px;padding:10px 12px;font-size:15px}.task-title{font-size:17px}.task-meta{font-size:12px}.task-progress{max-width:none;height:7px}.task-check{width:44px;height:44px;margin-left:-4px}.task-check::before{inset:8px;border-width:2px}.sequence-item small{font-size:11px}}
</style>
