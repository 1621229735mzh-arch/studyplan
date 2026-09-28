<script setup lang="ts">
/**
 * 候选目标页：院校、专业、参考链接、个人备注与状态变更。
 *
 * 对应后端 TargetController / dto：
 * - 字段是 `schoolName` / `majorName`，链接是 `links: [{url, label}]`（随目标整体提交、整体替换）；
 * - 状态取值是 CANDIDATE / CHOSEN / DROPPED；
 * - 状态变更用 `PATCH /api/targets/{id}/status`，必须带 `{status, version}`；
 * - 新建/修改都必须带 version（新建传 0）。
 *
 * 第一版只管理候选信息，最终选择由本人决定。
 */
import { onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'

import { toApiError } from '@/api/http'
import EmptyState from '@/components/EmptyState.vue'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { changeTargetStatus, createTarget, deleteTarget, listTargets, updateTarget } from '../api'
import {
  TARGET_STATUS_LABELS,
  TARGET_STATUS_OPTIONS,
  targetStatusLabel,
  type Target,
  type TargetLinkRequest,
  type TargetStatus
} from '../types'

const { online } = useOnlineStatus()

const targets = ref<Target[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

const dialogVisible = ref(false)
const editing = ref<Target | null>(null)

const form = reactive<{
  schoolName: string
  majorName: string
  note: string
  status: TargetStatus
  sortOrder: number | null
  links: Array<{ url: string; label: string }>
}>({
  schoolName: '',
  majorName: '',
  note: '',
  status: 'CANDIDATE',
  sortOrder: null,
  links: []
})

const fieldErrors = ref<{ schoolName?: string; links?: string }>({})

async function load(): Promise<void> {
  if (!online.value) {
    errorMessage.value = '离线状态无法读取候选目标。'
    return
  }
  loading.value = true
  errorMessage.value = null
  try {
    targets.value = await listTargets()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function statusTagType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  if (status === 'CHOSEN') {
    return 'success'
  }
  if (status === 'DROPPED') {
    return 'danger'
  }
  return 'info'
}

function openCreate(): void {
  editing.value = null
  form.schoolName = ''
  form.majorName = ''
  form.note = ''
  form.status = 'CANDIDATE'
  form.sortOrder = null
  form.links = []
  fieldErrors.value = {}
  dialogVisible.value = true
}

function openEdit(target: Target): void {
  editing.value = target
  form.schoolName = target.schoolName
  form.majorName = target.majorName ?? ''
  form.note = target.note ?? ''
  form.status = (TARGET_STATUS_OPTIONS.find((option) => option === target.status) ?? 'CANDIDATE')
  form.sortOrder = target.sortOrder ?? null
  form.links = (target.links ?? []).map((link) => ({ url: link.url, label: link.label ?? '' }))
  fieldErrors.value = {}
  dialogVisible.value = true
}

function onFormStatusChange(value: unknown): void {
  form.status = TARGET_STATUS_OPTIONS.find((option) => option === value) ?? 'CANDIDATE'
}

function onSortOrderChange(value: unknown): void {
  form.sortOrder = typeof value === 'number' ? value : null
}

function addLink(): void {
  form.links.push({ url: '', label: '' })
}

function removeLink(index: number): void {
  form.links.splice(index, 1)
}

/** 提交的链接列表：跳过完全空白的行；有名称没地址的行由本地校验拦下。 */
function buildLinks(): TargetLinkRequest[] {
  return form.links
    .filter((link) => link.url.trim() !== '')
    .map((link) => ({
      url: link.url.trim(),
      label: link.label.trim() === '' ? null : link.label.trim()
    }))
}

async function handleSubmit(): Promise<void> {
  const errors: { schoolName?: string; links?: string } = {}
  if (form.schoolName.trim() === '') {
    errors.schoolName = '请填写院校名称'
  }
  if (form.links.some((link) => link.url.trim() === '' && link.label.trim() !== '')) {
    errors.links = '有链接只填了名称没有地址，请补全地址或删除该行'
  }
  fieldErrors.value = errors
  if (Object.keys(errors).length > 0) {
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法保存候选目标。')
    return
  }
  submitting.value = true
  errorMessage.value = null
  try {
    const payload = {
      schoolName: form.schoolName.trim(),
      majorName: form.majorName.trim() === '' ? null : form.majorName.trim(),
      note: form.note.trim() === '' ? null : form.note,
      status: form.status,
      sortOrder: form.sortOrder,
      links: buildLinks()
    }
    if (editing.value === null) {
      await createTarget({ ...payload, version: 0 })
      ElMessage.success('候选目标已添加')
    } else {
      await updateTarget(editing.value.id, { ...payload, version: editing.value.version })
      ElMessage.success('候选目标已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (caught) {
    const apiError = toApiError(caught)
    errorMessage.value = apiError.status === 409 ? '该目标已在其它设备被修改，请刷新后重试。' : apiError.message
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(target: Target, value: unknown): Promise<void> {
  const status = TARGET_STATUS_OPTIONS.find((option) => option === value)
  if (status === undefined || status === target.status) {
    return
  }
  if (!online.value) {
    ElMessage.warning('离线状态无法修改状态。')
    return
  }
  try {
    // 状态变更同样需要版本号
    await changeTargetStatus(target.id, status, target.version)
    ElMessage.success('状态已更新')
    await load()
  } catch (caught) {
    const apiError = toApiError(caught)
    errorMessage.value = apiError.status === 409 ? '该目标已在其它设备被修改，请刷新后重试。' : apiError.message
  }
}

async function handleDelete(target: Target): Promise<void> {
  if (!online.value) {
    ElMessage.warning('离线状态无法删除。')
    return
  }
  try {
    await ElMessageBox.confirm('删除后该目标的参考链接会一并删除。确定删除吗？', '删除候选目标', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteTarget(target.id)
    ElMessage.success('已删除')
    await load()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="page">
    <div class="page__header">
      <div>
        <h2 class="page__title">候选目标</h2>
        <p class="page__subtitle">记录候选院校与专业、参考链接和个人备注；最终选择由本人决定。</p>
      </div>
      <div class="page__actions">
        <ElButton type="primary" :disabled="!online" @click="openCreate">新增候选</ElButton>
        <ElButton :loading="loading" @click="load">刷新</ElButton>
      </div>
    </div>

    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <section class="section-card">
      <EmptyState
        v-if="!loading && targets.length === 0"
        description="还没有候选目标"
        hint="先把感兴趣的院校专业记下来，之后可以逐步比较、筛选。"
      />
      <ElTable v-else :data="targets" size="small" stripe>
        <ElTableColumn label="院校" prop="schoolName" min-width="140" />
        <ElTableColumn label="专业" min-width="140">
          <template #default="{ row }">{{ row.majorName || '—' }}</template>
        </ElTableColumn>
        <ElTableColumn label="参考链接" min-width="200">
          <template #default="{ row }">
            <div v-if="(row.links ?? []).length > 0" class="target-links">
              <a
                v-for="link in row.links"
                :key="link.id"
                :href="link.url"
                target="_blank"
                rel="noopener noreferrer"
              >
                {{ link.label || link.url }}
              </a>
            </div>
            <span v-else class="hint-text">—</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="备注" min-width="180">
          <template #default="{ row }">
            <span v-if="row.note">{{ row.note }}</span>
            <span v-else class="hint-text">—</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="180">
          <template #default="{ row }">
            <ElSelect
              :model-value="row.status"
              size="small"
              @update:model-value="(value) => handleStatusChange(row, value)"
            >
              <ElOption
                v-for="status in TARGET_STATUS_OPTIONS"
                :key="status"
                :label="TARGET_STATUS_LABELS[status]"
                :value="status"
              />
            </ElSelect>
            <ElTag class="target-status" :type="statusTagType(row.status)" size="small" effect="plain">
              {{ targetStatusLabel(row.status) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <ElButton size="small" text type="primary" @click="openEdit(row)">编辑</ElButton>
            <ElButton size="small" text type="danger" @click="handleDelete(row)">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
    </section>

    <ElDialog v-model="dialogVisible" :title="editing ? '编辑候选目标' : '新增候选目标'" width="min(640px, 92vw)">
      <ElAlert
        type="info"
        show-icon
        :closable="false"
        title="链接随目标整体提交"
        description="保存时提交的列表就是该目标的全部链接，后端按提交顺序替换。"
      />
      <ElForm label-width="90px" @submit.prevent>
        <ElFormItem label="院校" :error="fieldErrors.schoolName">
          <ElInput v-model="form.schoolName" maxlength="100" placeholder="例如：某某大学" />
        </ElFormItem>
        <ElFormItem label="专业">
          <ElInput v-model="form.majorName" maxlength="100" placeholder="例如：计算机科学与技术（可留空）" />
        </ElFormItem>
        <ElFormItem label="参考链接" :error="fieldErrors.links">
          <div class="target-link-editor">
            <div v-for="(link, index) in form.links" :key="index" class="target-link-editor__row">
              <ElInput v-model="link.url" maxlength="500" placeholder="链接地址（必填）" />
              <ElInput v-model="link.label" maxlength="100" placeholder="链接名称（可留空）" />
              <ElButton size="small" text type="danger" @click="removeLink(index)">删除</ElButton>
            </div>
            <ElButton size="small" text type="primary" @click="addLink">添加链接</ElButton>
          </div>
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="form.note" type="textarea" :rows="3" placeholder="个人备注（可留空）" />
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect :model-value="form.status" class="target-select" @update:model-value="onFormStatusChange">
            <ElOption
              v-for="status in TARGET_STATUS_OPTIONS"
              :key="status"
              :label="TARGET_STATUS_LABELS[status]"
              :value="status"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="排序">
          <ElInputNumber
            :model-value="form.sortOrder ?? undefined"
            :min="0"
            :step="1"
            controls-position="right"
            placeholder="未设置"
            @update:model-value="onSortOrderChange"
          />
          <span class="hint-text">数字越小越靠前（可留空）</span>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" :disabled="!online" @click="handleSubmit">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.target-select {
  width: 200px;
}

.target-status {
  margin-left: 8px;
}

.target-links {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.target-link-editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.target-link-editor__row {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
