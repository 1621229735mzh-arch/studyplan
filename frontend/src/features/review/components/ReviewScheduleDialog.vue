<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElAlert, ElButton, ElDatePicker, ElDialog, ElForm, ElFormItem, ElOption, ElSelect } from 'element-plus'
import { toApiError } from '@/api/http'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { scheduleReview } from '../api/workspace'
import type { ReviewItem } from '../types'

const props = defineProps<{ modelValue: boolean; date: string; items: ReviewItem[]; itemId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [date: string]; library: [] }>()
const { online } = useOnlineStatus()
const date = ref(''); const itemId = ref<number>(); const saving = ref(false); const error = ref('')
const options = computed(() => props.items.filter(i => i.status !== 'ARCHIVED'))
const selected = computed(() => options.value.find(i => i.id === itemId.value))
watch(() => props.modelValue, open => {
  if (open) { date.value = props.date; itemId.value = props.itemId; error.value = '' }
}, { immediate: true })
async function save() {
  if (saving.value || !online.value || !selected.value || !date.value) return
  saving.value = true; error.value = ''
  try {
    await scheduleReview(selected.value.id, date.value, selected.value.version)
    emit('saved', date.value); emit('update:modelValue', false)
  } catch (caught) { error.value = toApiError(caught).message }
  finally { saving.value = false }
}
</script>
<template>
  <ElDialog :model-value="modelValue" :title="props.itemId ? '调整复习安排' : '添加复习安排'" width="min(580px, calc(100vw - 24px))" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @update:model-value="(value: boolean) => { if (!saving) emit('update:modelValue', value) }">
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <ElForm label-position="top" @submit.prevent="save">
      <ElFormItem label="复习日期"><ElDatePicker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" :disabled="saving || !online" /></ElFormItem>
      <ElFormItem label="已加入复习的内容"><ElSelect v-model="itemId" filterable placeholder="选择已有复习内容" :disabled="saving || !online || props.itemId != null" style="width:100%"><ElOption v-for="item in options" :key="item.id" :value="item.id" :label="item.title" /></ElSelect></ElFormItem>
    </ElForm>
    <p v-if="selected?.scheduledDate && selected.scheduledDate !== date" class="review-hint">该内容目前安排在 {{ selected.scheduledDate }}，保存后将调整到 {{ date }}。已有复习记录保留。</p>
    <p v-else class="review-hint">确认后进入所选日期的对应学科清单，无需填写用时。</p>
    <div v-if="!options.length" class="review-hint">还没有可安排的复习内容。<ElButton text :disabled="saving || !online" @click="emit('update:modelValue', false); emit('library')">查看复习内容库</ElButton></div>
    <template #footer><ElButton :disabled="saving" @click="emit('update:modelValue', false)">取消</ElButton><ElButton type="primary" :loading="saving" :disabled="!online || !selected || !date" @click="save">确认安排</ElButton></template>
  </ElDialog>
</template>
<style scoped>.review-hint { color:var(--kaoyan-text-secondary); font-size:13px; line-height:1.8; }.el-form { margin-top:18px; }</style>
