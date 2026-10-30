<script setup lang="ts">
import { computed } from 'vue'
import { ElButton } from 'element-plus'
import { amountText } from '@/composables/useFormat'
import type { CatalogNode } from '../catalog'
const props = defineProps<{ nodes: CatalogNode[]; unit: string; parentKey?: string | null; readonly?: boolean; percentMode?: boolean }>()
defineEmits<{ link: [node: CatalogNode] }>()
const children = computed(() => props.nodes.filter(n => (n.parentKey ?? null) === (props.parentKey ?? null)).sort((a,b) => a.sortOrder - b.sortOrder || a.id - b.id))
function chapter(node: CatalogNode) { return props.nodes.some(n => n.parentKey === node.key) }
function status(node: CatalogNode) { const value=props.percentMode ? node.completionPercent ?? 0:node.completedAmount ?? 0; return value <= 0 ? '未学' : value >= (props.percentMode ? 100 : node.totalAmount ?? Infinity) ? '已完成' : '学习中' }
</script>
<template>
  <div class="catalog-tree">
    <template v-for="node in children" :key="node.id">
      <details v-if="chapter(node)" class="catalog-chapter" open><summary><strong>{{ node.title }}</strong><span v-if="percentMode && node.contentCounts">已完成 {{ node.contentCounts.completed }} 项 · 进行中 {{ node.contentCounts.inProgress }} 项 · 未开始 {{ node.contentCounts.remaining }} 项</span><span v-else>已学 {{ amountText(node.completedAmount, unit) }}</span></summary><CatalogNodeTree :nodes="nodes" :unit="unit" :parent-key="node.key" :readonly="readonly" :percent-mode="percentMode" @link="$emit('link', $event)" /></details>
      <article v-else class="catalog-leaf" :class="{ learned: status(node) === '已完成' }"><div class="leaf-title"><span class="leaf-status">{{ status(node) }}</span><strong>{{ node.title }}</strong></div><div class="leaf-facts"><template v-if="percentMode"><span>已完成 {{ node.completionPercent ?? 0 }}%</span><progress :value="node.completionPercent ?? 0" max="100" :aria-label="node.title + '完成进度'"></progress></template><template v-else><span>{{ node.completedAmount }} / {{ node.totalAmount }} {{ unit }}</span><small v-if="node.totalAmount != null && (node.completedAmount ?? 0) > node.totalAmount">含超出该条目总量的学习记录</small></template><small>{{ node.taskIds.length ? '已关联 ' + node.taskIds.length + ' 项任务' : '未关联任务' }}</small><ElButton text :disabled="readonly" @click="$emit('link', node)">关联任务</ElButton></div></article>
    </template>
  </div>
</template>
<style scoped>
.catalog-tree { min-width:0; }.catalog-chapter { margin-bottom:12px; }.catalog-chapter > summary { cursor:pointer; padding:18px 12px; background:var(--kaoyan-surface); border-radius:8px; font-size:14px; }.catalog-chapter summary span { float:right; font-size:12px; color:var(--kaoyan-text-secondary); }.catalog-chapter > .catalog-tree { padding-left:18px; }.catalog-leaf { padding:18px 12px; border-bottom:1px solid var(--kaoyan-border); display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:12px; }.leaf-title { display:flex; gap:12px; align-items:center; min-width:0; }.leaf-title strong { font-size:14px; font-weight:500; overflow-wrap:anywhere; }.leaf-status { color:var(--kaoyan-text-secondary); background:var(--kaoyan-surface); padding:3px 7px; border-radius:4px; font-size:11px; white-space:nowrap; }.learned .leaf-status { background:var(--kaoyan-surface); color:var(--kaoyan-primary); }.leaf-facts { display:flex; flex-wrap:wrap; gap:10px; align-items:center; color:var(--kaoyan-primary); font-size:13px; }.leaf-facts small { color:var(--kaoyan-text-secondary); font-size:11px; }
@media(max-width:640px) { .catalog-chapter summary span { display:block; float:none; margin-top:8px; }.catalog-chapter > .catalog-tree { padding-left:8px; }.leaf-title { width:100%; }.leaf-facts { width:100%; }.leaf-facts :deep(.el-button) { margin-left:auto; } }
</style>
