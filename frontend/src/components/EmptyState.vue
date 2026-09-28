<script setup lang="ts">
/** 空状态占位：统一“还没有数据”时的说明与可选操作入口。 */
import { ElEmpty } from 'element-plus'

withDefaults(
  defineProps<{
    /** 主说明 */
    description?: string
    /** 补充说明（例如下一步该做什么） */
    hint?: string
  }>(),
  {
    description: '暂无数据',
    hint: ''
  }
)
</script>

<template>
  <div class="empty-state">
    <ElEmpty :description="description">
      <p v-if="hint" class="empty-state__hint">{{ hint }}</p>
      <slot />
    </ElEmpty>
  </div>
</template>

<style scoped>
.empty-state {
  padding: 8px 0;
}

.empty-state__hint {
  margin: 0 0 12px;
  color: var(--kaoyan-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}
</style>
