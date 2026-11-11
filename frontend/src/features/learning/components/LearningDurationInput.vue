<script setup lang="ts">
import { ElInputNumber } from 'element-plus'
defineProps<{ modelValue?: number | null; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: number | null] }>()
</script>
<template>
  <div class="learning-duration">
    <!-- Keep legacy zero values unchanged when editing; new entries validate positive minutes. -->
    <ElInputNumber :model-value="modelValue ?? undefined" :min="0" :max="2147483647" :precision="0"
      :controls="false" :disabled="disabled" placeholder="可留空" aria-label="本次用时（分钟）"
      @update:model-value="value => emit('update:modelValue', typeof value === 'number' ? value : null)" />
    <span>分钟 · 可选</span>
    <p>只填本次实际学习用时，扣除休息；分次学习分别填写，留空表示未知。积累后用于校准计划。</p>
  </div>
</template>
<style scoped>
.learning-duration { width:100%; }
.learning-duration span { margin-left:8px; font-size:12px; color:var(--kaoyan-text-secondary); }
.learning-duration p { margin:6px 0 0; font-size:12px; line-height:1.6; color:var(--kaoyan-text-secondary); }
</style>
