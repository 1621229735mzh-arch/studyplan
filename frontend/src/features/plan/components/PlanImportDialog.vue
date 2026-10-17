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
import continuationExample from '../../../../../docs/business/plan-import-continuation-example.json?raw'

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
    subject: tasks.get(item.taskKey)?.subject ?? '', quantity: tasks.get(item.taskKey)?.activity?.mode === 'COUNT_REVIEW' ? '填写实际复习量 · 不增加新词量' : tasks.get(item.taskKey)?.activity?.mode === 'COUNT_STUDY' ? '填写实际新学量 · 无固定目标' : tasks.get(item.taskKey)?.continuation ? '学习接续 · 按内容记录进度' : tasks.get(item.taskKey)?.activity?.mode === 'CHECK_ONLY' ? '只确认完成 · 不计数量或用时' : item.amount == null ? (tasks.get(item.taskKey)?.activity?.mode === 'RECAP' ? '复盘 · 不增加首次完成量' : '按内容执行 · 完成量据实') : `${item.amount} ${tasks.get(item.taskKey)?.unit ?? ''}`, minutes: item.estimatedMinutes, checkOnly: tasks.get(item.taskKey)?.activity?.mode === 'CHECK_ONLY' }))
    : [{ date: day.date, title: '休息 / 不安排任务', subject: '', quantity: '—', minutes: null, checkOnly: false }])
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
      <ElButton text @click="downloadText('张宇高数学习接续-首周样例.json', continuationExample, 'application/json')">张宇学习接续样例</ElButton>
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
      <p v-if="preview.document.tasks.some(t => t.activity?.mode === 'CHECK_ONLY')">完成确认型只保存当天是否完成；无需数量或时长，不增加新学量，未完成不叠加到次日。</p>
      <p v-if="preview.document.tasks.some(t => t.activity && ['TIMED_STUDY','RECAP'].includes(t.activity.mode))">活动安排不预填题量，结束当天安排不等于整讲完成。无需计时；复盘不增加首次完成量。</p>
      <details v-if="preview.document.goals.length || preview.document.months.length || preview.document.weeks.length"><summary>查看整体目标、月目标与周目标</summary><p v-for="goal in preview.document.goals" :key="goal.key">{{ goal.subject }} · {{ goal.title }}：{{ goal.description }}</p><p v-for="month in preview.document.months" :key="month.month">{{ month.month }} · {{ month.title }}：{{ month.description }}</p><p v-for="week in preview.document.weeks" :key="week.startDate">{{ week.startDate }} 当周 · {{ week.title }}：{{ week.description }}</p></details>
      <template v-if="preview.document.tasks.some(t => t.continuation)">
        <p>按书独立接续。完全未做继续当前内容；部分完成后，次日保留剩余并带入下一份。只收尾旧内容不追加后续；参考结束日期可顺延。</p>
        <details open><summary>查看学习接续内容</summary><p v-for="task in preview.document.tasks.filter(t => t.continuation)" :key="task.key">{{ task.continuation!.title }} · {{ task.title }}：{{ task.activity?.description }}</p></details>
      </template>
      <details v-for="task in preview.document.tasks.filter(t => t.sequence)" :key="task.key" open>
        <summary>{{ task.title }} · {{ task.sequence!.mode === 'SELECT_BY_NUMBER' ? '每天自行填写题号' : `有序接续清单（${task.sequence!.items.length} 题）` }}</summary>
        <p v-if="task.sequence!.mode !== 'SELECT_BY_NUMBER'">每天最多完成 1 题。未完成继续当前题，漏做名额不积压；尝试无需计时且不推进题目，结束日期不自动延长。</p>
        <p v-if="task.sequence!.mode === 'SELECT_BY_NUMBER'">不预设题号或顺序；每天最多完成 1 题，漏做不积压。</p>
        <ol><li v-for="item in task.sequence!.items" :key="item.key"><a :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.title }}</a></li></ol>
      </details>
      <div v-if="preview.warnings.length" class="import-warnings"><strong>请核对疑似重复项（最多显示 100 条）</strong><p v-for="warning in preview.warnings" :key="warning">{{ warning }}</p></div>
      <div class="import-table"><table><thead><tr><th>日期</th><th>科目 / 学习内容</th><th>计划量</th><th>预计用时</th></tr></thead><tbody><tr v-for="(row, index) in rows.slice((page - 1) * 30, page * 30)" :key="index"><td>{{ row.date }}</td><td><small>{{ row.subject }}</small>{{ row.title }}</td><td>{{ row.quantity }}</td><td>{{ row.checkOnly ? '无需用时' : row.minutes == null ? '—' : `${row.minutes} 分钟` }}</td></tr></tbody></table></div>
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
