<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { directions } from '../catalog'
</script>
<template>
  <div class="study-workspace progress-home">
    <header class="workspace-header"><div><span class="workspace-eyebrow">看清学到哪里</span><h1>学习进度</h1><p>选择一个方向，查看课程与资料的完整学习进度。</p></div><RouterLink to="/learning">回看学习记录 ↗</RouterLink></header>
    <nav class="direction-grid" aria-label="四个学习方向">
      <RouterLink v-for="direction in directions" :key="direction.key" :to="`/progress/${direction.key}`" class="direction-entry">
        <span class="direction-symbol" aria-hidden="true">{{ direction.symbol }}</span><span class="direction-en">{{ direction.subtitle }}</span><h2>{{ direction.title }}</h2><span class="direction-action">进入查看 <span aria-hidden="true">↗</span></span>
      </RouterLink>
    </nav>
  </div>
</template>
<style scoped src="@/styles/retrospective.css"></style>
<style scoped>
.progress-home { padding-bottom:40px; }.direction-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:22px; }.direction-entry { position:relative; display:flex; flex-direction:column; padding:36px; min-height:245px; border:1px solid var(--kaoyan-border); border-radius:20px; text-decoration:none; background:linear-gradient(145deg,var(--kaoyan-card),var(--kaoyan-surface)); transition:transform .15s,border-color .15s; }.direction-entry:hover { transform:translateY(-3px); border-color:var(--kaoyan-border-strong); }.direction-entry:focus-visible { outline:3px solid var(--kaoyan-primary); outline-offset:3px; }.direction-entry:nth-child(2) { background:linear-gradient(145deg,var(--kaoyan-card),var(--kaoyan-surface)); }.direction-entry:nth-child(3) { background:linear-gradient(145deg,var(--kaoyan-card),var(--kaoyan-surface)); }.direction-entry:nth-child(4) { background:linear-gradient(145deg,var(--kaoyan-card),var(--kaoyan-surface)); }.direction-symbol { position:absolute; right:30px; top:24px; font-size:56px; font-weight:600; opacity:.12; }.direction-en { font-size:11px; color:var(--kaoyan-text-secondary); letter-spacing:2px; margin-top:22px; }.direction-entry h2 { font-size:32px; color:var(--kaoyan-text); margin:12px 0 30px; }.direction-action { font-size:13px; color:var(--kaoyan-text-secondary); display:flex; justify-content:space-between; }
@media(max-width:640px) { .direction-grid { gap:12px; }.direction-entry { padding:22px 18px; min-height:200px; }.direction-symbol { font-size:38px; right:16px; top:18px; }.direction-en { font-size:9px; letter-spacing:1px; }.direction-entry h2 { font-size:26px; margin-bottom:24px; } }
</style>
