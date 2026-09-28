<script setup lang="ts">
/**
 * 复习建议与确认面板。
 *
 * 对应后端 ReviewController / dto：
 * - `GET /api/review/suggestions?date=` 返回单个对象：
 *   `{date, dailyReviewMinutes, scheduledMinutes, remainingMinutes, items[], warnings[]}`；
 *   没有 quotaStatus / suggestedDate / subjectName 之类的字段。
 * - `POST /api/review/suggestions/confirm`，body `{date, items:[{reviewItemId, estimatedMinutes}]}`，
 *   返回 `{date, dailyReviewMinutes, usedMinutes, scheduled[], rejected[]}`。
 *
 * 业务约束（PLAN.md 二.3）：
 * - 建议必须经本人确认才进入今日安排，未经确认不会自动占用今天的时间；
 * - 缺少额度或预计用时时，界面必须如实说明“无法判断”，不能声称建议符合额度（见 ../quota.ts）；
 * - 超额或已变化的建议由后端拒绝，界面原样展示拒绝原因。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElCheckbox,
  ElDatePicker,
  ElInputNumber,
  ElMessage,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'

import { toApiError } from '@/api/http'
import EmptyState from '@/components/EmptyState.vue'
import { formatIsoDate, formatMinutesOr, todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import { confirmReviewSuggestions, getReviewSuggestions } from '../api'
import { quotaClaim, quotaOverviewText } from '../quota'
import type { ReviewConfirmRejected, ReviewItem, ReviewSuggestionResponse } from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const date = ref(todayIso())
const suggestion = ref<ReviewSuggestionResponse | null>(null)
const selected = ref<number[]>([])
/** 每条建议由本人确认的用时；null 表示尚未补充（无法校验额度） */
const plannedMinutes = reactive<Record<number, number | null>>({})

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const noticeMessage = ref<string | null>(null)
const rejected = ref<ReviewConfirmRejected[]>([])
/** 后端确认进入当天安排的条目（ReviewItemResponse） */
const scheduled = ref<ReviewItem[]>([])
const usedMinutes = ref<number | null>(null)

const items = computed(() => suggestion.value?.items ?? [])
/** 后端给出的缺失输入说明，原样展示。 */
const warnings = computed(() => suggestion.value?.warnings ?? [])
/** 每日复习额度；null/undefined 表示未设置（不能当成 0）。 */
const quotaMinutes = computed(() => suggestion.value?.dailyReviewMinutes ?? null)
const quotaMissing = computed(() => quotaMinutes.value === null)

const quotaText = computed(() =>
  quotaOverviewText(
    suggestion.value?.dailyReviewMinutes ?? null,
    suggestion.value?.scheduledMinutes ?? null,
    suggestion.value?.remainingMinutes ?? null,
    warnings.value
  )
)

/** 单条建议能否宣称符合额度（缺额度或缺用时都不能）。 */
function itemQuotaClaim(reviewItemId: number): { claimsFit: boolean; reason: string | null } {
  return quotaClaim(quotaMinutes.value, plannedMinutes[reviewItemId], warnings.value)
}

function itemQuotaText(reviewItemId: number): string {
  const claim = itemQuotaClaim(reviewItemId)
  return claim.claimsFit ? '额度与用时齐全，可提交确认（是否放得下以后端确认结果为准）' : (claim.reason ?? '')
}

function itemQuotaTagType(reviewItemId: number): 'success' | 'warning' {
  return itemQuotaClaim(reviewItemId).claimsFit ? 'success' : 'warning'
}

function subjectNameOf(subjectId?: number | null): string | null {
  return catalog.subjectNameOf(subjectId)
}

async function load(): Promise<void> {
  if (!online.value) {
    errorMessage.value = '离线状态无法读取复习建议。'
    return
  }
  loading.value = true
  errorMessage.value = null
  noticeMessage.value = null
  try {
    const response = await getReviewSuggestions(date.value)
    suggestion.value = response
    for (const item of response.items) {
      if (plannedMinutes[item.reviewItemId] === undefined) {
        // 预填后端给出的预计用时；缺失时留空，由本人补充
        plannedMinutes[item.reviewItemId] = item.estimatedMinutes ?? null
      }
    }
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function onDateChange(value: unknown): void {
  date.value = typeof value === 'string' && value ? value : todayIso()
  void load()
}

function toggleSelection(reviewItemId: number, checked: unknown): void {
  const next = new Set(selected.value)
  if (checked === true) {
    next.add(reviewItemId)
  } else {
    next.delete(reviewItemId)
  }
  selected.value = [...next]
}

function onMinutesChange(reviewItemId: number, value: unknown): void {
  plannedMinutes[reviewItemId] = typeof value === 'number' ? value : null
}

async function confirmSelected(): Promise<void> {
  const chosen = items.value.filter((item) => selected.value.includes(item.reviewItemId))
  if (chosen.length === 0) {
    ElMessage.warning('请先勾选要确认的复习建议')
    return
  }
  const missing = chosen.filter((item) => {
    const value = plannedMinutes[item.reviewItemId]
    return value === null || value === undefined
  })
  if (missing.length > 0) {
    ElMessage.warning('选中的建议里还有缺少预计用时的条目，请先补充用时再确认（缺少用时无法判断额度）')
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法确认建议，第一版离线为只读。')
    return
  }
  if (quotaMissing.value) {
    // 后端在未设置额度时会直接拒绝（REVIEW_QUOTA_REQUIRED），这里先说清楚，不去撞这个错误。
    ElMessage.warning('尚未设置每日复习额度，后端会拒绝确认；请先到“设置”里填写每日复习额度。')
    return
  }

  submitting.value = true
  errorMessage.value = null
  noticeMessage.value = null
  try {
    const result = await confirmReviewSuggestions({
      date: date.value,
      items: chosen.map((item) => ({
        reviewItemId: item.reviewItemId,
        estimatedMinutes: plannedMinutes[item.reviewItemId] as number
      }))
    })
    usedMinutes.value = result.usedMinutes
    rejected.value = result.rejected ?? []
    scheduled.value = result.scheduled ?? []
    noticeMessage.value = `本次确认 ${result.scheduled.length} 条；当天已安排复习用时 ${result.usedMinutes} 分钟${
      result.dailyReviewMinutes === null || result.dailyReviewMinutes === undefined
        ? ''
        : `（额度 ${result.dailyReviewMinutes} 分钟）`
    }。`
    ElMessage.success(`已确认 ${result.scheduled.length} 条进入当天安排`)
    selected.value = []
    await load()
  } catch (caught) {
    const apiError = toApiError(caught)
    if (apiError.code === 'REVIEW_QUOTA_REQUIRED') {
      errorMessage.value = '尚未设置每日复习额度，后端拒绝了本次确认；请先到“设置”里填写每日复习额度。'
    } else {
      errorMessage.value =
        apiError.status === 409 ? '建议已过期或计划已变化，请刷新后重新确认。' : apiError.message
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void catalog.load()
  void load()
})
</script>

<template>
  <div>
    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />
    <ElAlert v-if="noticeMessage" type="success" show-icon :closable="false" :title="noticeMessage" />

    <ElAlert
      :type="quotaMissing || warnings.length > 0 ? 'warning' : 'info'"
      show-icon
      :closable="false"
      title="复习额度"
      :description="quotaText"
    />

    <ElAlert v-if="warnings.length > 0" type="warning" show-icon :closable="false" title="后端提示（缺少的输入）">
      <ul class="suggestion-list">
        <li v-for="warning in warnings" :key="warning">{{ warning }}</li>
      </ul>
    </ElAlert>

    <div class="suggestion-toolbar">
      <ElDatePicker
        :model-value="date"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="查看并确认到哪一天"
        @update:model-value="onDateChange"
      />
      <ElButton type="primary" :loading="submitting" :disabled="!online" @click="confirmSelected">
        确认进入当天安排
      </ElButton>
      <ElButton :loading="loading" @click="load">刷新建议</ElButton>
      <span class="hint-text">只有确认后的建议才会进入当天安排，未确认的不会占用学习时间。</span>
    </div>

    <div class="metric-grid">
      <div>
        <div class="metric-label">每日复习额度</div>
        <div class="metric-value">{{ quotaMinutes === null ? '未设置' : `${quotaMinutes} 分钟` }}</div>
      </div>
      <div>
        <div class="metric-label">当天已安排</div>
        <div class="metric-value">{{ formatMinutesOr(suggestion?.scheduledMinutes ?? null, '未知') }}</div>
      </div>
      <div>
        <div class="metric-label">剩余额度</div>
        <div class="metric-value">{{ formatMinutesOr(suggestion?.remainingMinutes ?? null, '未提供（未设额度）') }}</div>
      </div>
      <div v-if="usedMinutes !== null">
        <div class="metric-label">本次确认后已安排</div>
        <div class="metric-value">{{ formatMinutesOr(usedMinutes, '未知') }}</div>
      </div>
    </div>

    <ElAlert v-if="scheduled.length > 0" type="success" show-icon :closable="false" title="已进入当天安排的复习内容">
      <ul class="suggestion-list">
        <li v-for="item in scheduled" :key="item.id">
          {{ item.title }}
          <span class="hint-text">
            （{{ formatIsoDate(item.scheduledDate) }}，{{ formatMinutesOr(item.estimatedMinutes, '未提供用时') }}）
          </span>
        </li>
      </ul>
    </ElAlert>

    <ElAlert v-if="rejected.length > 0" type="warning" show-icon :closable="false" title="部分建议未能进入安排">
      <ul class="suggestion-list">
        <li v-for="item in rejected" :key="item.reviewItemId">
          {{ item.title ?? `复习项 #${item.reviewItemId}` }}：{{ item.reason }}
        </li>
      </ul>
    </ElAlert>

    <EmptyState
      v-if="!loading && items.length === 0"
      description="这一天没有到期的复习建议"
      hint="复习建议基于已加入复习的内容生成；先学习并把需要巩固的内容加入复习。"
    />

    <ElTable v-else :data="items" size="small" stripe>
      <ElTableColumn label="选择" width="70">
        <template #default="{ row }">
          <ElCheckbox
            :model-value="selected.includes(row.reviewItemId)"
            @update:model-value="(value) => toggleSelection(row.reviewItemId, value)"
          />
        </template>
      </ElTableColumn>
      <ElTableColumn label="内容" min-width="200">
        <template #default="{ row }">
          <div>{{ row.title }}</div>
          <ElTag v-if="subjectNameOf(row.subjectId)" size="small" effect="plain">
            {{ subjectNameOf(row.subjectId) }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="到期日" width="130">
        <template #default="{ row }">
          {{ formatIsoDate(row.dueDate) }}
          <div v-if="row.overdueDays > 0" class="hint-text">已积压 {{ row.overdueDays }} 天</div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="预计用时" width="180">
        <template #default="{ row }">
          <ElInputNumber
            :model-value="plannedMinutes[row.reviewItemId] ?? undefined"
            :min="1"
            :step="5"
            size="small"
            controls-position="right"
            placeholder="需补充"
            @update:model-value="(value) => onMinutesChange(row.reviewItemId, value)"
          />
          <span class="hint-text">分钟</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="额度判断" min-width="240">
        <template #default="{ row }">
          <ElTag :type="itemQuotaTagType(row.reviewItemId)" size="small" effect="plain">
            {{ itemQuotaText(row.reviewItemId) }}
          </ElTag>
        </template>
      </ElTableColumn>
    </ElTable>
  </div>
</template>

<style scoped>
.suggestion-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 12px 0;
}

.suggestion-list {
  margin: 0;
  padding-left: 18px;
}
</style>
