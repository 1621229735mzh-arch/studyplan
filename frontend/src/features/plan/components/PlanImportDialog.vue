<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElAlert, ElButton, ElCheckbox, ElDialog, ElInput, ElMessage, ElPagination } from 'element-plus'
import { toApiError } from '@/api/http'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useCatalogStore } from '@/stores/catalog'
import { confirmImport, previewImport } from '../api/workspace'
import { downloadText, type ImportPreview } from '../workspace'
import guide from '../../../../../docs/business/plan-import-guide.md?raw'
import example from '../../../../../docs/business/plan-import-example.json?raw'
import schema from '../../../../../docs/business/plan-import.schema.json?raw'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; imported: [] }>()
const catalog = useCatalogStore()
const { online } = useOnlineStatus()
const source = ref(''); const error = ref(''); const busy = ref(false)
const preview = ref<ImportPreview | null>(null); const checked = ref(false); const page = ref(1)
const fileInput = ref<HTMLInputElement>()
const rows = computed(() => {
  const doc = preview.value?.document
  if (!doc) return []
  const tasks = new Map(doc.tasks.map(t => [t.key, t]))
  return doc.days.flatMap(day => day.items.length ? day.items.map(item => ({ date: day.date, title: tasks.get(item.taskKey)?.title ?? item.taskKey,
    subject: tasks.get(item.taskKey)?.subject ?? '', quantity: `${item.amount} ${tasks.get(item.taskKey)?.unit ?? ''}`, minutes: item.estimatedMinutes }))
    : [{ date: day.date, title: '休息 / 不安排任务', subject: '', quantity: '—', minutes: null }])
})
watch(source, () => { preview.value = null; checked.value = false; page.value = 1; error.value = '' })
watch(() => props.modelValue, open => { if (open) void catalog.load(true) })
async function fileChanged(event: Event) {
  const input = event.target as HTMLInputElement; const file = input.files?.[0]
  if (!file) return
  input.value = ''
  if (file.size > 2_000_000) { error.value = '文件不能超过 2 MB'; return }
  source.value = await file.text()
}
async function inspect() {
  if (busy.value || !online.value) return
  if (new TextEncoder().encode(source.value).length > 2_000_000) { error.value = '文件不能超过 2 MB'; return }
  busy.value = true; error.value = ''; preview.value = null; checked.value = false
  try { preview.value = await previewImport(source.value); page.value = 1 }
  catch (caught) { error.value = toApiError(caught).message }
  finally { busy.value = false }
}
async function apply() {
  if (!preview.value || !checked.value || busy.value || !online.value) return
  busy.value = true; error.value = ''
  try {
    await confirmImport(source.value, preview.value.previewToken)
    ElMessage.success('方案已追加到计划'); source.value = ''; emit('update:modelValue', false); emit('imported')
  } catch (caught) {
    error.value = toApiError(caught).message; preview.value = null; checked.value = false
  } finally { busy.value = false }
}
function downloadCatalog() {
  if (catalog.error) { error.value = '科目与单位读取失败，请重试后下载'; return }
  downloadText('我的规划目录.json', JSON.stringify({ subjects: catalog.enabledSubjects.map(s => s.name), units: catalog.enabledUnits.map(u => u.name) }, null, 2), 'application/json')
}
</script>

<template>
<ElDialog :model-value="modelValue" title="导入外部方案" width="min(960px, calc(100vw - 24px))" :close-on-click-modal="!busy" :close-on-press-escape="!busy" :show-close="!busy" @update:model-value="emit('update:modelValue', $event)">
    <div class="import-steps"><span :class="{ active: !preview }">01 选择方案</span><i>→</i><span :class="{ active: preview }">02 检查预览</span><i>→</i><span>03 确认追加</span></div>
    <p class="import-hint">将规划书和你的科目、单位目录交给外部 AI，讨论完成后上传它生成的 JSON 文件。</p>
    <div class="import-downloads">
      <ElButton text @click="downloadText('计划导入规划书.md', guide)">下载规划书</ElButton>
      <ElButton text @click="downloadCatalog" :disabled="catalog.loading || !!catalog.error">下载我的目录</ElButton>
      <ElButton text @click="downloadText('计划示例.json', example, 'application/json')">JSON 示例</ElButton>
      <ElButton text @click="downloadText('plan-import.schema.json', schema, 'application/json')">格式规范</ElButton>
    </div>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <template v-if="!preview">
      <div class="import-upload"><input ref="fileInput" type="file" accept=".json,application/json" hidden @change="fileChanged" /><ElButton :disabled="busy" @click="fileInput?.click()">选择 JSON 文件</ElButton><span>或在下方粘贴完整内容</span></div>
      <ElInput v-model="source" type="textarea" :rows="10" placeholder="粘贴外部 AI 输出的 JSON，保留原始内容即可" :disabled="busy" aria-label="导入 JSON 内容" />
    </template>
    <div v-else class="import-preview">
      <h3>{{ preview.document.title }}</h3><p>{{ preview.document.startDate }} — {{ preview.document.endDate }} · {{ preview.taskCount }} 个任务 · {{ preview.dayCount }} 天 · {{ preview.itemCount }} 条安排</p>
      <details v-if="preview.document.goals.length || preview.document.months.length || preview.document.weeks.length"><summary>查看整体目标、月目标与周目标</summary><p v-for="goal in preview.document.goals" :key="goal.key">{{ goal.subject }} · {{ goal.title }}：{{ goal.description }}</p><p v-for="month in preview.document.months" :key="month.month">{{ month.month }} · {{ month.title }}：{{ month.description }}</p><p v-for="week in preview.document.weeks" :key="week.startDate">{{ week.startDate }} 当周 · {{ week.title }}：{{ week.description }}</p></details>
      <div v-if="preview.warnings.length" class="import-warnings"><strong>请核对疑似重复项（最多显示 100 条）</strong><p v-for="warning in preview.warnings" :key="warning">{{ warning }}</p></div>
      <div class="import-table"><table><thead><tr><th>日期</th><th>科目 / 学习内容</th><th>计划量</th><th>预计用时</th></tr></thead><tbody><tr v-for="(row, index) in rows.slice((page - 1) * 30, page * 30)" :key="index"><td>{{ row.date }}</td><td><small>{{ row.subject }}</small>{{ row.title }}</td><td>{{ row.quantity }}</td><td>{{ row.minutes == null ? '未填写' : `${row.minutes} 分钟` }}</td></tr></tbody></table></div>
<ElPagination v-model:current-page="page" :total="rows.length" :page-size="30" layout="prev, pager, next" size="small" />
      <ElCheckbox v-model="checked" :disabled="busy">已核对方案与重复提示，同意追加到现有安排</ElCheckbox>
    </div>
    <template #footer><ElButton v-if="preview" :disabled="busy" @click="preview = null; checked = false">返回修改</ElButton><ElButton v-if="!preview" type="primary" :loading="busy" :disabled="!online || !source.trim()" @click="inspect">校验并预览</ElButton><ElButton v-else type="primary" :loading="busy" :disabled="!online || !checked" @click="apply">确认追加</ElButton></template>
  </ElDialog>
</template>

<style scoped>
.import-steps { display: flex; gap: 14px; align-items: center; color: #85909d; flex-wrap: wrap; }
.import-steps .active { color: #326a61; font-weight: 600; }.import-steps i { font-style: normal; }
.import-hint,.import-upload span { color: #727d89; font-size: 13px; }.import-downloads { display:flex; flex-wrap:wrap; margin: 8px 0 20px; }
.import-downloads :deep(.el-button) { margin-left: 0; }.import-upload { display:flex; gap:12px; align-items:center; margin: 16px 0; }
.import-preview { display:flex; flex-direction:column; gap: 14px; padding-top:18px; }.import-preview h3,.import-preview p { margin:0; }.import-preview details p { margin-top:8px; }
.import-warnings { background: #fff8eb; padding:14px; border-radius:10px; max-height:170px; overflow:auto; }.import-warnings p { margin-top:6px; }
.import-table { overflow:auto; }.import-table table { border-collapse:collapse; width:100%; font-size:13px; }.import-table th { text-align:left; color:#727d89; font-weight:500; background:#f6f8fa; }.import-table th,.import-table td { padding:12px; border-bottom:1px solid #eef0f3; }.import-table td:first-child { white-space:nowrap; }.import-table small { display:block; color:#7d8895; }
.import-preview :deep(.el-checkbox) { white-space:normal; height:auto; }.import-preview :deep(.el-checkbox__label) { white-space:normal; }
</style>
