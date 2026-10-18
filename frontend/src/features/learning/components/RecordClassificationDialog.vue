<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElAlert, ElButton, ElDialog, ElOption, ElSelect } from 'element-plus'
import { toApiError } from '@/api/http'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { classifyRecord, getCatalog } from '@/features/progress/api/catalog'
import type { CatalogLibrary } from '@/features/progress/catalog'
import type { LearningRecord } from '../types'
const props = defineProps<{ record: LearningRecord | null }>()
const emit = defineEmits<{ close: []; saved: [] }>()
const { online } = useOnlineStatus()
const library = ref<CatalogLibrary | null>(null); const nodeId = ref<number>(); const error = ref(''); const loading = ref(false); const busy = ref(false)
let request = 0
const options = computed(() => library.value?.courses.filter(c => c.subjectId === props.record?.subjectId && c.unitId === props.record?.unitId).flatMap(c => c.nodes.filter(n => n.totalAmount != null).map(n => ({ id:n.id, title:c.branch+' · '+c.title+' · '+n.title }))) ?? [])
watch(() => props.record, async record => {
  const id=++request; library.value=null; nodeId.value=undefined; error.value=''
  if(!record || !online.value) return
  loading.value=true
  try {const value=await getCatalog(); if(id===request) library.value=value}
  catch(caught) {if(id===request) error.value=toApiError(caught).message}
  finally {if(id===request) loading.value=false}
})
async function save(reset=false) {
  if(!props.record || !library.value || busy.value || !online.value || (!reset && !nodeId.value)) return
  busy.value=true; error.value=''
  try {await classifyRecord(props.record.id,reset?null:nodeId.value!,props.record.version,library.value.revision); emit('saved'); emit('close')}
  catch(caught) {error.value=toApiError(caught).message}
  finally {busy.value=false}
}
onBeforeUnmount(()=>{++request})
</script>
<template>
  <ElDialog :model-value="!!record" title="确认课程归属" width="min(650px, calc(100vw - 24px))" :close-on-click-modal="false" :show-close="!busy" :close-on-press-escape="!busy" @update:model-value="(v: boolean) => !v && !busy && emit('close')">
    <p>{{ record?.content || record?.taskTitle || '学习记录' }}</p><p class="help">更改归属只移动这条记录对应的课程进度，学科累计不重复增加。</p>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <ElSelect v-model="nodeId" filterable :loading="loading" :disabled="busy || loading || !online" placeholder="选择同科目、同单位的具体目录项" class="classification-select"><ElOption v-for="o in options" :key="o.id" :label="o.title" :value="o.id" /></ElSelect>
    <p v-if="library && !options.length" class="help">还没有匹配的目录，请先在进度页整理同科目、同单位的课程。</p>
    <div class="classification-actions"><ElButton :disabled="busy || loading || !library || !online" @click="save(true)">恢复任务自动归属</ElButton><ElButton type="primary" :disabled="busy || loading || !nodeId || !online" @click="save()">确认归属</ElButton></div>
  </ElDialog>
</template>
<style scoped>.help { font-size:13px; color:var(--kaoyan-text-secondary); line-height:1.7; }.classification-select { width:100%; margin:18px 0; }.classification-actions { display:flex; justify-content:space-between; flex-wrap:wrap; gap:10px; }.classification-actions :deep(.el-button) { margin:0; }</style>
