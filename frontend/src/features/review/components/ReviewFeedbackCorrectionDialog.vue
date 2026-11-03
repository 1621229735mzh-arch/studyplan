<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { ElAlert, ElButton, ElDialog, ElMessage, ElRadioGroup, ElRadioButton } from 'element-plus'
import { toApiError } from '@/api/http'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { masteryLabel } from '@/composables/useFormat'
import type { MasteryLevel } from '@/types/domain'
import { getLatestReviewFeedback, correctReviewFeedback } from '../api'
import type { ReviewFeedbackContext } from '../types'

const props = defineProps<{ modelValue: boolean; itemId: number | null; title: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; saved: [] }>()
const { online } = useOnlineStatus()
const context = ref<ReviewFeedbackContext | null>(null)
const result = ref<MasteryLevel>('VAGUE')
const loading = ref(false); const saving = ref(false); const error = ref(''); const stale = ref(false)
let generation = 0
let attempt: { signature: string; token: string } | null = null
async function load() {
  const id = ++generation; context.value = null; error.value = ''; stale.value = false; attempt = null; loading.value = false
  if (!props.modelValue || props.itemId == null || !online.value) return
  loading.value = true
  try {
    const response = await getLatestReviewFeedback(props.itemId)
    if (id !== generation) return
    context.value = response
    if (response.record?.result) result.value = response.record.result
  } catch (caught) { if (id === generation) error.value = toApiError(caught).message }
  finally { if (id === generation) loading.value = false }
}
async function save() {
  const current = context.value
  if (saving.value || !online.value || stale.value || !current?.allowed || !current.record || props.itemId == null) return
  const signature = JSON.stringify([props.itemId, current.version, current.record.id, result.value])
  if (attempt?.signature !== signature) attempt = { signature, token: crypto.randomUUID() }
  saving.value = true; error.value = ''
  try {
    await correctReviewFeedback(props.itemId, { version: current.version, recordId: current.record.id, result: result.value, clientToken: attempt!.token })
    ElMessage.success('反馈已更正，掌握状态和下次日期已重算')
    emit('saved'); emit('update:modelValue', false)
  } catch (caught) {
    const api = toApiError(caught); error.value = api.message
    if (api.status === 409) stale.value = true
  } finally { saving.value = false }
}
watch(() => [props.modelValue, props.itemId], () => { if (!saving.value) void load() }, { immediate: true })
onBeforeUnmount(() => { generation++ })
</script>
<template>
  <ElDialog :model-value="modelValue" title="更正最近反馈" width="min(480px, 92vw)" :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving" :before-close="(done: () => void) => { if (!saving) done() }" @update:model-value="(value: boolean) => { if (!saving) emit('update:modelValue', value) }">
    <p>{{ title }}</p>
    <p class="hint-text">更正不增加复习次数，原日期、进度和用时保留，并保存更正历史。系统会重算掌握状态和下次日期，替换当前手动改期；暂停、归档不会恢复。手动复习按当前间隔设置重算，并回到待安排。</p>
    <ElAlert v-if="!online" title="联网后才能读取和更正反馈" type="info" :closable="false" />
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="loading" role="status">正在读取最近反馈…</p>
    <template v-else-if="context">
      <p v-if="context.record">{{ context.record.reviewDate }} · 原反馈：{{ masteryLabel(context.record.result ?? null) }} · 原下次日期：{{ context.record.nextReviewDate ?? '待安排' }}</p>
      <p v-if="context.currentNextReviewDate !== undefined" class="hint-text">当前下次日期：{{ context.currentNextReviewDate ?? '待定' }} · 已确认安排：{{ context.scheduledDate ?? '无' }}。更正后将替换这些日期。</p>
      <ElAlert v-if="!context.allowed" :title="context.reason" type="info" :closable="false" />
      <ElRadioGroup v-else v-model="result" :disabled="saving || !online || stale" aria-label="更正掌握情况">
        <ElRadioButton value="FORGOT">不会</ElRadioButton><ElRadioButton value="VAGUE">模糊</ElRadioButton><ElRadioButton value="MASTERED">掌握</ElRadioButton>
      </ElRadioGroup>
    </template>
    <template #footer>
      <ElButton :disabled="saving || loading || !online" @click="load">重新加载反馈</ElButton>
      <ElButton :disabled="saving" @click="emit('update:modelValue', false)">取消</ElButton>
      <ElButton type="primary" :loading="saving" :disabled="!online || loading || stale || !context?.allowed || result === context?.record?.result" @click="save">更正并重算日期</ElButton>
    </template>
  </ElDialog>
</template>
