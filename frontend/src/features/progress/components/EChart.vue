<script setup lang="ts">
/**
 * ECharts 通用容器。
 *
 * 直接使用完整 echarts 包（不按需引入），换取类型与初始化逻辑的稳定；
 * 图表只负责“把后端返回的数字画出来”，不做任何统计计算。
 */
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'

const props = withDefaults(
  defineProps<{
    option: echarts.EChartsOption
    /** 图表高度 */
    height?: string
    /** 数据加载中 */
    loading?: boolean
  }>(),
  {
    height: '320px',
    loading: false
  }
)

const container = ref<HTMLElement | null>(null)
const chart = shallowRef<echarts.ECharts | null>(null)

function resize(): void {
  chart.value?.resize()
}

onMounted(() => {
  if (container.value === null) {
    return
  }
  chart.value = echarts.init(container.value)
  chart.value.setOption(props.option)
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart.value?.dispose()
  chart.value = null
})

watch(
  () => props.option,
  (option) => {
    chart.value?.setOption(option, true)
  },
  { deep: true }
)

watch(
  () => props.loading,
  (loading) => {
    if (loading) {
      chart.value?.showLoading()
    } else {
      chart.value?.hideLoading()
    }
  }
)

defineExpose({ resize })
</script>

<template>
  <div ref="container" class="echart" :style="{ height }" />
</template>

<style scoped>
.echart {
  width: 100%;
}
</style>
