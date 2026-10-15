<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElAlert, ElButton, ElCheckbox, ElDialog, ElInput } from 'element-plus'
import { toApiError } from '@/api/http'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import { previewCatalog, confirmCatalog, getTaskCandidates } from '../api/catalog'
import type { CatalogLibrary, CatalogPreview } from '../catalog'
import guide from '../../../../../docs/business/progress-catalog-guide.md?raw'
import schema from '../../../../../docs/business/progress-catalog.schema.json?raw'
import example from '../../../../../docs/business/progress-catalog-example.json?raw'
const props = defineProps<{ modelValue: boolean; initialSource?: string }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; applied: [CatalogLibrary] }>()
const { online } = useOnlineStatus()
const catalog = useCatalogStore()
const source = ref(''); const preview = ref<CatalogPreview | null>(null); const error = ref(''); const busy = ref(false); const checked = ref(false)
let sequence = 0
watch(source, () => { ++sequence; preview.value = null; checked.value = false; error.value = '' })
watch(() => props.modelValue, open => { if (open) { source.value = props.initialSource ?? ''; preview.value = null; checked.value = false; error.value = ''; void catalog.load() } else ++sequence })
function download(name: string, text: string) {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
  const anchor = document.createElement('a'); anchor.href = url; anchor.download = name; anchor.click(); URL.revokeObjectURL(url)
}
async function exportCatalog() {
  error.value = ''; busy.value = true
  try { await catalog.load(true); if (catalog.error) throw new Error(catalog.error.message); const tasks = await getTaskCandidates(); download('当前科目单位与任务.json', JSON.stringify({ subjects: catalog.subjects, units: catalog.units, tasks }, null, 2)) }
  catch (caught) { error.value = toApiError(caught).message } finally { busy.value = false }
}
async function selectFile(event: Event) {
  const input = event.target as HTMLInputElement; const file = input.files?.[0]; input.value = ''
  if (!file) return
  if (file.size > 2_000_000) { error.value = '文件不能超过 2 MB'; return }
  const id = ++sequence
  try { const text = await file.text(); if (id === sequence) source.value = text }
  catch { if (id === sequence) error.value = '文件读取失败，请重新选择。' }
}
async function inspect() {
  if (busy.value || !online.value || !source.value.trim()) return
  if (new TextEncoder().encode(source.value).length > 2_000_000) { error.value = '文件不能超过 2 MB'; return }
  const id = ++sequence; busy.value = true; error.value = ''; checked.value = false; preview.value = null
  try { const value = await previewCatalog(source.value); if (id === sequence) preview.value = value }
  catch (caught) { if (id === sequence) error.value = toApiError(caught).message }
  finally { busy.value = false }
}
async function apply() {
  if (busy.value || !online.value || !checked.value || !preview.value) return
  busy.value = true; error.value = ''
  try { const value = await confirmCatalog(preview.value.previewToken); emit('applied', value); emit('update:modelValue', false) }
  catch (caught) { error.value = toApiError(caught).message; if (toApiError(caught).status === 409) { preview.value = null; checked.value = false } }
  finally { busy.value = false }
}
</script>
<template>
  <ElDialog :model-value="modelValue" title="导入与确认目录" width="min(900px, calc(100vw - 24px))" :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" @update:model-value="(value: boolean) => !busy && emit('update:modelValue', value)">
    <p class="help">目录只定义课程与内容，不生成安排或学习记录。先核对完整总量及任务关联，再确认保存。</p>
    <div class="downloads"><ElButton text @click="download('目录导入说明.md', guide)">下载说明</ElButton><ElButton text @click="download('目录格式.schema.json', schema)">下载格式</ElButton><ElButton text @click="download('目录示例.json', example)">下载示例</ElButton><ElButton text :disabled="busy || !online" @click="exportCatalog">下载当前科目与任务</ElButton></div>
    <label class="file-label">选择目录 JSON<input type="file" accept=".json,application/json" :disabled="busy || !online" @change="selectFile" /></label>
    <ElInput v-model="source" type="textarea" :rows="6" :disabled="busy || !online" placeholder="也可粘贴目录 JSON" aria-label="目录 JSON" />
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <ElButton :disabled="busy || !online || !source.trim()" :loading="busy" @click="inspect">校验并预览</ElButton>
    <section v-if="preview" class="catalog-preview">
      <h3>确认这次变化</h3><p class="help">预览有效至 {{ preview.expiresAt.replace('T',' ') }}。未提交的已有条目会保留。</p>
      <div v-for="change in preview.changes" :key="change.title" class="change-row"><strong>{{ change.title }}</strong><span>{{ change.previousTotal }} → {{ change.nextTotal }} {{ change.unitName }} · 新增 {{ change.addedNodes }} 项</span></div>
      <details v-for="course in preview.document.courses" :key="course.key" class="preview-course" open><summary>{{ course.branch }} · {{ course.title }}</summary><div v-for="node in course.nodes" :key="node.key" class="preview-node"><strong>{{ node.title }}</strong><span>{{ node.totalAmount == null ? '章节' : node.totalAmount + ' ' + (catalog.unitNameOf(course.unitId) || '单位') }}<template v-if="node.parentKey"> · 父目录：{{ node.parentKey }}</template><template v-if="node.taskIds.length"> · 关联任务：{{ node.taskIds.join('、') }}</template></span></div></details>
      <ElCheckbox v-model="checked" :disabled="busy || !online">我已核对目录、总量变化及任务归属</ElCheckbox><ElButton type="primary" :disabled="!checked || busy || !online" :loading="busy" @click="apply">确认保存目录</ElButton>
    </section>
  </ElDialog>
</template>
<style scoped>
.help { font-size:13px; color:#738076; line-height:1.7; }.downloads { display:flex; flex-wrap:wrap; gap:4px; margin-bottom:12px; }.downloads :deep(.el-button) { margin:0; }.file-label { display:flex; flex-direction:column; gap:8px; font-size:13px; margin:16px 0; }.file-label input { max-width:100%; }.catalog-preview { border-top:1px solid #dfe7e1; margin-top:20px; padding-top:10px; }.catalog-preview h3 { font-size:17px; }.change-row,.preview-node { display:flex; justify-content:space-between; flex-wrap:wrap; gap:8px; padding:12px; border-bottom:1px solid #e7ece8; font-size:13px; }.change-row span,.preview-node span { color:#76857b; }.preview-course { margin:14px 0; background:#f6f9f5; padding:12px; border-radius:8px; }.preview-course summary { cursor:pointer; font-size:14px; }.preview-node strong { font-weight:500; overflow-wrap:anywhere; }.catalog-preview :deep(.el-checkbox) { display:flex; height:auto; white-space:normal; margin:18px 0; }.catalog-preview :deep(.el-checkbox__label) { white-space:normal; }.el-alert { margin:14px 0; }.el-textarea { margin-bottom:14px; }
</style>
