<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElAlert, ElButton, ElDialog, ElInput, ElInputNumber, ElMessage } from 'element-plus'
import { get, post, toApiError } from '@/api/http'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import type { LearningRecord } from '@/features/learning/types'
interface Preview { taskId: number; title: string; version: number; workspaceRevision: number; revision: string; history: {record: LearningRecord; problemNumber: number | null}[] }
const props=defineProps<{ taskId: number | null }>()
const emit=defineEmits<{ close: []; saved: [] }>()
const { online }=useOnlineStatus()
const preview=ref<Preview | null>(null)
const title=ref(''), error=ref(''), loading=ref(false), saving=ref(false)
let generation=0
async function load() {
  const id=props.taskId, ticket=++generation
  preview.value=null; error.value=''
  if(id==null || !online.value)return
  loading.value=true
  try { const value=await get<Preview>(`/plan/tasks/${id}/problem-number-mode`); if(ticket!==generation)return;preview.value=value;title.value='LeetCode每日一题：自行填写题号' }
  catch(caught) {if(ticket===generation)error.value=toApiError(caught).message}
  finally {if(ticket===generation)loading.value=false}
}
async function save() {
  const value=preview.value
  if(!value || saving.value || loading.value || !online.value || !title.value.trim())return
  if(value.history.some(h=>!Number.isInteger(h.problemNumber) || h.problemNumber!<1)) {error.value='请核对每条历史记录的题号（正整数）';return}
  saving.value=true;error.value=''
  try {
    await post(`/plan/tasks/${value.taskId}/problem-number-mode`,{title:title.value.trim(),version:value.version,workspaceRevision:value.workspaceRevision,revision:value.revision,corrections:value.history.map(h=>({id:h.record.id,version:h.record.version,problemNumber:h.problemNumber}))})
    ElMessage.success('已改为自行填写题号');emit('saved');emit('close')
  } catch(caught) {error.value=toApiError(caught).message} finally {saving.value=false}
}
watch(()=>props.taskId,load,{immediate:true})
</script>
<template>
  <ElDialog :model-value="taskId!=null" title="改为自行填写题号" width="min(560px,92vw)" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @close="emit('close')">
    <p v-if="loading">正在核对计划和历史记录…</p>
    <template v-if="preview">
      <label>任务名称<ElInput v-model="title" maxlength="200" :disabled="saving" /></label>
      <p>每天自行填写完成题号，取消固定清单和顺序限制。一天可填多题，也可分次追加，漏做不积压；原计划日期与总目标保留作参考。</p>
      <p>核对以下历史题号。保存时保留原记录、日期、完成量和复盘，只更正填写有变化的题号。</p>
      <div v-for="h in preview.history" :key="h.record.id" class="history-row">
        <label>{{ h.record.recordDate }} · {{ h.record.amount ? '完成' : '尝试' }} · {{ h.record.sequenceTitle }}
          <ElInputNumber :model-value="h.problemNumber ?? undefined" @update:model-value="h.problemNumber=$event ?? null" :min="1" :max="2147483647" :precision="0" :controls="false" :disabled="saving" :aria-label="`记录${h.record.id}题号`" />
        </label>
      </div>
    </template>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <template #footer><ElButton :disabled="saving || !online" @click="load">重新预览</ElButton><ElButton type="primary" :disabled="!online || !preview || !title.trim() || loading" :loading="saving" @click="save">确认修改</ElButton></template>
  </ElDialog>
</template>
<style scoped>.history-row {margin:12px 0}.history-row label {display:flex;gap:12px;justify-content:space-between;align-items:center}</style>
