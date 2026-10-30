<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElButton } from 'element-plus'
import ChecklistTask from './ChecklistTask.vue'
import type { TodayChecklistTask } from '../types'
const props = defineProps<{ title: string; tasks: TodayChecklistTask[]; readonly: boolean; showEstimate?: boolean }>()
defineEmits<{ action: [task: TodayChecklistTask, complete: boolean] }>()
const revealed = ref('')
const nextContent = computed(() => props.tasks.find(t => t.continuation?.optional))
const contentKey = (task: TodayChecklistTask) => `${task.id}:${task.revision}`
const pending = computed(() => props.tasks.filter(t => t.completionPercent < 100 &&
  (!t.continuation?.optional || revealed.value === contentKey(t))))
</script>
<template>
  <li class="book-task">
    <h3>{{ title }}</h3>
    <ul>
      <li v-for="task in pending" :key="task.id" class="book-content">
        <span class="content-origin">{{ task.continuation?.optional ? '继续学习 · 自选' : task.continuation?.carried ? '剩余内容' : '新内容' }}</span>
        <ul><ChecklistTask :task="task" :readonly="readonly" :show-estimate="showEstimate" @action="(item,complete) => $emit('action',item,complete)" /></ul>
      </li>
    </ul>
    <details v-if="tasks.some(t => t.completionPercent >= 100)">
      <summary>已完成 · {{ tasks.filter(t => t.completionPercent >= 100).length }}</summary>
      <ul><ChecklistTask v-for="task in tasks.filter(t => t.completionPercent >= 100)" :key="task.id" :task="task" :readonly="readonly" @action="(item,complete) => $emit('action',item,complete)" /></ul>
    </details>
    <div v-if="nextContent && revealed !== contentKey(nextContent)" class="continue-learning">
      <p>本书当天内容已完成，可以继续往后学。</p>
      <ElButton size="small" :disabled="readonly" @click="revealed = contentKey(nextContent)">继续学习下一段</ElButton>
    </div>
  </li>
</template>
<style scoped>
.book-task{border-top:1px solid var(--kaoyan-border);padding:22px 0}.book-task h3{font-size:14px;margin:0 0 8px;font-weight:500;color:#cbb7df;letter-spacing:.5px}
ul{list-style:none;padding:0;margin:0}.book-content{margin-top:8px}
.content-origin{display:inline-block;padding:2px 8px;border:1px solid var(--kaoyan-border);border-radius:4px;font-size:11px;color:var(--kaoyan-text-secondary)}summary{font-size:14px;color:var(--kaoyan-text-secondary);cursor:pointer;padding:10px 0}
.book-content :deep(.task-row){border-top:0;padding:14px 0 4px}
.continue-learning{margin-top:12px}.continue-learning p{font-size:13px;color:var(--kaoyan-text-secondary);margin:0 0 10px}
@media(max-width:760px){.book-task{padding:18px 0}.book-task h3{font-size:13px}}
</style>
