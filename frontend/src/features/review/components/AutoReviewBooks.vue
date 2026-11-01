<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { ElAlert, ElButton, ElMessageBox } from 'element-plus'
import { toApiError } from '@/api/http'
import { todayIso } from '@/composables/useFormat'
import { getAutoReviewBooks, getAutoReviewSegments, setAutoReviewPolicy, type AutoReviewBook, type AutoReviewSegment } from '../api/automatic'

const props = defineProps<{ online: boolean; subjectId?: number; workspace: unknown }>()
const emit = defineEmits<{ changed: []; schedule: [date: string, itemId: number] }>()
const books = ref<AutoReviewBook[]>([]); const segments = ref<AutoReviewSegment[]>([])
const loading = ref(false); const writing = ref(false); const error = ref('')
let generation = 0
async function load() {
  const id = ++generation
  books.value = []; segments.value = []; error.value = ''; loading.value = false
  if (!props.online) return
  loading.value = true
  try {
    const [bookList, contentList] = await Promise.all([getAutoReviewBooks(), getAutoReviewSegments()])
    if (id !== generation) return
    books.value = bookList; segments.value = contentList
  } catch (caught) { if (id === generation) error.value = toApiError(caught).message }
  finally { if (id === generation) loading.value = false }
}
function state(segment: AutoReviewSegment) {
  if (segment.status === 'ARCHIVED') return '已归档'
  if (!segment.sourceValid) return '来源未学完 · 已暂停'
  if (!segment.enabled || segment.paused) return '已暂停'
  if (segment.scheduledDate) return `${segment.scheduledDate < todayIso() ? '已逾期 · ' : ''}${segment.scheduledDate} · 第${segment.repetitions + 1}次复习`
  return '待安排'
}
async function toggle(book: AutoReviewBook) {
  if (writing.value || !props.online) return
  try {
    await ElMessageBox.confirm(book.enabled
      ? `暂停“${book.title}”的自动复习？待执行日期将移除，学习和复习历史保留。`
      : `启用“${book.title}”的自动复习？已学完整段将纳入，首轮按实际学完日的次日安排，过去到期的内容保留逾期日期；后续根据实际复习反馈自动安排。已有手动复习项保持原样。`,
    book.enabled ? '暂停自动复习' : '启用自动复习', { confirmButtonText: '确认', cancelButtonText: '取消' })
  } catch { return }
  if (writing.value || !props.online) return
  writing.value = true; error.value = ''
  try { await setAutoReviewPolicy(book, !book.enabled); emit('changed'); await load() }
  catch (caught) { error.value = toApiError(caught).message }
  finally { writing.value = false }
}
watch(() => [props.online, props.subjectId, props.workspace], () => void load(), { immediate: true })
onBeforeUnmount(() => { generation++ })
</script>

<template>
  <section class="automatic-books" aria-label="按教材自动复习" :aria-busy="loading">
    <div class="automatic-heading"><h2>按教材自动复习</h2><small>学完小段后，自动安排下一次巩固</small></div>
    <p class="automatic-hint">首轮在学完次日检查；之后按回忆和解题表现调整间隔。先独立回忆、做题，再核对答案。不会或需要提示请选择“不会”；能独立完成但吃力请选择“模糊”；顺利完成请选择“掌握”。</p>
    <ElAlert v-if="error" :title="error" type="error" :closable="false"><ElButton text :disabled="!online || loading || writing" @click="load">重新加载自动复习</ElButton></ElAlert>
    <p v-if="!online" class="automatic-hint">联网后查看和调整自动复习。</p>
    <p v-else-if="!loading && !error && !books.length" class="automatic-hint">还没有可接入的按书学习内容。</p>
    <details v-for="book in books.filter(b => !subjectId || b.subjectId === subjectId)" :key="book.streamId" class="automatic-book">
      <summary><strong>{{ book.title }}</strong><span>{{ book.enabled ? '自动复习中' : '未启用' }} · {{ book.reviewCount }} / {{ book.segmentCount }} 段已纳入<span v-if="book.overdueCount"> · {{ book.overdueCount }} 段逾期</span></span></summary>
      <div class="automatic-controls"><ElButton :disabled="writing || loading || !online" @click="toggle(book)">{{ book.enabled ? '暂停自动复习' : '启用自动复习' }}</ElButton><small>暂停与改期保留历史；未学完的内容不提前建复习。</small></div>
      <p v-if="!segments.some(s => s.streamId === book.streamId)" class="automatic-hint">{{ book.enabled ? '等待内容段完整学完；已有手动复习项请在内容库查看。' : '启用后，按实际学完的小段逐步建立复习。' }}</p>
      <ul class="automatic-segments">
        <li v-for="segment in segments.filter(s => s.streamId === book.streamId)" :key="segment.reviewItemId">
          <div><strong>{{ segment.title }}</strong><small>学完于 {{ segment.completedOn }} · 已完整复习 {{ segment.repetitions }} 次</small><span>{{ state(segment) }}</span></div>
          <ElButton v-if="book.enabled && segment.sourceValid && segment.status !== 'ARCHIVED'" text :disabled="writing || !online" @click="emit('schedule', todayIso(), segment.reviewItemId)">{{ segment.scheduledDate ? '调整日期' : '恢复安排' }}</ElButton>
        </li>
      </ul>
    </details>
  </section>
</template>

<style scoped>
.automatic-books { margin:20px 0; padding:18px; border:1px solid var(--kaoyan-border); border-radius:12px; }
.automatic-heading,.automatic-controls { display:flex; align-items:center; flex-wrap:wrap; gap:12px; }
.automatic-heading h2 { margin:0; font-size:17px; }.automatic-hint,small { color:var(--kaoyan-text-secondary); font-size:12px; line-height:1.8; }
.automatic-book { margin-top:12px; border-top:1px solid var(--kaoyan-border); padding-top:12px; }
summary { cursor:pointer; min-height:44px; }summary span { display:block; font-size:12px; margin-top:5px; }
.automatic-controls { margin:12px 0; }.automatic-segments { padding:0; list-style:none; margin:0; }
.automatic-segments li { display:flex; justify-content:space-between; gap:12px; padding:12px 0; border-top:1px solid var(--kaoyan-border); }
.automatic-segments strong { font-size:13px; overflow-wrap:anywhere; }.automatic-segments small,.automatic-segments span { display:block; font-size:12px; margin-top:5px; }
@media(max-width:600px) { .automatic-books { padding:12px; }.automatic-segments li { flex-direction:column; } }
</style>
