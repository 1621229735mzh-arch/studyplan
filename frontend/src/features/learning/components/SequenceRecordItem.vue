<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { get, toApiError } from '@/api/http'
import type { SequenceState } from '../types'
const props = defineProps<{ taskId: number | null; date: string; itemId?: number | null; editing: boolean }>()
const emit = defineEmits<{ item: [number | null]; ready: [boolean] }>()
const state = ref<SequenceState | null>(null)
const error = ref('')
const loading = ref(false)
const item = computed(() => state.value?.items.find(i => i.id === props.itemId) ?? state.value?.dayItem)
let request = 0
async function load() {
  const token = ++request
  state.value = null; error.value = ''; loading.value = false
  if (!props.taskId || !props.date) { emit('item', null); emit('ready', true); return }
  loading.value = true; emit('ready', false)
  try {
    const result = await get<SequenceState | null>(`/learning/sequences/${props.taskId}`, { params: { date: props.date } })
    if (token !== request) return
    state.value = result || null
    if (!props.editing || !props.itemId) emit('item', result?.dayItem?.id ?? null)
    emit('ready', !result || !!(props.editing ? props.itemId || result.dayItem : result.dayItem))
  } catch (caught) { if (token === request) error.value = toApiError(caught).message }
  finally { if (token === request) loading.value = false }
}
watch(() => [props.taskId, props.date], load, { immediate: true })
</script>
<template>
  <div v-if="loading || error || state" class="sequence-record" role="status">
    <span v-if="loading">正在核对接续题目…</span>
    <template v-else-if="error">{{ error }} <button type="button" @click="load">重试</button></template>
    <template v-else-if="state">
      <a v-if="item" :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.title }} ↗</a>
      <span v-else>题目清单已全部完成</span>
      <p>完成整题填写 1；尝试未完成请留空完成量、填写用时。每天最多完成 1 题。</p>
    </template>
  </div>
</template>
<style scoped>.sequence-record{grid-column:1 / -1;padding:10px 14px;background:#f3f7f4;border-radius:8px;margin-bottom:12px;font-size:13px}.sequence-record p{margin:6px 0 0;color:#64756b}</style>
