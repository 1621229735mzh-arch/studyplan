<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElButton, ElInputNumber } from 'element-plus'
import { toApiError } from '@/api/http'
import { readTimeDraft, saveTimeDraft, discardTimeDraft, type TimeDraft, type StudyTimeRange } from '../api/timeDraft'

const props = defineProps<{ context: string; modelValue?: number | null; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: number | null] }>()
const mode = ref<'range' | 'minutes'>(props.modelValue == null ? 'range' : 'minutes')
const draft = ref<TimeDraft | null>(null)
const start = ref('')
const end = ref('')
const busy = ref(false)
const ready = ref(false)
const error = ref('')
const elapsed = computed(() => {
  if (!start.value || !end.value) return null
  // Both inputs are Beijing wall-clock times, independent of the device timezone.
  const minutes = (Date.parse(end.value + 'Z') - Date.parse(start.value + 'Z')) / 60000
  return Number.isInteger(minutes) && minutes > 0 && minutes <= 2147483647 ? minutes : null
})
const invalidEnd = computed(() => !!end.value && !!start.value && elapsed.value == null)
function now() {
  const parts = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit',
    day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(new Date())
  const part = (name: Intl.DateTimeFormatPartTypes) => parts.find(p => p.type === name)?.value
  return `${part('year')}-${part('month')}-${part('day')}T${part('hour')}:${part('minute')}`
}
async function load() {
  busy.value = true
  error.value = ''
  try {
    const current = await readTimeDraft(props.context)
    if (current?.revision !== draft.value?.revision) end.value = ''
    draft.value = current
    start.value = draft.value?.startedAt.slice(0, 16) ?? ''
    if (draft.value) mode.value = 'range'
    ready.value = true
  } catch (caught) {
    error.value = toApiError(caught).message
    ready.value = false
  } finally { busy.value = false }
}
async function changeStart(value: string) {
  if (busy.value || props.disabled || !ready.value) return
  start.value = value
  busy.value = true
  error.value = ''
  try {
    if (value) draft.value = await saveTimeDraft(props.context, value, draft.value?.revision ?? null)
    else {
      if (draft.value) await discardTimeDraft(props.context, draft.value.revision)
      draft.value = null
      end.value = ''
    }
  } catch (caught) {
    const failure = toApiError(caught)
    error.value = failure.message
    ready.value = failure.status !== 409
  } finally { busy.value = false }
}
function submission(manualMinutes: number | null): { durationMinutes: number | null; timeRange?: StudyTimeRange; error?: string } {
  if (mode.value === 'minutes') return { durationMinutes: manualMinutes }
  if (busy.value || !ready.value || error.value) return { durationMinutes: null, error: '请等开始时间同步后再保存，或选择手填分钟。' }
  if (!start.value && !end.value) return { durationMinutes: null }
  if (elapsed.value == null || !draft.value) return { durationMinutes: null, error: '请填写完整的起止时间，结束时间须晚于开始时间。' }
  return { durationMinutes: elapsed.value, timeRange: { revision: draft.value.revision, endedAt: end.value } }
}
onMounted(load)
defineExpose({ submission })
</script>
<template>
  <div class="learning-duration">
    <div class="mode" role="group" aria-label="用时填写方式">
      <ElButton size="small" :type="mode === 'range' ? 'primary' : 'default'" :disabled="disabled || busy" @click="mode = 'range'">起止时间</ElButton>
      <ElButton size="small" :type="mode === 'minutes' ? 'primary' : 'default'" :disabled="disabled || busy" @click="mode = 'minutes'">手填分钟</ElButton>
    </div>
    <template v-if="mode === 'range'">
      <div class="clock-row"><label>开始时间（北京时间）<input type="datetime-local" :value="start" :disabled="disabled || busy || !ready"
        @change="changeStart(($event.target as HTMLInputElement).value)" /></label>
        <ElButton size="small" :disabled="disabled || busy || !ready" aria-label="以现在为开始时间" @click="changeStart(now())">现在</ElButton></div>
      <p role="status" aria-live="polite">
        <template v-if="busy">正在同步开始时间…</template>
        <template v-else-if="error">{{ error }}
          <ElButton v-if="ready" text size="small" :disabled="disabled" @click="changeStart(start)">重试保存</ElButton>
          <ElButton text size="small" :disabled="disabled" @click="load">重新读取</ElButton>
        </template>
        <template v-else-if="draft">开始时间已保存，可以退出。 <ElButton text size="small" :disabled="disabled" @click="changeStart('')">清除</ElButton></template>
      </p>
      <div class="clock-row"><label>结束时间（北京时间）<input v-model="end" type="datetime-local" :disabled="disabled || !draft || !ready || busy" /></label>
        <ElButton size="small" :disabled="disabled || !draft || !ready || busy" aria-label="以现在为结束时间" @click="end = now()">现在</ElButton></div>
      <p v-if="invalidEnd" role="alert">结束时间须晚于开始时间，请核对日期。</p>
      <p v-else-if="elapsed != null" role="status"><strong>本次用时 {{ elapsed }} 分钟</strong></p>
    </template>
    <template v-else>
      <ElInputNumber :model-value="modelValue ?? undefined" :min="0" :max="2147483647" :precision="0"
        :controls="false" :disabled="disabled" placeholder="可留空" aria-label="本次用时（分钟）"
        @update:model-value="value => emit('update:modelValue', typeof value === 'number' ? value : null)" />
      <span> 分钟</span><p>只填本次实际学习用时，扣除休息。</p>
    </template>
  </div>
</template>
<style scoped>
.learning-duration { width:100%; min-width:0; }
.mode { margin-bottom:8px; }
.clock-row { display:flex; align-items:end; gap:8px; }
.clock-row label { display:grid; flex:1; min-width:0; gap:2px; font-size:13px; }
.clock-row input { box-sizing:border-box; width:100%; min-width:0; padding:6px; border:1px solid var(--el-border-color); border-radius:4px; font:inherit; }
.learning-duration p { margin:4px 0; font-size:12px; line-height:1.6; color:var(--kaoyan-text-secondary); }
</style>
