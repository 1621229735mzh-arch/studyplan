<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ElAlert, ElButton, ElDialog, ElMessage, ElOption, ElSelect } from 'element-plus'
import { toApiError } from '@/api/http'
import { amountText, formatDateTime } from '@/composables/useFormat'
import { useCatalogStore } from '@/stores/catalog'
import type { LearningRecord } from '@/features/learning/types'
import { classifyRecord, getTaskCandidates, linkNodeTasks } from '../api/catalog'
import { directions, type CatalogLibrary, type CatalogNode, type Course, type Direction, type TaskCandidate } from '../catalog'
import { useCatalogWorkspace } from '../useCatalogWorkspace'
import CourseRing from '../components/CourseRing.vue'
import CatalogNodeTree from '../components/CatalogNodeTree.vue'
import CatalogImportDialog from '../components/CatalogImportDialog.vue'
import CatalogEditorDialog from '../components/CatalogEditorDialog.vue'

const route = useRoute(); const catalog = useCatalogStore()
const { library, loading, error, notice, readonly, syncedAt, online, reload, accept } = useCatalogWorkspace()
const direction = computed(() => directions.find(d => d.key === String(route.params.direction)))
const courses = computed(() => library.value?.courses.filter(c => c.direction === direction.value?.key) ?? [])
const selectedCourse = computed(() => courses.value.find(c => String(c.id) === route.query.course) ?? null)
const branch = computed(() => selectedCourse.value?.branch || String(route.query.branch || ''))
const branches = computed(() => [...new Set(courses.value.map(c => c.branch))])
const visibleCourses = computed(() => courses.value.filter(c => !branch.value || c.branch === branch.value))
const cumulative = computed(() => library.value?.cumulative.filter(q => q.direction === direction.value?.key) ?? [])
const otherCumulative = computed(() => library.value?.cumulative.filter(q => q.direction === 'UNASSIGNED') ?? [])
const pending = computed(() => library.value?.unclassified.filter(r => {
  const group = library.value?.cumulative.find(q => q.subjectId === r.subjectId)
  return group?.direction === direction.value?.key || group?.direction === 'UNASSIGNED'
}) ?? [])
const editorVisible = ref(false); const importVisible = ref(false); const editorCourse = ref<Course | null>(null); const importSource = ref('')
const busy = ref(false); const actionError = ref(''); const choices = ref<Record<number, number | undefined>>({})
const linking = ref<CatalogNode | null>(null); const linkedTasks = ref<number[]>([]); const candidates = ref<TaskCandidate[]>([]); const linkLoading = ref(false)
const candidateOptions = computed(() => candidates.value.filter(t => t.subjectId === selectedCourse.value?.subjectId && t.unitId === selectedCourse.value?.unitId && (t.nodeId == null || t.nodeId === linking.value?.id)))
function courseOptions(record: LearningRecord) {
  return courses.value.filter(c => c.subjectId === record.subjectId && c.unitId === record.unitId).flatMap(c => c.nodes.filter(n => n.totalAmount != null).map(n => ({ id: n.id, title: c.title + ' · ' + n.title })))
}
function openEditor(course: Course | null = null) { editorCourse.value = course; editorVisible.value = true }
function reviewSource(source = '') { importSource.value = source; importVisible.value = true }
async function applied(value: CatalogLibrary) { await accept(value); ElMessage.success('目录已保存，已有学习成果已同步') }
async function assign(record: LearningRecord) {
  const node = choices.value[record.id]
  if (!node || !library.value || busy.value || readonly.value || !online.value) return
  busy.value = true; actionError.value = ''
  try { const value = await classifyRecord(record.id, node, record.version, library.value.revision); await accept(value); delete choices.value[record.id]; ElMessage.success('已归类，学科累计不重复增加') }
  catch (caught) { actionError.value = toApiError(caught).message }
  finally { busy.value = false }
}
async function openLink(node: CatalogNode) {
  if (readonly.value || !online.value) return
  linking.value = node; linkedTasks.value = [...node.taskIds]; actionError.value = ''; candidates.value = []; linkLoading.value = true
  try { candidates.value = await getTaskCandidates() }
  catch (caught) { actionError.value = toApiError(caught).message }
  finally { linkLoading.value = false }
}
async function saveLink() {
  if (!linking.value || !library.value || readonly.value || !online.value || busy.value || linkLoading.value) return
  busy.value = true; actionError.value = ''
  try { const value = await linkNodeTasks(linking.value.id, linkedTasks.value, library.value.revision); await accept(value); linking.value = null; ElMessage.success('任务归属已更新，相关学习成果已同步') }
  catch (caught) { actionError.value = toApiError(caught).message }
  finally { busy.value = false }
}
onMounted(() => { if (online.value) void catalog.load() })
</script>
<template>
  <div class="study-workspace direction-workspace" :aria-busy="loading">
    <nav class="breadcrumbs" aria-label="进度路径"><RouterLink to="/progress">学习进度</RouterLink><template v-if="direction"><span>／</span><RouterLink :to="`/progress/${direction.key}`">{{ direction.title }}</RouterLink></template><template v-if="branch && direction"><span>／</span><RouterLink :to="{ path: `/progress/${direction.key}`, query: { branch } }">{{ branch }}</RouterLink></template><template v-if="selectedCourse"><span>／</span><span>{{ selectedCourse.title }}</span></template></nav>
    <header class="workspace-header"><div><span class="workspace-eyebrow">已学的成果，尚未走完的路</span><h1>{{ selectedCourse?.title || branch || direction?.title || '方向不存在' }}</h1><p>查看内容与累计完成量，完整目录始终保留。</p></div><div v-if="direction" class="workspace-actions"><ElButton :disabled="readonly || !online || loading || !library || busy" @click="openEditor(selectedCourse)">{{ selectedCourse ? '维护目录' : '整理课程／资料' }}</ElButton><ElButton type="primary" :disabled="readonly || !online || loading || !library || busy" @click="reviewSource()">导入完整目录</ElButton></div></header>
    <ElAlert v-if="error" :title="error" type="error" :closable="false"><ElButton text :disabled="loading || busy" @click="reload">重新加载</ElButton></ElAlert>
    <ElAlert v-if="notice" :title="notice" type="warning" :closable="false" />
    <p v-if="readonly && syncedAt" class="workspace-note">概览同步时间：{{ formatDateTime(syncedAt) }}</p>
    <ElAlert v-if="actionError && !linking" :title="actionError" type="error" :closable="false"><ElButton text :disabled="loading || busy" @click="reload">刷新核对</ElButton></ElAlert>
    <div v-if="!direction" class="workspace-empty"><h3>请选择已有学习方向</h3><RouterLink to="/progress">返回四个方向入口</RouterLink></div>
    <div v-else-if="loading" class="workspace-loading" role="status">正在读取学习进度…</div>
    <template v-else-if="library">
      <section v-if="!selectedCourse" class="cumulative-panel"><span>学科累计</span><div v-if="cumulative.length" class="cumulative-amounts"><strong v-for="q in cumulative" :key="q.subjectId + ':' + q.unitId">{{ q.subjectName }} <b>{{ amountText(q.amount,q.unitName) }}</b></strong></div><p v-else class="workspace-note">还没有该方向的学习量记录。</p></section>
      <section v-if="selectedCourse" class="workspace-panel course-detail">
        <div class="course-overview"><CourseRing :completed="selectedCourse.completedAmount" :total="selectedCourse.totalAmount" :unit="selectedCourse.unitName" /><div><h2>{{ selectedCourse.title }}</h2><p class="course-quantity">已完成 {{ selectedCourse.completedAmount }} / {{ selectedCourse.totalAmount }} {{ selectedCourse.unitName }}</p><p v-if="selectedCourse.recordedAmount !== selectedCourse.completedAmount" class="workspace-note">实际累计 {{ amountText(selectedCourse.recordedAmount, selectedCourse.unitName) }}；每项完成度分别以目录总量为限。</p><p class="workspace-note">同一条学习记录只计一次，修改与删除后自动更新。</p></div></div>
        <h3 class="catalog-title">完整学习目录</h3><CatalogNodeTree v-if="!readonly" :nodes="selectedCourse.nodes" :unit="selectedCourse.unitName" :readonly="readonly || busy" @link="openLink" /><p v-else class="workspace-note">联网后查看完整目录与任务归属。</p>
      </section>
      <template v-else-if="!branch && branches.length"><div class="section-heading"><div><h2>按分科查看</h2><p>进入分科，再选择课程或资料。</p></div></div><nav class="branch-grid" aria-label="分科"><RouterLink v-for="b in branches" :key="b" :to="{ path: `/progress/${direction.key}`, query: { branch: b } }" class="branch-card"><h3>{{ b }}</h3><span>查看课程与资料 ↗</span></RouterLink></nav></template>
      <template v-else-if="visibleCourses.length"><div class="section-heading"><h2>课程与资料</h2></div><div class="course-grid"><RouterLink v-for="course in visibleCourses" :key="course.id" :to="{ path: `/progress/${direction.key}`, query: { branch: course.branch, course: course.id } }" class="course-card"><CourseRing :completed="course.completedAmount" :total="course.totalAmount" :unit="course.unitName" /><div><span class="workspace-note">{{ course.branch }}</span><h3>{{ course.title }}</h3><p>{{ course.completedAmount }} / {{ course.totalAmount }} {{ course.unitName }}</p><span class="course-enter">查看目录 ↗</span></div></RouterLink></div></template>
      <div v-else class="workspace-empty"><h3>还没有课程或资料目录</h3><p>可以从已有计划整理候选，或导入包含未排期内容的完整目录。</p><ElButton type="primary" :disabled="readonly || !online || busy" @click="openEditor()">整理第一份目录</ElButton></div>
      <details v-if="pending.length && !readonly" class="workspace-panel pending-records"><summary>待归类学习成果 · {{ pending.length }} 条</summary><p class="workspace-note">已计入学科累计。选择明确归属后计入课程，不会增加第二份数量。</p><article v-for="record in pending" :key="record.id" class="pending-row"><div><span class="workspace-note">{{ record.subjectName }}</span><h4>{{ record.content || record.taskTitle || '学习记录' }}</h4><span>{{ amountText(record.amount,record.unitName) }}</span></div><div class="pending-target"><ElSelect v-model="choices[record.id]" filterable placeholder="选择课程与具体内容" :disabled="busy || !online"><ElOption v-for="o in courseOptions(record)" :key="o.id" :label="o.title" :value="o.id" /></ElSelect><ElButton :disabled="!choices[record.id] || busy || !online" @click="assign(record)">确认归类</ElButton><p v-if="!courseOptions(record).length" class="workspace-note">先建立同科目、同单位的目录。</p></div></article></details>
      <details v-if="otherCumulative.length && !selectedCourse" class="workspace-panel"><summary>尚未归入四个方向的科目累计</summary><p v-for="q in otherCumulative" :key="q.subjectId + ':' + q.unitId" class="workspace-note">{{ q.subjectName }} · {{ amountText(q.amount, q.unitName) }}</p><p class="workspace-note">整理课程时确认所属方向后，这些成果会进入对应学科。</p></details>
    </template>
    <footer class="workspace-footer"><RouterLink to="/learning">查看或修正学习记录 ↗</RouterLink><ElButton text :disabled="loading || busy || !online" @click="reload">刷新进度</ElButton></footer>
    <CatalogEditorDialog v-if="direction" v-model="editorVisible" :direction="direction.key as Direction" :course="editorCourse" :branch="branch" @review="reviewSource" />
    <CatalogImportDialog v-model="importVisible" :initial-source="importSource" @applied="applied" />
    <ElDialog :model-value="!!linking" title="关联任务" width="min(620px, calc(100vw - 24px))" :close-on-click-modal="false" :show-close="!busy" :close-on-press-escape="!busy" @update:model-value="(v: boolean) => !v && !busy && (linking = null)"><p class="workspace-note">{{ linking?.title }}：这些任务的历史及后续学习记录将同步到这一目录项。</p><ElAlert v-if="actionError" :title="actionError" type="error" :closable="false" /><ElSelect v-model="linkedTasks" multiple filterable :loading="linkLoading" :disabled="busy || !online || readonly" placeholder="选择同科目、同单位的任务" class="link-select"><ElOption v-for="t in candidateOptions" :key="t.id" :label="t.title" :value="t.id" /></ElSelect><ElButton type="primary" :disabled="busy || linkLoading || !online || readonly" @click="saveLink">确认任务归属</ElButton></ElDialog>
  </div>
</template>
<style scoped src="@/styles/retrospective.css"></style>
<style scoped>
.breadcrumbs { display:flex; flex-wrap:wrap; gap:8px; font-size:12px; color:#839387; margin:0 0 18px; }.breadcrumbs a { text-decoration:none; }.workspace-header h1 { overflow-wrap:anywhere; }.cumulative-panel { border-top:1px solid #e4eae4; border-bottom:1px solid #e4eae4; padding:20px 0; margin-bottom:24px; }.cumulative-panel > span { font-size:12px; color:#869489; }.cumulative-amounts { display:flex; flex-wrap:wrap; gap:16px 30px; margin-top:12px; }.cumulative-amounts strong { font-size:13px; font-weight:400; color:#728475; }.cumulative-amounts b { font-size:18px; color:#3f6b52; margin-left:10px; }.branch-grid,.course-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:18px; margin-bottom:24px; }.branch-card,.course-card { border:1px solid #e0e7de; border-radius:14px; background:#fff; padding:24px; text-decoration:none; }.branch-card:hover,.course-card:hover { border-color:#78a08a; }.branch-card h3 { margin:0 0 14px; color:#31583e; font-size:20px; }.branch-card span { font-size:12px; color:#7c9382; }.course-card { display:flex; align-items:center; gap:20px; }.course-card > div { min-width:0; }.course-card h3 { margin:6px 0 12px; font-size:17px; font-weight:500; color:#36533e; overflow-wrap:anywhere; }.course-card p { font-size:14px; color:#5a7c60; }.course-enter { font-size:12px; color:#8b9c8e; }.course-overview { display:flex; align-items:center; gap:26px; margin-bottom:30px; }.course-overview h2 { margin:0 0 10px; font-size:22px; overflow-wrap:anywhere; }.course-quantity { font-size:16px; color:#517759; }.catalog-title { font-size:17px; font-weight:550; margin:24px 0 12px; }.pending-records { margin-top:26px; }.pending-records > summary { cursor:pointer; color:#577561; font-size:14px; }.pending-row { display:flex; justify-content:space-between; gap:20px; padding:18px 0; border-top:1px solid #e9eee7; }.pending-row h4 { font-size:14px; font-weight:500; margin:6px 0 10px; overflow-wrap:anywhere; }.pending-row > div:first-child { min-width:0; }.pending-row > div:first-child > span:last-child { font-size:13px; color:#728472; }.pending-target { width:350px; display:flex; flex-wrap:wrap; gap:8px; flex-shrink:0; }.pending-target :deep(.el-select) { flex:1; min-width:160px; }.pending-target p { margin:0; width:100%; }.link-select { width:100%; margin:18px 0; }
@media(max-width:760px) { .branch-grid,.course-grid { grid-template-columns:1fr; }.course-card { padding:20px; }.course-overview { gap:16px; flex-wrap:wrap; }.course-overview h2 { font-size:19px; }.pending-row { flex-wrap:wrap; }.pending-target { width:100%; }.cumulative-amounts b { font-size:16px; } }
</style>
