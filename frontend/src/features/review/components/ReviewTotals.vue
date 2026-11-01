<script setup lang="ts">
import type { ReviewTotals } from '../workspace'
defineProps<{ totals: ReviewTotals }>()
</script>
<template>
  <dl class="review-totals" aria-label="复习概览">
    <div><dt>纳入复习</dt><dd>{{ totals.itemCount }}<small>项内容</small></dd></div>
    <div><dt>已完整复习过</dt><dd>{{ totals.reviewedItems }}<small>项内容</small></dd></div>
    <div><dt>累计完成复习</dt><dd>{{ totals.completedReviews }}<small>次</small></dd></div>
    <div><dt>复习天数</dt><dd>{{ totals.reviewDays }}<small>天</small></dd></div>
  </dl>
</template>
<style scoped>
.review-totals { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:16px; margin:0 0 24px; padding:22px; border:1px solid var(--kaoyan-border); border-radius:14px; background:var(--kaoyan-card); }
dt { color:var(--kaoyan-text-secondary); font-size:12px; } dd { margin:10px 0 0; font-size:24px; color:var(--kaoyan-primary); } small { margin-left:6px; font-size:11px; color:var(--kaoyan-text-secondary); }
@media(max-width:640px) { .review-totals { grid-template-columns:repeat(2,minmax(0,1fr)); padding:18px; gap:20px; } dd { font-size:21px; } }
</style>
