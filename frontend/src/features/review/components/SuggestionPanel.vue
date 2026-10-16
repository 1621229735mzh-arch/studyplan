<script setup lang="ts">
/** 到期复习由本人选择并确认，无需填写用时。 */
import { computed, onMounted, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElCheckbox,
  ElDatePicker,
  ElMessage,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'

import { toApiError } from '@/api/http'
import EmptyState from '@/components/EmptyState.vue'
import { formatIsoDate, todayIso } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import { confirmReviewSuggestions, getReviewSuggestions } from '../api'
import type { ReviewConfirmRejected, ReviewItem, ReviewSuggestionResponse } from '../types'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const date = ref(todayIso())
const suggestion = ref<ReviewSuggestionResponse | null>(null)
const selected = ref<number[]>([])

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const noticeMessage = ref<string | null>(null)
const rejected = ref<ReviewConfirmRejected[]>([])
/** 后端确认成功的内容。 */
const scheduled = ref<ReviewItem[]>([])

const items = computed(() => suggestion.value?.items ?? [])
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


async function confirmSelected(): Promise<void> {
  if (submitting.value) return
  const chosen = items.value.filter((item) => selected.value.includes(item.reviewItemId))
  if (chosen.length === 0) {
    ElMessage.warning('请先勾选要确认的复习建议')
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法确认建议，第一版离线为只读。')
    return
  }

  submitting.value = true
  errorMessage.value = null
  noticeMessage.value = null
  try {
    const result = await confirmReviewSuggestions({
      date: date.value,
      manualSelection: true,
      items: chosen.map((item) => ({
        reviewItemId: item.reviewItemId
      }))
    })
    rejected.value = result.rejected ?? []
    scheduled.value = result.scheduled ?? []
    noticeMessage.value = `本次确认 ${result.scheduled.length} 条。`
    ElMessage.success(`已确认 ${result.scheduled.length} 条进入当天安排`)
    selected.value = []
    await load()
    noticeMessage.value = `本次确认 ${result.scheduled.length} 条。`
  } catch (caught) {
    const apiError = toApiError(caught)
    errorMessage.value =
      apiError.status === 409 ? '建议已过期或计划已变化，请刷新后重新确认。' : apiError.message
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

    <ElAlert type="info" :closable="false" title="按实际需要选择复习内容，确认后进入当天安排。" />

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

    <ElAlert v-if="scheduled.length > 0" type="success" show-icon :closable="false" title="已进入当天安排的复习内容">
      <ul class="suggestion-list">
        <li v-for="item in scheduled" :key="item.id">
          {{ item.title }}
          <span class="hint-text">
            （{{ formatIsoDate(item.scheduledDate) }}）
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
