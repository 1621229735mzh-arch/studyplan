<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElButton } from 'element-plus'
import { reviewDateExplanation } from '@/features/review/dateExplanation'
import ChecklistTask from './ChecklistTask.vue'
import type { TodayChecklistTask } from '../types'

const props = defineProps<{ title: string; tasks: TodayChecklistTask[]; readonly: boolean }>()
defineEmits<{ action: [task: TodayChecklistTask, complete: boolean] }>()
const revealed = ref('')
const current = computed(() => props.tasks.find(t => t.completionPercent < 100))
const key = (task: TodayChecklistTask) => `${task.id}:${task.version}`
const visible = computed(() => current.value && (!current.value.automaticReview?.optional || revealed.value === key(current.value)))
const completed = computed(() => props.tasks.filter(t => t.completionPercent >= 100))
</script>

<template>
  <li class="review-book">
    <h4>{{ title }}</h4>
    <template v-if="current">
      <p class="review-queue-note">待复习 {{ current.automaticReview?.remainingCount }} 段 · 先逾期，再今天到期</p>
      <div v-if="visible" class="review-current">
        <p class="review-due">{{ current.automaticReview!.overdueDays > 0 ? `逾期 ${current.automaticReview!.overdueDays} 天` : '今天到期' }} · 原到期日 {{ current.automaticReview?.dueDate }} · 第{{ current.automaticReview?.round }}次复习</p>
        <p v-if="current.automaticReview?.selectionReason" class="review-queue-note">{{ current.automaticReview.selectionReason }}</p>
        <p class="review-queue-note">{{ reviewDateExplanation(current.automaticReview?.dateSource) }}</p>
        <ul><ChecklistTask :task="current" :readonly="readonly" @action="(task, complete) => $emit('action', task, complete)" /></ul>
      </div>
      <div v-else class="continue-review">
        <p>这一项已复习完成，还可以继续处理下一项。</p>
        <ElButton :disabled="readonly" @click="revealed = key(current)">继续复习下一项</ElButton>
      </div>
    </template>
    <p v-else class="review-queue-note">今天到期的内容已复习完。</p>
    <details v-if="completed.length" class="review-completed">
      <summary>今日已复习 · {{ completed.length }} 项</summary>
      <ul><ChecklistTask v-for="task in completed" :key="task.id" :task="task" :readonly="readonly" @action="(item, complete) => $emit('action', item, complete)" /></ul>
    </details>
  </li>
</template>

<style scoped>
.review-book{padding:18px 0;border-top:1px solid var(--kaoyan-border)}h4{font-size:15px;margin:0 0 10px;color:#cbb7df;overflow-wrap:anywhere}ul{padding:0;list-style:none;margin:0}
.review-queue-note,.review-due,.continue-review p{font-size:12px;line-height:1.8;color:var(--kaoyan-text-secondary);margin:6px 0}.review-due{color:var(--kaoyan-primary)}
.review-current :deep(.task-row){border:0;padding-top:12px}.review-completed summary{font-size:13px;cursor:pointer;padding:12px 0;color:var(--kaoyan-text-secondary)}.continue-review{margin-top:12px}
</style>
