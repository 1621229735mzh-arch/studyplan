<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElButton } from 'element-plus'
import { amountText, formatIsoDate, formatMinutes, todayIso } from '@/composables/useFormat'
import type { LearningRecord } from '../types'
const props = defineProps<{ records: LearningRecord[]; readonly?: boolean }>()
defineEmits<{ edit: [record: LearningRecord]; delete: [record: LearningRecord] }>()
const limit = ref(14)
watch(() => props.records, () => { limit.value = 14 })
const days = computed(() => {
  const groups = new Map<string, LearningRecord[]>()
  for (const record of props.records) {
    const group = groups.get(record.recordDate) ?? []
    group.push(record)
    groups.set(record.recordDate, group)
  }
  return [...groups].sort(([a], [b]) => b.localeCompare(a)).map(([date, records]) => ({ date, records }))
})
</script>
<template>
  <div class="record-timeline">
    <section v-for="day in days.slice(0, limit)" :key="day.date" class="record-day">
      <header class="record-date"><time :datetime="day.date">{{ formatIsoDate(day.date) }}</time><span v-if="day.date === todayIso()">今天</span><small>{{ day.records.length }} 条记录</small></header>
      <div class="record-entries">
        <article v-for="record in day.records" :key="record.id" class="record-entry">
          <div class="record-body">
            <div class="record-tags"><span>{{ record.subjectName || '未关联科目' }}</span><small v-if="!record.taskId">独立记录</small></div>
            <h3>{{ record.taskTitle || record.content || '学习记录' }}</h3>
            <p v-if="record.content && record.taskTitle" class="record-content">{{ record.content }}</p>
            <div class="record-facts"><span v-if="record.amount != null">已学 <strong>{{ amountText(record.amount, record.unitName) }}</strong></span><span>用时 <strong>{{ formatMinutes(record.durationMinutes) }}</strong></span></div>
            <p v-if="record.note" class="record-note"><span>复盘</span>{{ record.note }}</p>
          </div>
          <div class="record-actions"><ElButton text :disabled="readonly" :aria-label="'修改 ' + (record.taskTitle || record.content || '学习记录')" @click="$emit('edit', record)">修改</ElButton><ElButton text type="danger" :disabled="readonly" @click="$emit('delete', record)">删除</ElButton></div>
        </article>
      </div>
    </section>
    <ElButton v-if="days.length > limit" class="load-more" @click="limit += 14">查看更早的记录</ElButton>
  </div>
</template>
<style scoped>
.record-day { display:grid; grid-template-columns:170px minmax(0,1fr); gap:24px; margin-bottom:24px; }
.record-date { display:flex; flex-direction:column; align-items:flex-start; gap:6px; padding-top:20px; font-size:13px; color:#405b50; }
.record-date span { font-size:11px; background:#eaf2ec; padding:1px 8px; border-radius:5px; }.record-date small { color:#7b8a80; }
.record-entries { border:1px solid #e5eae8; border-radius:14px; background:white; overflow:hidden; }
.record-entry { display:flex; align-items:flex-start; gap:18px; padding:22px 24px; border-bottom:1px solid #edf0ec; }.record-entry:last-child { border:0; }
.record-body { flex:1; min-width:0; }.record-tags { display:flex; gap:10px; align-items:center; font-size:11px; color:#5d7f6b; }.record-tags small { color:#829087; }
h3 { margin:6px 0 10px; font-size:16px; font-weight:550; overflow-wrap:anywhere; }.record-facts { display:flex; flex-wrap:wrap; gap:18px; color:#71817a; font-size:12px; }.record-facts strong { color:#3e5d50; font-weight:500; }
.record-note,.record-content { font-size:13px; color:#6b7b72; white-space:pre-wrap; overflow-wrap:anywhere; }.record-note { border-left:2px solid #d9e5dc; padding-left:12px; margin:14px 0 0; }.record-note span { display:block; font-size:11px; color:#849189; margin-bottom:3px; }
.record-actions { display:flex; }.record-actions :deep(.el-button) { margin:0; padding:7px; font-size:12px; }.load-more { display:block; margin:20px auto; }
@media(max-width:700px) { .record-day { display:block; }.record-date { flex-direction:row; flex-wrap:wrap; align-items:center; padding:0 0 10px; gap:10px; }.record-date small { margin-left:auto; }.record-entry { padding:18px 16px; flex-wrap:wrap; gap:6px; }.record-body { flex-basis:100%; }.record-actions { margin-left:auto; } h3 { font-size:15px; } }
</style>
