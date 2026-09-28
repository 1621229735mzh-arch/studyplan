<script setup lang="ts">
/**
 * 科目维护。
 *
 * 科目被任务、学习记录、进度、复习、备忘录共用，因此变更后要刷新全局 catalog。
 */
import { onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElColorPicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'

import { toApiError } from '@/api/http'
import EmptyState from '@/components/EmptyState.vue'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import type { Subject } from '@/types/domain'
import { createSubject, deleteSubject, updateSubject } from '../api'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

const dialogVisible = ref(false)
const editing = ref<Subject | null>(null)

const form = reactive<{
  name: string
  category: string
  color: string
  sortOrder: number | null
  enabled: boolean
}>({
  name: '',
  category: '',
  color: '#2f6feb',
  sortOrder: null,
  enabled: true
})

const fieldErrors = ref<{ name?: string }>({})

async function load(): Promise<void> {
  loading.value = true
  try {
    await catalog.reload()
    errorMessage.value = null
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  editing.value = null
  form.name = ''
  form.category = ''
  form.color = '#2f6feb'
  form.sortOrder = null
  form.enabled = true
  fieldErrors.value = {}
  dialogVisible.value = true
}

function openEdit(subject: Subject): void {
  editing.value = subject
  form.name = subject.name
  form.category = subject.category ?? ''
  form.color = subject.color ?? '#2f6feb'
  form.sortOrder = subject.sortOrder
  form.enabled = subject.enabled
  fieldErrors.value = {}
  dialogVisible.value = true
}

function onColorChange(value: unknown): void {
  form.color = typeof value === 'string' ? value : ''
}

function onSortOrderChange(value: unknown): void {
  form.sortOrder = typeof value === 'number' ? value : null
}

function onEnabledChange(value: unknown): void {
  form.enabled = value === true
}

async function handleSubmit(): Promise<void> {
  if (form.name.trim() === '') {
    fieldErrors.value = { name: '请填写科目名称' }
    return
  }
  fieldErrors.value = {}
  if (!online.value) {
    ElMessage.warning('离线状态无法保存，第一版离线为只读。')
    return
  }
  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      category: form.category.trim() === '' ? null : form.category.trim(),
      color: form.color === '' ? null : form.color,
      sortOrder: form.sortOrder ?? 0,
      enabled: form.enabled
    }
    if (editing.value === null) {
      await createSubject(payload)
      ElMessage.success('科目已新增')
    } else {
      await updateSubject(editing.value.id, payload)
      ElMessage.success('科目已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    submitting.value = false
  }
}

async function handleDelete(subject: Subject): Promise<void> {
  if (!online.value) {
    ElMessage.warning('离线状态无法删除。')
    return
  }
  try {
    await ElMessageBox.confirm(
      '如果已有任务引用该科目，后端会拒绝删除。确定删除吗？',
      '删除科目',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteSubject(subject.id)
    ElMessage.success('科目已删除')
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
  <div>
    <div class="table-toolbar">
      <ElButton type="primary" :disabled="!online" @click="openCreate">新增科目</ElButton>
      <ElButton :loading="loading" @click="load">刷新</ElButton>
      <span class="hint-text">科目用于任务、记录、复习与备忘的分类；被引用时无法删除。</span>
    </div>

    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <EmptyState v-if="!loading && catalog.subjects.length === 0" description="还没有科目" hint="先建立 408 的四门科目（数据结构、计算机组成原理、操作系统、计算机网络）。" />

    <ElTable v-else :data="catalog.subjects" size="small" stripe>
      <ElTableColumn label="名称" min-width="160">
        <template #default="{ row }">
          <span class="subject-color" :style="{ background: row.color ?? '#d0d7de' }" />
          {{ row.name }}
        </template>
      </ElTableColumn>
      <ElTableColumn label="分类" prop="category" width="140">
        <template #default="{ row }">{{ row.category ?? '—' }}</template>
      </ElTableColumn>
      <ElTableColumn label="排序" prop="sortOrder" width="90" />
      <ElTableColumn label="状态" width="110">
        <template #default="{ row }">
          <ElTag :type="row.enabled ? 'success' : 'info'" size="small" effect="plain">
            {{ row.enabled ? '启用' : '停用' }}
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

    <ElDialog v-model="dialogVisible" :title="editing ? '编辑科目' : '新增科目'" width="min(520px, 92vw)">
      <ElForm label-width="90px" @submit.prevent>
        <ElFormItem label="名称" :error="fieldErrors.name">
          <ElInput v-model="form.name" maxlength="64" placeholder="例如：数据结构" />
        </ElFormItem>
        <ElFormItem label="分类">
          <ElInput v-model="form.category" maxlength="32" placeholder="例如：专业课 / 公共课" />
        </ElFormItem>
        <ElFormItem label="颜色">
          <ElColorPicker :model-value="form.color" @update:model-value="onColorChange" />
        </ElFormItem>
        <ElFormItem label="排序">
          <ElInputNumber
            :model-value="form.sortOrder ?? undefined"
            :min="0"
            :step="1"
            controls-position="right"
            @update:model-value="onSortOrderChange"
          />
        </ElFormItem>
        <ElFormItem label="启用">
          <ElSwitch :model-value="form.enabled" @update:model-value="onEnabledChange" />
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
.subject-color {
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-right: 6px;
  border-radius: 50%;
  vertical-align: middle;
}
</style>
