<script setup lang="ts">
import type { ContentCounts } from '../catalog'
defineProps<{ counts: ContentCounts }>()
</script>
<template>
  <div class="content-counts" aria-label="内容学习进度">
    <span>已完成 <strong>{{ counts.completed }}</strong> 项</span>
    <span>进行中 <strong>{{ counts.inProgress }}</strong> 项</span>
    <span>未开始 <strong>{{ counts.remaining }}</strong> 项</span>
    <small>完整目录 {{ counts.total }} 项内容</small>
  </div>
</template>
<style scoped>
.content-counts{display:flex;flex-wrap:wrap;gap:10px 20px;color:var(--kaoyan-text-secondary);font-size:13px}.content-counts strong{font-size:20px;color:var(--kaoyan-primary);font-weight:500}.content-counts small{width:100%;font-size:12px}
</style>
