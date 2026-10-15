<script setup lang="ts">
import { computed } from 'vue'
import { ElButton } from 'element-plus'
import { amountText } from '@/composables/useFormat'
import type { CatalogNode } from '../catalog'
const props = defineProps<{ nodes: CatalogNode[]; unit: string; parentKey?: string | null; readonly?: boolean }>()
defineEmits<{ link: [node: CatalogNode] }>()
const children = computed(() => props.nodes.filter(n => (n.parentKey ?? null) === (props.parentKey ?? null)).sort((a,b) => a.sortOrder - b.sortOrder || a.id - b.id))
function status(node: CatalogNode) { return node.completedAmount <= 0 ? '未学' : node.totalAmount != null && node.completedAmount >= node.totalAmount ? '已完成' : '学习中' }
</script>
<template>
  <div class="catalog-tree">
    <template v-for="node in children" :key="node.id">
      <details v-if="node.totalAmount == null" class="catalog-chapter" open><summary><strong>{{ node.title }}</strong><span>已学 {{ amountText(node.completedAmount, unit) }}</span></summary><CatalogNodeTree :nodes="nodes" :unit="unit" :parent-key="node.key" :readonly="readonly" @link="$emit('link', $event)" /></details>
      <article v-else class="catalog-leaf" :class="{ learned: node.completedAmount >= node.totalAmount }"><div class="leaf-title"><span class="leaf-status">{{ status(node) }}</span><strong>{{ node.title }}</strong></div><div class="leaf-facts"><span>{{ node.completedAmount }} / {{ node.totalAmount }} {{ unit }}</span><small v-if="node.completedAmount > node.totalAmount">含超出该条目总量的学习记录</small><small>{{ node.taskIds.length ? '已关联 ' + node.taskIds.length + ' 项任务' : '未关联任务' }}</small><ElButton text :disabled="readonly" @click="$emit('link', node)">关联任务</ElButton></div></article>
    </template>
  </div>
</template>
<style scoped>
.catalog-tree { min-width:0; }.catalog-chapter { margin-bottom:12px; }.catalog-chapter > summary { cursor:pointer; padding:18px 12px; background:#f4f7f2; border-radius:8px; font-size:14px; }.catalog-chapter summary span { float:right; font-size:12px; color:#798c7e; }.catalog-chapter > .catalog-tree { padding-left:18px; }.catalog-leaf { padding:18px 12px; border-bottom:1px solid #e8eee8; display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:12px; }.leaf-title { display:flex; gap:12px; align-items:center; min-width:0; }.leaf-title strong { font-size:14px; font-weight:500; overflow-wrap:anywhere; }.leaf-status { color:#8c988c; background:#f0f3ef; padding:3px 7px; border-radius:4px; font-size:11px; white-space:nowrap; }.learned .leaf-status { background:#e6f2e9; color:#3f765a; }.leaf-facts { display:flex; flex-wrap:wrap; gap:10px; align-items:center; color:#4c6a56; font-size:13px; }.leaf-facts small { color:#869587; font-size:11px; }
@media(max-width:640px) { .catalog-chapter summary span { display:block; float:none; margin-top:8px; }.catalog-chapter > .catalog-tree { padding-left:8px; }.leaf-title { width:100%; }.leaf-facts { width:100%; }.leaf-facts :deep(.el-button) { margin-left:auto; } }
</style>
