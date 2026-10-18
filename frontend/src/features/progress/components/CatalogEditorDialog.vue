<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElAlert, ElButton, ElCheckbox, ElDialog, ElInput, ElInputNumber, ElOption, ElSelect } from 'element-plus'
import { toApiError } from '@/api/http'
import { useCatalogStore } from '@/stores/catalog'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { getTaskCandidates } from '../api/catalog'
import { courseDocument, directions, type Course, type CourseInput, type Direction, type NodeInput, type TaskCandidate } from '../catalog'
const props = defineProps<{ modelValue: boolean; direction: Direction; course?: Course | null; branch?: string }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; review: [source: string] }>()
const catalog = useCatalogStore(); const { online } = useOnlineStatus()
function newNode(): NodeInput { return { key: `item-${crypto.randomUUID()}`, parentKey: null, title: '', totalAmount: 1, sortOrder: 0, taskIds: [] } }
function fresh(): CourseInput { return { key: `course-${crypto.randomUUID()}`, direction: props.direction, branch: props.branch || '', title: '', subjectId: 0, unitId: 0, nodes: [newNode()] } }
const draft = ref<CourseInput>(fresh()); const candidates = ref<TaskCandidate[]>([]); const chosen = ref<number[]>([]); const error = ref(''); const busy = ref(false)
const persisted = ref(new Set<string>())
let request = 0
const available = computed(() => candidates.value.filter(t => t.subjectId === draft.value.subjectId && t.unitId === draft.value.unitId && t.nodeId == null && !draft.value.nodes.some(n => n.taskIds.includes(t.id))))
watch(() => props.modelValue, async open => {
  const id = ++request; if (!open) return
  draft.value = props.course ? courseDocument(props.course).courses[0] : fresh()
  persisted.value = new Set(props.course?.nodes.map(n => n.key) ?? [])
  error.value = ''; chosen.value = []; candidates.value = []; busy.value = true
  try {
    await catalog.load()
    if (id !== request) return
    if (!props.course) { draft.value.subjectId = catalog.subjects[0]?.id ?? 0; draft.value.unitId = catalog.units[0]?.id ?? 0 }
    const value = await getTaskCandidates(); if (id === request) candidates.value = value
  } catch (caught) { if (id === request) error.value = toApiError(caught).message }
  finally { if (id === request) busy.value = false }
})
function appendTasks() {
  const initial = draft.value.nodes[0]
  if (draft.value.nodes.length === 1 && initial && !initial.title && initial.totalAmount === 1 && !initial.parentKey && !initial.taskIds.length && !persisted.value.has(initial.key)) draft.value.nodes = []
  for (const id of chosen.value) {
    const task = available.value.find(t => t.id === id)
    if (task) draft.value.nodes.push({ key: `task-${task.id}`, parentKey: null, title: task.title, totalAmount: task.plannedAmount, sortOrder: draft.value.nodes.length, taskIds: [task.id] })
  }
  chosen.value = []
}
function container(node: NodeInput, value: boolean) { node.totalAmount = value ? null : 1; if (value) node.taskIds = [] }
function review() {
  if (!online.value || busy.value) return
  if (!draft.value.title.trim() || !draft.value.branch.trim() || !draft.value.subjectId || !draft.value.unitId || draft.value.nodes.some(n => !n.title.trim())) { error.value = '请填写课程、分科、科目、单位及所有目录项名称。'; return }
  error.value = ''
  emit('review', JSON.stringify({ schemaVersion: 1, courses: [draft.value] }, null, 2))
  emit('update:modelValue', false)
}
</script>
<template>
  <ElDialog :model-value="modelValue" :title="course ? '维护课程目录' : '整理课程或资料'" width="min(900px, calc(100vw - 24px))" :close-on-click-modal="false" @update:model-value="(value: boolean) => emit('update:modelValue', value)">
    <p class="editor-help">整理完整学习内容，已有任务可带入作为候选。任务计划量只是初始值，请核对资料的真实总量。下一步预览后才会保存。</p>
    <ElAlert v-if="error || catalog.error" :title="error || catalog.error?.message || ''" type="error" :closable="false" />
    <div class="course-fields">
      <label>课程／资料名称<ElInput v-model="draft.title" maxlength="200" :disabled="busy || !online" placeholder="例如：高数基础课程" /></label>
      <label>所属方向<ElSelect v-model="draft.direction" :disabled="!!course || busy || !online"><ElOption v-for="d in directions" :key="d.key" :label="d.title" :value="d.key" /></ElSelect></label>
      <label>分科<ElInput v-model="draft.branch" maxlength="100" :disabled="busy || !online" placeholder="例如：高等数学" /></label>
      <label>对应科目<ElSelect v-model="draft.subjectId" :disabled="!!course || busy || !online" @change="chosen = []"><ElOption v-for="s in catalog.subjects" :key="s.id" :label="s.name" :value="s.id" /></ElSelect></label>
      <label>计量单位<ElSelect v-model="draft.unitId" :disabled="!!course || busy || !online" @change="chosen = []"><ElOption v-for="u in catalog.units" :key="u.id" :label="u.name" :value="u.id" /></ElSelect></label>
    </div>
    <div class="task-picker"><ElSelect v-model="chosen" multiple filterable :disabled="busy || !online" placeholder="从已有计划选择候选任务"><ElOption v-for="t in available" :key="t.id" :label="t.title" :value="t.id" /></ElSelect><ElButton :disabled="!chosen.length || busy || !online" @click="appendTasks">带入目录</ElButton></div>
    <p class="editor-help">章节容器只分组；数量与任务关联放在末级条目。已保存条目通过稳定标识维护，省略它不会删除历史目录。</p>
    <div v-for="(node, index) in draft.nodes" :key="node.key" class="node-editor">
      <div class="node-title"><span>{{ index + 1 }}</span><ElInput v-model="node.title" maxlength="200" :disabled="busy || !online" placeholder="内容名称，例如第 1 讲：函数" /><ElButton text type="danger" :disabled="persisted.has(node.key) || busy || !online" @click="draft.nodes.splice(index, 1)">移除草稿</ElButton></div>
      <div class="node-fields"><label>父章节<ElSelect :model-value="node.parentKey ?? undefined" clearable :disabled="busy || !online" placeholder="顶层" @update:model-value="(v: unknown) => node.parentKey = typeof v === 'string' ? v : null"><ElOption v-for="p in draft.nodes.filter(p => p.key !== node.key && p.totalAmount == null)" :key="p.key" :label="p.title || '未命名章节'" :value="p.key" /></ElSelect></label><label>完整总量<ElInputNumber :model-value="node.totalAmount ?? undefined" :disabled="node.totalAmount == null || busy || !online" :min="0.000001" :controls="false" @update:model-value="v => node.totalAmount = v ?? null" /></label><label>顺序<ElInputNumber v-model="node.sortOrder" :min="0" :max="100000" :disabled="busy || !online" /></label><ElCheckbox :model-value="node.totalAmount == null" :disabled="busy || !online" @change="v => container(node, !!v)">章节容器</ElCheckbox></div>
      <p v-if="node.taskIds.length" class="editor-help">关联任务：{{ node.taskIds.map(id => candidates.find(t => t.id === id)?.title || '#' + id).join('、') }}</p>
    </div>
    <div class="editor-actions"><ElButton :disabled="busy || !online" @click="draft.nodes.push(newNode())">＋ 增加目录项</ElButton><ElButton type="primary" :disabled="busy || !online || !draft.nodes.length" @click="review">下一步：核对变化</ElButton></div>
  </ElDialog>
</template>
<style scoped>
.editor-help { font-size:12px; color:var(--kaoyan-text-secondary); line-height:1.7; }.course-fields { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; margin:18px 0; }.course-fields label,.node-fields label { display:flex; flex-direction:column; gap:6px; font-size:12px; color:var(--kaoyan-primary); }.task-picker { display:flex; gap:10px; padding:16px; background:var(--kaoyan-surface); border-radius:10px; }.task-picker :deep(.el-select) { flex:1; min-width:0; }.node-editor { padding:16px; border:1px solid var(--kaoyan-border); border-radius:10px; margin:12px 0; }.node-title { display:flex; gap:10px; align-items:center; }.node-title span { color:var(--kaoyan-text-secondary); font-size:12px; }.node-fields { margin-top:14px; display:grid; grid-template-columns:2fr 1fr 1fr; align-items:end; gap:12px; }.node-fields :deep(.el-input-number) { width:100%; }.editor-actions { display:flex; justify-content:space-between; gap:10px; margin-top:18px; }
@media(max-width:640px) { .course-fields { grid-template-columns:1fr; }.task-picker { flex-wrap:wrap; }.task-picker :deep(.el-select) { flex-basis:100%; }.node-title { flex-wrap:wrap; }.node-title :deep(.el-input) { flex:1; }.node-fields { grid-template-columns:repeat(2,minmax(0,1fr)); }.node-fields label:first-child { grid-column:1/-1; }.node-fields :deep(.el-checkbox) { grid-column:1/-1; }.editor-actions { flex-wrap:wrap; }.editor-actions :deep(.el-button) { margin:0; } }
</style>
