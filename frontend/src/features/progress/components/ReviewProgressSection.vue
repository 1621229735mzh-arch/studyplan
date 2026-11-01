<script setup lang="ts">
import { RouterLink } from 'vue-router'
import ReviewTotals from '@/features/review/components/ReviewTotals.vue'
import type { ReviewProgress } from '@/features/review/workspace'
withDefaults(defineProps<{ reviews: ReviewProgress[]; available?: boolean }>(), { available: true })
</script>
<template>
  <section class="review-progress" aria-label="复习进度">
    <div class="review-heading"><div><h2>复习进度</h2><p>已学内容的复习情况，独立于首次学习量。</p></div><RouterLink to="/review">查看复习计划 ↗</RouterLink></div>
    <p v-if="!available" class="review-empty">当前快照尚未同步复习概览，请联网后刷新进度。</p>
    <template v-else-if="reviews.length"><article v-for="review in reviews" :key="review.subjectId"><div class="review-subject"><h3>{{ review.subjectName }}</h3><RouterLink :to="{ path: '/review', query: { subject: review.subjectId } }">查看安排 ↗</RouterLink></div><ReviewTotals :totals="review.totals" /></article></template>
    <p v-else class="review-empty">还没有纳入复习的内容。确定复习内容后，这里会显示复习概览。</p>
  </section>
</template>
<style scoped>
.review-progress { margin:28px 0; }.review-heading,.review-subject { display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap; }.review-heading { margin-bottom:18px; } h2 { margin:0 0 6px; font-size:18px; font-weight:550; } h3 { font-size:14px; font-weight:500; } p { margin:0; font-size:12px; color:var(--kaoyan-text-secondary); } a { font-size:12px; color:var(--kaoyan-primary); text-decoration:none; }.review-empty { padding:24px; border:1px dashed var(--kaoyan-border); border-radius:12px; line-height:1.8; }
</style>
