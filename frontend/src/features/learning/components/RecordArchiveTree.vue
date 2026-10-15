<script setup lang="ts">
import { ref } from 'vue'
import { ElButton } from 'element-plus'
import { amountText, formatIsoDate, formatMinutes, todayIso, weekStartOf } from '@/composables/useFormat'
import type { ArchiveMonth, LearningRecord } from '../types'
defineProps<{ months: ArchiveMonth[]; readonly?: boolean }>()
defineEmits<{ edit: [record: LearningRecord]; delete: [record: LearningRecord]; classify: [record: LearningRecord] }>()
const openMonths = ref(new Set([todayIso().slice(0, 7)]))
const openWeeks = ref(new Set([weekStartOf(todayIso())]))
function remember(event: Event, set: Set<string>, key: string) {
  if ((event.target as HTMLDetailsElement).open) set.add(key)
  else set.delete(key)
}
function monthTitle(value: string) { return `${value.slice(0,4)} 年 ${Number(value.slice(5))} 月` }
</script>
<template>
  <div class="record-archive">
    <details v-for="month in months" :key="month.month" class="archive-month" :open="openMonths.has(month.month)" @toggle="remember($event, openMonths, month.month)">
      <summary class="archive-heading"><strong>{{ monthTitle(month.month) }}</strong><span>{{ month.stats.learningDays }} 天学习 · {{ formatMinutes(month.stats.studyMinutes) }}<small v-if="month.stats.missingDurationRecords"> · {{ month.stats.missingDurationRecords }} 次未填用时</small></span></summary>
      <div class="month-content">
        <details v-for="week in month.weeks" :key="week.weekStart" class="archive-week" :open="openWeeks.has(week.weekStart)" @toggle="remember($event, openWeeks, week.weekStart)">
          <summary class="archive-heading"><strong>{{ week.from.slice(5) }} — {{ week.to.slice(5) }}</strong><span>{{ week.stats.learningDays }} 天 · {{ formatMinutes(week.stats.studyMinutes) }}</span></summary>
          <div class="week-content">
            <details v-for="day in week.days" :key="day.date" class="archive-day">
              <summary><div class="day-heading"><strong>{{ formatIsoDate(day.date) }}</strong><span>{{ formatMinutes(day.stats.studyMinutes) }}<small v-if="day.stats.missingDurationRecords"> · 有用时未记录</small></span></div><div class="day-amounts"><span v-for="amount in day.stats.amounts" :key="amount.subjectId + ':' + amount.unitId">{{ amount.subjectName }} · {{ amountText(amount.amount, amount.unitName) }}</span></div></summary>
              <div class="day-content">
                <details v-for="group in day.groups" :key="group.key" class="archive-group">
                  <summary><span class="group-subject">{{ group.subjectName || '自主学习' }}</span><strong>{{ group.title }}</strong><span><template v-if="group.records.every(r => r.activityMode === 'CHECK_ONLY')">已确认完成</template><template v-else>{{ amountText(group.amount, group.unitName) }} · {{ formatMinutes(group.studyMinutes) }} · {{ group.records.length }} 次</template></span></summary>
                  <article v-for="record in group.records" :key="record.id" class="archive-record">
                    <div><p v-if="record.sequenceTitle"><a :href="record.sequenceUrl || undefined" target="_blank" rel="noopener noreferrer">{{ record.sequenceTitle }} ↗</a></p><h4>{{ record.content || record.taskTitle || '学习记录' }}</h4><p>{{ record.activityMode === 'CHECK_ONLY' ? '已确认完成 · 无需数量或时长' : `${amountText(record.amount, record.unitName)} · ${formatMinutes(record.durationMinutes)}` }}</p><p v-if="record.note" class="record-note">{{ record.note }}</p></div>
                    <div class="record-actions"><ElButton v-if="record.amount != null" text :disabled="readonly" @click="$emit('classify', record)">课程归属</ElButton><ElButton v-if="record.activityMode !== 'CHECK_ONLY'" text :disabled="readonly" @click="$emit('edit', record)">修改</ElButton><ElButton text type="danger" :disabled="readonly" @click="$emit('delete', record)">删除</ElButton></div>
                  </article>
                </details>
              </div>
            </details>
          </div>
        </details>
      </div>
    </details>
  </div>
</template>
<style scoped>
details { min-width:0; } summary { cursor:pointer; list-style:none; } summary::-webkit-details-marker { display:none; }
summary::before { content:'›'; float:left; margin-right:10px; color:#79978c; font-size:20px; line-height:22px; transition:transform .15s; } details[open] > summary::before { transform:rotate(90deg); }
.archive-month { background:#fff; border:1px solid #e0e8e3; border-radius:14px; margin:16px 0; overflow:hidden; }.archive-heading { padding:22px 24px; display:flex; align-items:center; gap:12px; }.archive-heading strong { font-size:19px; }.archive-heading span { margin-left:auto; color:#72827a; font-size:13px; }
.month-content { padding:0 24px 20px; }.archive-week { border-top:1px solid #e8ede9; }.archive-week > summary { padding:20px 0; }.archive-week strong { font-size:15px; }.week-content { padding-left:20px; }.archive-day { border:1px solid #e7ede9; border-radius:10px; margin-bottom:12px; }.archive-day > summary { padding:16px; }.day-heading { display:flex; flex-wrap:wrap; gap:8px; justify-content:space-between; }.day-heading strong { font-size:14px; }.day-heading span,.day-amounts { font-size:12px; color:#6c8175; }.day-amounts { margin-top:10px; display:flex; flex-wrap:wrap; gap:8px 20px; }.day-content { padding:0 16px 12px; }
.archive-group { border-top:1px solid #edf0ec; }.archive-group > summary { padding:14px 0; display:flex; flex-wrap:wrap; align-items:center; gap:8px; }.archive-group strong { font-size:14px; overflow-wrap:anywhere; }.archive-group summary > span:last-child { margin-left:auto; font-size:12px; color:#72827a; }.group-subject { color:#39776d; font-size:12px; background:#eef5f1; padding:3px 8px; border-radius:6px; }
.archive-record { display:flex; justify-content:space-between; gap:12px; background:#f7f9f6; padding:14px; margin:6px 0; border-radius:8px; }.archive-record > div:first-child { min-width:0; }.archive-record h4 { font-size:13px; margin:0 0 8px; font-weight:500; overflow-wrap:anywhere; }.archive-record p { margin:6px 0 0; font-size:12px; color:#6a7c71; }.record-note { white-space:pre-wrap; overflow-wrap:anywhere; }.record-actions { display:flex; flex-shrink:0; align-items:flex-start; }.record-actions :deep(.el-button) { margin:0; padding:5px; font-size:12px; }
@media(max-width:640px) { .archive-heading { flex-wrap:wrap; padding:18px 16px; }.archive-heading span { width:100%; margin:0 0 0 22px; font-size:12px; }.month-content { padding:0 12px 12px; }.archive-week > summary { padding:16px 4px; }.week-content { padding-left:4px; }.day-heading span { width:100%; }.archive-group summary > span:last-child { width:100%; margin:0; }.archive-record { flex-wrap:wrap; }.record-actions { margin-left:auto; } }
</style>
