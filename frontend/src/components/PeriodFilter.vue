<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElButton } from 'element-plus'
import { addDays, todayIso, toIsoDate, weekStartOf } from '@/composables/useFormat'

const props = defineProps<{ disabled?: boolean; allowAll?: boolean; initialPeriod?: 'week' | 'all' }>()
const emit = defineEmits<{ change: [range: { from: string; to: string }] }>()
const selected = ref<string>(props.initialPeriod ?? 'week')
const customFrom = ref(weekStartOf(todayIso()))
const customTo = ref(addDays(customFrom.value, 6))
const validRange = computed(() => customFrom.value !== '' && customTo.value !== '' && customFrom.value <= customTo.value)
const choices = [
  { key: 'week', label: '本周' }, { key: 'month', label: '本月' },
  { key: 'recent', label: '近 30 天' }, { key: 'custom', label: '自选日期' }
]
function choose(key: string) {
  if (props.disabled) return
  selected.value = key
  const today = todayIso()
  if (key === 'custom') return
  const start = key === 'week' ? weekStartOf(today) : key === 'month' ? today.slice(0, 7) + '-01' : addDays(today, -29)
  const end = key === 'week' ? addDays(start, 6) : key === 'month' ? toIsoDate(new Date(Number(today.slice(0, 4)), Number(today.slice(5, 7)), 0)) : today
  emit('change', key === 'all' ? { from: '', to: '' } : { from: start, to: end })
}
</script>

<template>
  <div class="period-filter">
    <nav aria-label="查看时间范围">
      <button v-for="choice in [...choices, ...(allowAll ? [{ key: 'all', label: '全部' }] : [])]" :key="choice.key"
        :class="{ selected: selected === choice.key }" :aria-pressed="selected === choice.key" :disabled="disabled" @click="choose(choice.key)">{{ choice.label }}</button>
    </nav>
    <div v-if="selected === 'custom'" class="custom-range">
      <label>开始日期<input v-model="customFrom" type="date" :max="customTo || undefined" :disabled="disabled" /></label>
      <label>结束日期<input v-model="customTo" type="date" :min="customFrom || undefined" :disabled="disabled" /></label>
      <ElButton :disabled="disabled || !validRange" @click="validRange && emit('change', { from: customFrom, to: customTo })">查看</ElButton>
    </div>
    <p v-if="selected === 'custom' && !validRange" class="range-error" role="status">请选择完整日期范围，结束日期不能早于开始日期。</p>
  </div>
</template>

<style scoped>
.period-filter { min-width:0; }
nav { display:flex; flex-wrap:wrap; gap:4px; padding:5px; border-radius:12px; background:#eaf0ed; width:fit-content; }
button { border:0; border-radius:8px; background:transparent; color:#63756a; font:inherit; padding:9px 18px; cursor:pointer; }
button.selected { background:white; color:#2a6257; font-weight:600; box-shadow:0 2px 5px #233b2910; }
button:disabled { cursor:default; opacity:.6; } button:focus-visible { outline:2px solid #39776d; outline-offset:2px; }
.custom-range { display:flex; align-items:flex-end; flex-wrap:wrap; gap:8px; margin-top:12px; max-width:100%; }
.custom-range label { display:flex; flex-direction:column; gap:4px; font-size:12px; color:#63756a; min-width:0; }
.custom-range input { font:inherit; color:#405b50; border:1px solid #dce4df; border-radius:6px; background:white; min-width:0; padding:6px 8px; height:32px; }
.custom-range input:focus-visible { outline:2px solid #39776d; outline-offset:2px; }
.range-error { font-size:12px; color:#a34a36; }
@media(max-width:640px) { button { padding:8px 11px; font-size:12px; } }
</style>
