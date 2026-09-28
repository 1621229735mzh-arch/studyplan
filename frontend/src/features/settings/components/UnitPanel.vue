<script setup lang="ts">
/**
 * 计量单位维护（题、讲、章、页……）。
 *
 * 单位决定完成量的口径：不同单位不混加，因此单位配置要尽量稳定；
 * 被任务或记录引用时后端会拒绝删除。
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
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'

import { toApiError } from '@/api/http'
import EmptyState from '@/components/EmptyState.vue'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import type { StudyUnit } from '@/types/domain'
import { createUnit, deleteUnit, updateUnit } from '../api'

const catalog = useCatalogStore()
const { online } = useOnlineStatus()

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

const dialogVisible = ref(false)
const editing = ref<StudyUnit | null>(null)

const form = reactive<{ name: string; sortOrder: number | null; enabled: boolean }>({
  name: '',
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
  form.sortOrder = null
  form.enabled = true
  fieldErrors.value = {}
  dialogVisible.value = true
}

function openEdit(unit: StudyUnit): void {
  editing.value = unit
  form.name = unit.name
  form.sortOrder = unit.sortOrder
  form.enabled = unit.enabled
  fieldErrors.value = {}
  dialogVisible.value = true
}

function onSortOrderChange(value: unknown): void {
  form.sortOrder = typeof value === 'number' ? value : null
}

function onEnabledChange(value: unknown): void {
  form.enabled = value === true
}

async function handleSubmit(): Promise<void> {
  if (form.name.trim() === '') {
    fieldErrors.value = { name: '请填写单位名称' }
    return
  }
  fieldErrors.value = {}
  if (!online.value) {
    ElMessage.warning('离线状态无法保存，第一版离线为只读。')
    return
  }
  submitting.value = true
  try {
    const payload = { name: form.name.trim(), sortOrder: form.sortOrder ?? 0, enabled: form.enabled }
    if (editing.value === null) {
      await createUnit(payload)
      ElMessage.success('单位已新增')
    } else {
      await updateUnit(editing.value.id, payload)
      ElMessage.success('单位已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (caught) {
    errorMessage.value = toApiError(caught).message
  } finally {
    submitting.value = false
  }
}

async function handleDelete(unit: StudyUnit): Promise<void> {
  if (!online.value) {
    ElMessage.warning('离线状态无法删除。')
    return
  }
  try {
    await ElMessageBox.confirm('如果已有任务或记录引用该单位，后端会拒绝删除。确定删除吗？', '删除单位', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteUnit(unit.id)
    ElMessage.success('单位已删除')
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
      <ElButton type="primary" :disabled="!online" @click="openCreate">新增单位</ElButton>
      <ElButton :loading="loading" @click="load">刷新</ElButton>
      <span class="hint-text">例如：题、讲、章、页、套。不同单位不会相加。</span>
    </div>

    <ElAlert v-if="errorMessage" type="error" show-icon :closable="false" :title="errorMessage" />

    <EmptyState v-if="!loading && catalog.units.length === 0" description="还没有计量单位" hint="先建立常用的单位，创建任务时需要选择其中一个。" />

    <ElTable v-else :data="catalog.units" size="small" stripe>
      <ElTableColumn label="名称" prop="name" min-width="160" />
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

    <ElDialog v-model="dialogVisible" :title="editing ? '编辑单位' : '新增单位'" width="min(480px, 92vw)">
      <ElForm label-width="90px" @submit.prevent>
        <ElFormItem label="名称" :error="fieldErrors.name">
          <ElInput v-model="form.name" maxlength="32" placeholder="例如：讲" />
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
