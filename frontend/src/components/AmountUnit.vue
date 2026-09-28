<script setup lang="ts">
/**
 * 数量 + 单位。
 *
 * 跨功能统一入口：任何数量都必须带单位展示，避免不同单位被误读成同一个数。
 * 缺少单位时明确标注“单位未知”，不用无单位数字蒙混过去。
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    /** 数量；null/undefined 表示后端未提供 */
    amount?: number | null
    /** 计量单位名称（题、讲、章、页……） */
    unit?: string | null
    /** 数量缺失时的占位文本 */
    placeholder?: string
    /** 小数位；默认按后端返回值原样显示 */
    precision?: number | null
  }>(),
  {
    amount: null,
    unit: null,
    placeholder: '—',
    precision: null
  }
)

const displayAmount = computed<string>(() => {
  if (props.amount === null || props.amount === undefined) {
    return props.placeholder
  }
  return props.precision === null ? String(props.amount) : props.amount.toFixed(props.precision)
})

const hasAmount = computed(() => props.amount !== null && props.amount !== undefined)
const hasUnit = computed(() => typeof props.unit === 'string' && props.unit.trim().length > 0)
</script>

<template>
  <span class="amount-unit">
    <span class="amount-unit__value">{{ displayAmount }}</span>
    <span v-if="hasUnit" class="amount-unit__unit">{{ unit }}</span>
    <span v-else-if="hasAmount" class="amount-unit__unit amount-unit__unit--missing">（单位未知）</span>
  </span>
</template>

<style scoped>
.amount-unit {
  white-space: nowrap;
}

.amount-unit__unit {
  margin-left: 4px;
  color: var(--kaoyan-text-secondary);
  font-size: 0.92em;
}

.amount-unit__unit--missing {
  color: var(--el-color-warning);
}
</style>
