<script setup lang="ts">
import { computed } from 'vue'
import { amountText } from '@/composables/useFormat'
const props = defineProps<{ completed: number; total: number; unit: string }>()
const fraction = computed(() => props.total > 0 ? Math.max(0, Math.min(1, props.completed / props.total)) : 0)
const circumference = 2 * Math.PI * 46
</script>
<template>
  <figure class="course-ring" :aria-label="`已完成 ${amountText(completed, unit)}，总量 ${amountText(total, unit)}`">
    <svg viewBox="0 0 112 112" aria-hidden="true"><circle cx="56" cy="56" r="46" fill="none" stroke="#e8eee9" stroke-width="9"/><circle cx="56" cy="56" r="46" fill="none" stroke="#4c8976" stroke-width="9" stroke-linecap="round" :stroke-dasharray="circumference" :stroke-dashoffset="circumference * (1 - fraction)" transform="rotate(-90 56 56)"/></svg>
    <figcaption><strong>{{ Math.round(fraction * 100) }}%</strong><span>已完成</span></figcaption>
  </figure>
</template>
<style scoped>.course-ring { width:112px; height:112px; margin:0; position:relative; flex-shrink:0; }.course-ring svg { width:100%; }.course-ring figcaption { position:absolute; inset:0; display:flex; flex-direction:column; align-items:center; justify-content:center; }.course-ring strong { font-size:22px; color:#356752; }.course-ring span { margin-top:4px; font-size:11px; color:#87958c; }</style>
