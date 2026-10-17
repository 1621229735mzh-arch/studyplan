<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElAlert, ElButton, ElDialog, ElInput, ElMessage } from 'element-plus'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { toApiError } from '@/api/http'
import { previewQuantityMode, confirmQuantityMode, type QuantityModePreview } from '../api/quantity'
const props=defineProps<{ taskId: number | null }>()
const emit=defineEmits<{ close: []; saved: [] }>()
const { online }=useOnlineStatus()
const preview=ref<QuantityModePreview | null>(null)
const title=ref(''), error=ref(''), loading=ref(false), saving=ref(false)
let generation=0
async function load() {
  const id=props.taskId, ticket=++generation
  preview.value=null;error.value=''
  if (id==null || !online.value) return
  loading.value=true
  try { const value=await previewQuantityMode(id);if(ticket!==generation)return;preview.value=value;title.value=value.title }
  catch(caught) {if(ticket===generation)error.value=toApiError(caught).message}
  finally {if(ticket===generation)loading.value=false}
}
async function save() {
  if(!online.value || !preview.value || saving.value || !title.value.trim())return
  saving.value=true;error.value=''
  try { await confirmQuantityMode(preview.value,title.value.trim());ElMessage.success('已改为填写实际数量');emit('saved');emit('close') }
  catch(caught) { error.value=toApiError(caught).message }
  finally {saving.value=false}
}
watch(()=>props.taskId,load,{immediate:true})
</script>
<template>
  <ElDialog :model-value="taskId!=null" title="改为填写实际数量" width="min(480px,92vw)" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" @close="emit('close')">
    <p v-if="loading">正在读取当前任务和日安排…</p>
    <template v-if="preview">
      <label for="quantity-task-title">任务名称</label><ElInput id="quantity-task-title" v-model="title" maxlength="200" :disabled="saving" />
      <p>将 {{ preview.scheduleCount }} 条日安排改为学习后填写实际数量，取消固定每日目标与总目标量。日期保持原样。</p>
      <p>保留 {{ preview.preservedRecordCount }} 条历史记录。{{ preview.mode==='COUNT_REVIEW' ? '复习词数单独统计；过去只勾选完成的记录保留为数量未知。' : '实际新学数量继续计入学习进度。' }}</p>
    </template>
    <ElAlert v-if="error" :title="error" type="error" :closable="false" />
    <template #footer><ElButton :disabled="saving || !online" @click="load">重新预览</ElButton><ElButton type="primary" :disabled="!online || !preview || !title.trim() || loading" :loading="saving" @click="save">确认修改</ElButton></template>
  </ElDialog>
</template>
