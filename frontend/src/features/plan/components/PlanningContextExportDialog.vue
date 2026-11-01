<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { ElAlert, ElButton, ElDialog } from 'element-plus'
import { toApiError } from '@/api/http'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { getPlanningContextExport, type PlanningContextExport } from '../api/contextExport'
import { downloadText } from '../workspace'

const open = defineModel<boolean>({ required: true })
const { online } = useOnlineStatus()
const result = ref<PlanningContextExport | null>(null)
const loading = ref(false)
const error = ref('')
let request = 0
async function load() {
  const current = ++request
  result.value = null
  error.value = ''
  if (!online.value) { loading.value = false; error.value = '请联网后导出最新规划上下文。'; return }
  loading.value = true
  try {
    const data = await getPlanningContextExport()
    if (current === request && open.value) result.value = data
  } catch (caught) { if (current === request) error.value = toApiError(caught).message }
  finally { if (current === request) loading.value = false }
}
function download(format: 'md' | 'json') {
  if (!result.value || !online.value) return
  const { snapshot, markdown } = result.value
  const stamp = snapshot.exportedAt.replace(/[^0-9]/g, '').slice(0, 14)
  downloadText(`规划上下文-${stamp}-${snapshot.dataVersion.slice(0, 8)}.${format}`,
    format === 'md' ? markdown : JSON.stringify(snapshot, null, 2),
    format === 'md' ? 'text/markdown' : 'application/json')
}
watch(open, (value) => {
  if (value) void load()
  else { ++request; result.value = null; loading.value = false; error.value = '' }
}, { immediate: true })
watch(online, (value) => {
  if (!value) { ++request; result.value = null; loading.value = false; error.value = '请联网后导出最新规划上下文。' }
})
onBeforeUnmount(() => { ++request })
</script>

<template>
  <ElDialog v-model="open" title="导出规划上下文" width="min(680px, calc(100vw - 24px))">
    <p>带着真实学习情况讨论下一阶段计划。包含全部科目的课程与任务 ID、原参考安排、实际进度、学习与复习反馈及未完成备忘录。</p>
    <p class="section-note">导出范围为全部科目，不受计划页当前筛选影响。两种格式使用下方同一份快照。</p>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <p v-if="loading" role="status">正在整理最新学习上下文…</p>
    <div v-if="result" class="context-summary">
      <p>{{ result.snapshot.facts.progress.courses.length }} 门课程 · {{ result.snapshot.facts.tasks.length }} 个任务 · {{ result.snapshot.facts.studyRecords.length }} 条学习记录 · {{ result.snapshot.facts.reviewRecords.length }} 条复习记录</p>
      <p>最近 30 个学习日：{{ result.snapshot.recentFrom }} — {{ result.snapshot.recentTo }}</p>
      <p>期间学习 {{ result.snapshot.feedback.learningDays }} 天，记录 {{ result.snapshot.feedback.studyRecordCount }} 次；完成复习 {{ result.snapshot.feedback.completedReviews }} 次。</p>
      <p>薄弱项 {{ result.snapshot.feedback.weaknesses.length }} 项（依据最新真实反馈）· 未完成备忘录 {{ result.snapshot.facts.openMemos.length }} 条</p>
      <p>导出时点：{{ result.snapshot.exportedAt }}<br>学习日：{{ result.snapshot.studyDate }}（北京时间 09:00 切换）</p>
      <p class="context-version">数据版本：{{ result.snapshot.dataVersion }}</p>
    </div>
    <p class="section-note">Markdown 适合直接阅读和讨论，JSON 保留结构化字段。这份文件用于讨论；调整建议以后需核对确认，不能作为新计划直接导入。</p>
    <template #footer>
      <div class="context-actions">
        <ElButton :disabled="loading || !online" @click="load">重新读取</ElButton>
        <ElButton :disabled="!result || loading || !online" @click="download('md')">下载 Markdown</ElButton>
        <ElButton type="primary" :disabled="!result || loading || !online" @click="download('json')">下载 JSON</ElButton>
      </div>
    </template>
  </ElDialog>
</template>

<style scoped>
.context-summary { margin: 16px 0; padding: 12px 16px; background: var(--kaoyan-surface); border: 1px solid var(--kaoyan-border); border-radius: 8px; }
.context-summary p { margin: 8px 0; }
.context-version { overflow-wrap: anywhere; font-size: 12px; color: var(--kaoyan-text-secondary); }
.context-actions { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 8px; }
.context-actions :deep(.el-button) { margin-left: 0; }
</style>
