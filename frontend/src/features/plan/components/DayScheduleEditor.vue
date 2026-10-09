<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElAlert, ElButton, ElDatePicker, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElOption, ElRadioButton, ElRadioGroup, ElSelect } from 'element-plus'
import { toApiError } from '@/api/http'
import { useCatalogStore } from '@/stores/catalog'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { adjustDayPlanItem } from '../api'
import { quickAddDay } from '../api/workspace'
import type { DailyPlanItemView, PlanTask } from '../types'

const props = defineProps<{ modelValue: boolean; date: string; item?: DailyPlanItemView | null; tasks: PlanTask[] }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [string] }>()
const catalog = useCatalogStore()
const { online } = useOnlineStatus()
const busy = ref(false)
const error = ref('')
const token = ref('')
const form = reactive({ date: '', mode: 'new', title: '', subjectId: undefined as number | undefined,
  unitId: undefined as number | undefined, existingTaskId: undefined as number | undefined, amount: 1, minutes: undefined as number | undefined })
const editing = computed(() => !!props.item)
watch(() => props.modelValue, open => {
  if (!open) return
  error.value = ''
  Object.assign(form, { date: props.date, mode: 'new', title: props.item?.taskTitle ?? '', subjectId: props.item?.subjectId ?? undefined,
    unitId: props.item?.unitId ?? undefined, existingTaskId: undefined, amount: props.item?.plannedAmount ?? 1, minutes: props.item?.estimatedMinutes ?? undefined })
  token.value = crypto.randomUUID()
})
watch(form, () => { token.value = crypto.randomUUID() })
async function save() {
  if (busy.value || !online.value) return
  if (!form.date || !Number.isFinite(form.amount) || form.amount < (editing.value ? 0 : 0.01)) { error.value = '请填写日期和有效的计划量'; return }
  if (!editing.value && (form.mode === 'new' ? (!form.title.trim() || !form.subjectId || !form.unitId) : !form.existingTaskId)) {
    error.value = form.mode === 'new' ? '请填写学习内容、科目和单位' : '请选择已有任务'; return
  }
  busy.value = true; error.value = ''
  try {
    if (props.item) await adjustDayPlanItem(form.date, props.item.id, { plannedAmount: form.amount, estimatedMinutes: form.minutes ?? null, version: props.item.version, reason: '计划页手动调整' })
    else await quickAddDay(form.date, { ...(form.mode === 'existing' ? { existingTaskId: form.existingTaskId } : { subjectId: form.subjectId, unitId: form.unitId, title: form.title.trim() }),
      plannedAmount: form.amount, estimatedMinutes: form.minutes ?? null, clientToken: token.value })
    ElMessage.success(editing.value ? '安排已更新' : '已加入安排')
    emit('update:modelValue', false); emit('saved', form.date)
  } catch (caught) { error.value = toApiError(caught).message }
  finally { busy.value = false }
}
</script>

<template>
<ElDialog :model-value="modelValue" :title="editing ? '调整安排' : '添加日安排'" width="min(560px, calc(100vw - 24px))" :close-on-click-modal="!busy" :close-on-press-escape="!busy" :show-close="!busy" @update:model-value="emit('update:modelValue', $event)">
    <ElAlert v-if="error" :title="error" type="error" :closable="false" style="margin-bottom: 16px" />
    <ElForm label-position="top" @submit.prevent="save">
      <ElFormItem label="安排日期"><ElDatePicker v-model="form.date" type="date" value-format="YYYY-MM-DD" :clearable="false" :disabled="editing || busy" /></ElFormItem>
      <ElRadioGroup v-if="!editing" v-model="form.mode" :disabled="busy" style="margin-bottom: 20px"><ElRadioButton value="new">新学习内容</ElRadioButton><ElRadioButton value="existing">已有任务</ElRadioButton></ElRadioGroup>
      <template v-if="editing || form.mode === 'new'">
        <ElFormItem label="学习内容"><ElInput v-model="form.title" maxlength="200" placeholder="例如：高数第一讲 · 极限" :disabled="editing || busy" /></ElFormItem>
        <div class="editor-columns">
          <ElFormItem label="科目"><ElSelect v-model="form.subjectId" placeholder="选择科目" :disabled="editing || busy"><ElOption v-for="subject in catalog.enabledSubjects" :key="subject.id" :label="subject.name" :value="subject.id" /></ElSelect></ElFormItem>
          <ElFormItem label="单位"><ElSelect v-model="form.unitId" placeholder="选择单位" :disabled="editing || busy"><ElOption v-for="unit in catalog.enabledUnits" :key="unit.id" :label="unit.name" :value="unit.id" /></ElSelect></ElFormItem>
        </div>
      </template>
      <ElFormItem v-else label="已有任务"><ElSelect v-model="form.existingTaskId" filterable placeholder="搜索学习任务" :disabled="busy"><ElOption v-for="task in tasks.filter(t => t.status === 'ACTIVE')" :key="task.id" :label="`${catalog.subjectNameOf(task.subjectId)} · ${task.title}（${catalog.unitNameOf(task.unitId)}）`" :value="task.id" /></ElSelect></ElFormItem>
      <div class="editor-columns">
        <ElFormItem label="当日计划量"><ElInputNumber v-model="form.amount" :min="editing ? 0 : 0.01" :max="99999999.99" :precision="2" :disabled="busy" /></ElFormItem>
        <ElFormItem label="预计用时（分钟，可留空）"><ElInputNumber v-model="form.minutes" :min="0" :max="2147483647" :precision="0" :disabled="busy" /></ElFormItem>
      </div>
    </ElForm>
    <template #footer><ElButton :disabled="busy" @click="emit('update:modelValue', false)">取消</ElButton><ElButton type="primary" :disabled="!online" :loading="busy" @click="save">保存安排</ElButton></template>
  </ElDialog>
</template>

<style scoped>
.editor-columns { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 16px; }
.editor-columns :deep(.el-input-number) { width: 100%; }
@media(max-width: 450px) { .editor-columns { grid-template-columns: 1fr; gap: 0; } }
</style>
