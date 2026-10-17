<script setup lang="ts">
import { amountText } from '@/composables/useFormat'
import type { CountedProgress } from '../catalog'
defineProps<{ activities: CountedProgress[] }>()
</script>
<template>
  <section aria-label="每日学习与复习进度" class="counted-progress">
    <div class="section-heading"><div><h2>每日学习与复习</h2><p>数量来自实际记录，修改或删除后自动更新。</p></div></div>
    <div class="activity-grid">
      <article v-for="activity in activities" :key="activity.taskId" class="activity-card">
        <span class="workspace-note">{{ activity.mode === 'COUNT_REVIEW' ? '复习' : '新学' }}</span>
        <h3>{{ activity.title }}</h3>
        <p class="activity-quantity" v-if="activity.recordedAmount != null">{{ activity.mode === 'COUNT_REVIEW' ? '已记录复习' : '累计新学' }} <strong>{{ amountText(activity.recordedAmount,activity.unitName) }}</strong></p>
        <p v-else class="activity-quantity">{{ activity.mode === 'COUNT_REVIEW' ? '复习' : '新学' }}数量未记录</p>
        <p class="workspace-note">{{ activity.learningDays }} 天 · {{ activity.recordCount }} 次记录</p>
        <p v-if="activity.unknownQuantityRecords" class="workspace-note">{{ activity.unknownQuantityRecords }} 次历史记录未填写数量，未推算词数。</p>
        <p class="workspace-note">{{ activity.mode === 'COUNT_REVIEW' ? '复习词数单独累计，不增加新词量。' : '按实际背诵量累计，不设固定总目标。' }}</p>
      </article>
    </div>
  </section>
</template>
<style scoped>
.counted-progress { margin-bottom:28px; }.activity-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:18px; }.activity-card { border:1px solid #e0e7de; border-radius:14px; background:#fff; padding:24px; min-width:0; }.activity-card h3 { margin:8px 0 20px; color:#36533e; font-size:19px; overflow-wrap:anywhere; }.activity-quantity { color:#517759; font-size:15px; }.activity-quantity strong { margin-left:8px; font-size:26px; font-weight:550; }.activity-card .workspace-note { line-height:1.7; }
@media(max-width:760px) { .activity-grid { grid-template-columns:1fr; }.activity-card { padding:20px; } }
</style>
