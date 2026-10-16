<script setup lang="ts">
/**
 * 学习记录表单（新建 / 修改共用）。
 *
 * 设计要点：
 * - 3—5 分钟即可填完：默认今天、无需计时、复盘可选；
 * - 完成数量必须带单位，单位跟随所选任务展示，不允许手工输入单位；
 * - 离线（第一版只读）时不允许提交，但保留已填内容供联网后重试；
 * - 提交失败不清空表单，由父组件决定何时调用 reset()；
 * - 每次填写生成一个 clientToken，重试时复用，配合后端防重。
 *
 * 说明：数值/日期/下拉组件统一用 :model-value + @update:model-value 绑定，
 * 而不是 v-model——Element Plus 这些组件的 modelValue 是较宽的联合类型，
 * v-model 在 vue-tsc 下会报类型不兼容。
 */
import { computed, reactive, ref, watch } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElOption,
  ElSelect,
  ElTag
} from 'element-plus'

import SequenceRecordItem from './SequenceRecordItem.vue'
import AmountUnit from '@/components/AmountUnit.vue'
import { todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import type { FieldViolation } from '@/types/api'
import type { Subject, StudyUnit } from '@/types/domain'
import type {
  LearningRecordFormModel,
  LearningRecordFormSubmission,
  LearningRecordTaskOption
} from '../types'
import { validateLearningRecordForm } from '../recordEditorValidation'

const props = withDefaults(
  defineProps<{
    /** 任务候选项（今日计划任务或全部任务） */
    tasks?: LearningRecordTaskOption[]
    subjects?: Subject[]
    units?: StudyUnit[]
    /** 修改模式：按钮文案与提示不同 */
    mode?: 'create' | 'edit'
    /** 提交中：禁用按钮避免重复提交 */
    continued?: boolean
    progressDelta?: number | null
    submitting?: boolean
  }>(),
  {
    tasks: () => [],
    subjects: () => [],
    units: () => [],
    mode: 'create',
    submitting: false
  }
)

const emit = defineEmits<{
  (event: 'submit', payload: LearningRecordFormSubmission): void
  (event: 'cancel'): void
}>()

const { online } = useOnlineStatus()


function createEmptyModel(): LearningRecordFormModel {
  return {
    recordDate: todayIso(),
    taskId: null,
    sequenceItemId: null,
    subjectId: null,
    unitId: null,
    amount: null,
    durationMinutes: null,
    content: '',
    note: ''
  }
}

const form = reactive<LearningRecordFormModel>(createEmptyModel())
const fieldErrors = ref<Partial<Record<keyof LearningRecordFormModel, string>>>({})
const formError = ref<string | null>(null)

/** 幂等标识（对应后端 clientToken）：一次填写一个，重试复用；提交成功后由 reset() 重新生成。 */
function newClientToken(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `token-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

const clientToken = ref(newClientToken())
const sequenceReady = ref(true)
let submittedSignature: string | null = null

const selectedTask = computed<LearningRecordTaskOption | null>(
  () => props.tasks.find((task) => task.id === form.taskId) ?? null
)

/** 单位来自任务，只读展示。 */
const unitName = computed(() => selectedTask.value?.unitName ?? props.units.find(unit => unit.id === form.unitId)?.name ?? null)
const subjectName = computed(() => selectedTask.value?.subjectName ?? props.subjects.find(subject => subject.id === form.subjectId)?.name ?? null)

const submitLabel = computed(() => (props.mode === 'edit' ? '保存修改' : '记一笔'))


// 选择任务后自动带出科目与单位（单位不可手填，避免不同单位混用）
watch(selectedTask, (task) => {
  if (task === null) {
    return
  }
  form.unitId = task.unitId ?? null
  form.subjectId = task.subjectId ?? null
  if (task.unitId === null || task.unitId === undefined) {
    fieldErrors.value = { ...fieldErrors.value, taskId: '该任务缺少计量单位，请先在“设置”中补充' }
  }
})

// Element Plus 输入组件的回调：入参用 unknown 接收，避免类型不兼容
function onDateChange(value: unknown): void {
  form.recordDate = typeof value === 'string' ? value : ''
}

function onTaskChange(value: unknown): void {
  form.sequenceItemId = null
  sequenceReady.value = true
  form.taskId = typeof value === 'number' ? value : null
  if (form.taskId === null) { form.subjectId = null; form.unitId = null }
}

function onAmountChange(value: unknown): void {
  form.amount = typeof value === 'number' ? value : null
}



/** 提交：先本地校验（同步、可测），再判断离线，最后把载荷交给父组件。 */
function submit(): void {
  if (props.submitting || !sequenceReady.value) return
  const result = validateLearningRecordForm(form, props.mode)
  fieldErrors.value = result.errors
  formError.value = result.summary
  if (!result.valid) {
    return
  }
  if (!online.value) {
    // 第一版离线只读：不排队、不假装成功，保留输入。
    formError.value = '当前处于离线状态，第一版不支持离线提交。已填写的内容会保留，联网后可直接重试。'
    return
  }
  const signature = JSON.stringify(form)
  if (submittedSignature !== null && signature !== submittedSignature) clientToken.value = newClientToken()
  submittedSignature = signature
  emit('submit', { model: { ...form }, clientToken: clientToken.value })
}

/** 后端返回的字段错误（例如任务不存在）绑定回表单。 */
function setServerError(message: string, violations: FieldViolation[] = []): void {
  formError.value = message
  const next: Partial<Record<keyof LearningRecordFormModel, string>> = {}
  for (const violation of violations) {
    const field = violation.field as keyof LearningRecordFormModel
    if (field in form) {
      next[field] = violation.message
    }
  }
  fieldErrors.value = { ...fieldErrors.value, ...next }
}

/** 提交成功后重置表单；幂等标识同时更新。 */
function reset(): void {
  Object.assign(form, createEmptyModel())
  fieldErrors.value = {}
  formError.value = null
  clientToken.value = newClientToken()
  submittedSignature = null
}

/** 修改模式：用已有记录填充表单。 */
function fillFrom(model: Partial<LearningRecordFormModel>): void {
  Object.assign(form, createEmptyModel(), model)
  fieldErrors.value = {}
  formError.value = null
}

defineExpose({ form, submit, reset, fillFrom, setServerError })
</script>

<template>
  <ElForm class="record-form" label-position="top" :disabled="submitting" @submit.prevent>
    <ElAlert
      v-if="!online"
      class="record-form__alert"
      type="warning"
      show-icon
      :closable="false"
      title="离线状态"
      description="第一版离线只读，无法提交学习记录；已填内容会保留，联网后可直接重试。"
    />

    <div class="record-form__grid">
      <SequenceRecordItem v-if="selectedTask?.sequential || form.sequenceItemId" :task-id="form.taskId" :date="form.recordDate" :item-id="form.sequenceItemId" :editing="mode === 'edit'" @item="form.sequenceItemId = $event" @ready="sequenceReady = $event" />
      <ElFormItem label="日期" :error="fieldErrors.recordDate">
        <ElDatePicker
          :model-value="form.recordDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="记录日期"
          :disabled="continued"
          @update:model-value="onDateChange"
        />
      </ElFormItem>

      <ElFormItem label="任务" :error="fieldErrors.taskId">
        <ElSelect
          :model-value="form.taskId ?? undefined"
          :disabled="continued"
          clearable
          filterable
          placeholder="选择任务（可留空记计划外学习）"
          @update:model-value="onTaskChange"
        >
          <ElOption v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
        </ElSelect>
      </ElFormItem>

      <ElFormItem class="record-form__wide" label="学习内容" :error="fieldErrors.content">
        <ElInput v-model="form.content" maxlength="500" placeholder="学了什么？例如：高数极限习题、英语长难句（可选）" />
      </ElFormItem>

      <template v-if="!form.taskId && (subjects.length || units.length)">
        <ElFormItem label="科目" :error="fieldErrors.subjectId">
          <ElSelect :model-value="form.subjectId ?? undefined" @update:model-value="value => { form.subjectId = typeof value === 'number' ? value : null }" clearable placeholder="选择科目（可选）"><ElOption v-for="subject in subjects.filter(s => s.enabled || s.id === form.subjectId)" :key="subject.id" :label="subject.name" :value="subject.id" /></ElSelect>
        </ElFormItem>
        <ElFormItem label="计量单位" :error="fieldErrors.unitId">
          <ElSelect :model-value="form.unitId ?? undefined" @update:model-value="value => { form.unitId = typeof value === 'number' ? value : null }" clearable placeholder="填写完成量时选择"><ElOption v-for="unit in units.filter(u => u.enabled || u.id === form.unitId)" :key="unit.id" :label="unit.name" :value="unit.id" /></ElSelect>
        </ElFormItem>
      </template>

      <ElFormItem v-if="!continued" label="完成量" :error="fieldErrors.amount">
        <div class="record-form__inline">
          <ElInputNumber
            :model-value="form.amount ?? undefined"
            :min="0"
            :step="1"
            controls-position="right"
            @update:model-value="onAmountChange"
          />
          <AmountUnit :amount="form.amount" :unit="unitName" placeholder="未填" />
        </div>
      </ElFormItem>

      <p v-if="continued" class="record-form__wide">本次增加进度：{{ progressDelta ?? 0 }}%。需要更正进度时删除此记录，再填写正确百分比。</p>

      <ElFormItem class="record-form__wide" label="复盘" :error="fieldErrors.note">
        <ElInput
          v-model="form.note"
          type="textarea"
          :rows="2"
          maxlength="1000"
          show-word-limit
          placeholder="简短复盘：卡在哪里、下一步怎么做（可留空）"
        />
      </ElFormItem>

      <ElFormItem v-if="form.taskId" label="科目/单位">
        <div class="record-form__inline">
          <ElTag size="small" effect="plain">{{ subjectName ?? '未关联科目' }}</ElTag>
          <ElTag size="small" type="info" effect="plain">
            {{ unitName ? `单位：${unitName}` : '单位来自任务' }}
          </ElTag>
        </div>
      </ElFormItem>
    </div>

    <ElAlert
      v-if="formError"
      class="record-form__alert"
      type="error"
      show-icon
      :closable="false"
      :title="formError"
    />

    <div class="form-actions">
      <ElButton type="primary" :loading="submitting" :disabled="!online || !sequenceReady" @click="submit">
        {{ submitLabel }}
      </ElButton>
      <ElButton v-if="mode === 'edit'" :disabled="submitting" @click="emit('cancel')">取消</ElButton>
    </div>
  </ElForm>
</template>

<style scoped>
.record-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.record-form__inline {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.record-form__alert {
  margin-bottom: 12px;
}
.record-form__wide { grid-column:1 / -1; }
.record-form :deep(.el-date-editor),.record-form :deep(.el-select) { width:100%; }
@media(max-width:600px) { .record-form__grid { grid-template-columns:minmax(0,1fr); } }
</style>
