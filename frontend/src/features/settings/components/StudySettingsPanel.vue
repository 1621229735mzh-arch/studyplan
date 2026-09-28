<script setup lang="ts">
/**
 * 学习设置：考试日期、每日学习预算、每日复习额度、复习阶段与复习间隔。
 *
 * 预算、额度与复习间隔都是待定项：允许留空（后端存 null），界面必须显示“未设置”，
 * 不能当作 0，也不能据此宣称满足额度。复习间隔由本人配置，应用不提供默认值；
 * 间隔未配置时后端不会推导下次复习日期。
 * 更新必须回传当前 version。
 */
import { onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
  ElForm,
  ElFormItem,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElTag
} from 'element-plus'

import { toApiError } from '@/api/http'
import { formatBudget, formatIntervalDays, reviewPhaseLabel } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import type { ReviewPhase } from '@/types/domain'
import { getStudySettings, updateStudySettings } from '../api'

const { online } = useOnlineStatus()

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const noticeMessage = ref<string | null>(null)

const form = reactive<{
  examDate: string
  dailyStudyMinutes: number | null
  dailyReviewMinutes: number | null
  reviewPhase: ReviewPhase
  reviewIntervalForgotDays: number | null
  reviewIntervalVagueDays: number | null
  reviewIntervalMasteredDays: number | null
}>({
  examDate: '',
  dailyStudyMinutes: null,
  dailyReviewMinutes: null,
  reviewPhase: 'MAIN',
  reviewIntervalForgotDays: null,
  reviewIntervalVagueDays: null,
  reviewIntervalMasteredDays: null
})

const version = ref<number | null>(null)

const PHASE_OPTIONS: ReviewPhase[] = ['MAIN', 'INTENSIVE']

/** 三个复习间隔档位：字段名与后端一致。 */
type IntervalField = 'reviewIntervalForgotDays' | 'reviewIntervalVagueDays' | 'reviewIntervalMasteredDays'

const INTERVAL_FIELDS: IntervalField[] = [
  'reviewIntervalForgotDays',
  'reviewIntervalVagueDays',
  'reviewIntervalMasteredDays'
]

async function load(): Promise<void> {
  if (!online.value) {
    errorMessage.value = '离线状态无法读取设置。'
    return
  }
  loading.value = true
  errorMessage.value = null
  try {
    const settings = await getStudySettings()
    form.examDate = settings.examDate ?? ''
    form.dailyStudyMinutes = settings.dailyStudyMinutes ?? null
    form.dailyReviewMinutes = settings.dailyReviewMinutes ?? null
    form.reviewPhase = settings.reviewPhase ?? 'MAIN'
    form.reviewIntervalForgotDays = settings.reviewIntervalForgotDays ?? null
    form.reviewIntervalVagueDays = settings.reviewIntervalVagueDays ?? null
    form.reviewIntervalMasteredDays = settings.reviewIntervalMasteredDays ?? null
    version.value = settings.version
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function onExamDateChange(value: unknown): void {
  form.examDate = typeof value === 'string' ? value : ''
}

function onStudyMinutesChange(value: unknown): void {
  form.dailyStudyMinutes = typeof value === 'number' ? value : null
}

function onReviewMinutesChange(value: unknown): void {
  form.dailyReviewMinutes = typeof value === 'number' ? value : null
}

function onPhaseChange(value: unknown): void {
  form.reviewPhase = value === 'INTENSIVE' ? 'INTENSIVE' : 'MAIN'
}

function onIntervalChange(field: IntervalField, value: unknown): void {
  // 清空输入表示“未设置”，存 null，不写成 0。
  form[field] = typeof value === 'number' ? value : null
}

/** 清空为“未设置”，而不是填 0。 */
function clearStudyMinutes(): void {
  form.dailyStudyMinutes = null
}

function clearReviewMinutes(): void {
  form.dailyReviewMinutes = null
}

function clearInterval(field: IntervalField): void {
  form[field] = null
}

const INTERVAL_LABELS: Record<IntervalField, string> = {
  reviewIntervalForgotDays: '不会（FORGOT）',
  reviewIntervalVagueDays: '模糊（VAGUE）',
  reviewIntervalMasteredDays: '掌握（MASTERED）'
}

async function handleSave(): Promise<void> {
  if (version.value === null) {
    ElMessage.warning('还没有读到设置版本，请先刷新')
    return
  }
  if (form.dailyStudyMinutes !== null && form.dailyStudyMinutes < 0) {
    errorMessage.value = '每日学习预算不能为负数'
    return
  }
  if (form.dailyReviewMinutes !== null && form.dailyReviewMinutes < 0) {
    errorMessage.value = '每日复习额度不能为负数'
    return
  }
  const negativeInterval = INTERVAL_FIELDS.find(
    (field) => form[field] !== null && (form[field] as number) < 0
  )
  if (negativeInterval !== undefined) {
    errorMessage.value = '复习间隔不能为负数'
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法保存设置，第一版离线为只读。')
    return
  }
  submitting.value = true
  errorMessage.value = null
  noticeMessage.value = null
  try {
    const settings = await updateStudySettings({
      examDate: form.examDate === '' ? null : form.examDate,
      dailyStudyMinutes: form.dailyStudyMinutes,
      dailyReviewMinutes: form.dailyReviewMinutes,
      reviewPhase: form.reviewPhase,
      reviewIntervalForgotDays: form.reviewIntervalForgotDays,
      reviewIntervalVagueDays: form.reviewIntervalVagueDays,
      reviewIntervalMasteredDays: form.reviewIntervalMasteredDays,
      version: version.value
    })
    version.value = settings.version
    form.dailyStudyMinutes = settings.dailyStudyMinutes ?? null
    form.dailyReviewMinutes = settings.dailyReviewMinutes ?? null
    form.reviewIntervalForgotDays = settings.reviewIntervalForgotDays ?? null
    form.reviewIntervalVagueDays = settings.reviewIntervalVagueDays ?? null
    form.reviewIntervalMasteredDays = settings.reviewIntervalMasteredDays ?? null
    noticeMessage.value = '设置已保存'
    ElMessage.success('设置已保存')
  } catch (caught) {
    const apiError = toApiError(caught)
    if (apiError.status === 409) {
      errorMessage.value = '设置已在其它设备被修改，已重新加载最新值，请确认后再保存。'
      await load()
    } else {
      errorMessage.value = apiError.message
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div>
    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />
    <ElAlert v-if="noticeMessage" type="success" show-icon :closable="false" :title="noticeMessage" />
    <ElAlert
      type="info"
      show-icon
      :closable="false"
      title="预算、额度与复习间隔都是待定项"
      description="第一版不擅自固定具体数值；留空表示“未设置”，计划与复习校验会按未设置处理，不会被当作 0。"
    />

    <ElForm label-width="150px" @submit.prevent>
      <ElFormItem label="考试时间">
        <ElDatePicker
          :model-value="form.examDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="可留空"
          @update:model-value="onExamDateChange"
        />
        <span class="hint-text">用于显示备考剩余时间，不参与进度计算。</span>
      </ElFormItem>

      <ElFormItem label="每日学习预算">
        <ElInputNumber
          :model-value="form.dailyStudyMinutes ?? undefined"
          :min="0"
          :step="10"
          controls-position="right"
          placeholder="未设置"
          @update:model-value="onStudyMinutesChange"
        />
        <span class="hint-text">分钟</span>
        <ElButton size="small" text type="primary" @click="clearStudyMinutes">清除（未设置）</ElButton>
        <ElTag size="small" :type="form.dailyStudyMinutes === null ? 'warning' : 'success'" effect="plain">
          {{ form.dailyStudyMinutes === null ? '当前：未设置' : `当前：${formatBudget(form.dailyStudyMinutes)}` }}
        </ElTag>
      </ElFormItem>

      <ElFormItem label="每日复习额度">
        <ElInputNumber
          :model-value="form.dailyReviewMinutes ?? undefined"
          :min="0"
          :step="10"
          controls-position="right"
          placeholder="未设置"
          @update:model-value="onReviewMinutesChange"
        />
        <span class="hint-text">分钟</span>
        <ElButton size="small" text type="primary" @click="clearReviewMinutes">清除（未设置）</ElButton>
        <ElTag size="small" :type="form.dailyReviewMinutes === null ? 'warning' : 'success'" effect="plain">
          {{ form.dailyReviewMinutes === null ? '当前：未设置' : `当前：${formatBudget(form.dailyReviewMinutes)}` }}
        </ElTag>
      </ElFormItem>

      <ElFormItem label="复习阶段">
        <ElSelect :model-value="form.reviewPhase" class="study-settings__select" @update:model-value="onPhaseChange">
          <ElOption v-for="phase in PHASE_OPTIONS" :key="phase" :label="reviewPhaseLabel(phase)" :value="phase" />
        </ElSelect>
        <span class="hint-text">
          当前：{{ reviewPhaseLabel(form.reviewPhase) }}。阶段由本人主动切换，集中复习阶段需要重新设置额度。
        </span>
      </ElFormItem>

      <ElFormItem
        v-for="field in INTERVAL_FIELDS"
        :key="field"
        :label="`复习间隔·${INTERVAL_LABELS[field]}`"
      >
        <ElInputNumber
          :model-value="form[field] ?? undefined"
          :min="0"
          :step="1"
          controls-position="right"
          placeholder="未设置"
          @update:model-value="(value) => onIntervalChange(field, value)"
        />
        <span class="hint-text">天</span>
        <ElButton size="small" text type="primary" @click="clearInterval(field)">清除（未设置）</ElButton>
        <ElTag size="small" :type="form[field] === null ? 'warning' : 'success'" effect="plain">
          {{ form[field] === null ? '当前：未设置' : `当前：${formatIntervalDays(form[field])}` }}
        </ElTag>
      </ElFormItem>

      <ElFormItem>
        <span class="hint-text">
          复习间隔由本人配置：某个档位未设置时，后端不会推导下次复习日期，界面也不会编造日期。
        </span>
      </ElFormItem>

      <ElFormItem>
        <ElButton type="primary" :loading="submitting" :disabled="!online" @click="handleSave">保存设置</ElButton>
        <ElButton :loading="loading" @click="load">重新读取</ElButton>
        <span class="hint-text">保存时会带上版本号（当前：{{ version ?? '未知' }}），多设备同时编辑时会提示冲突。</span>
      </ElFormItem>
    </ElForm>
  </div>
</template>

<style scoped>
.study-settings__select {
  width: 200px;
}
</style>
