<script setup lang="ts">
import { ElAlert, ElButton } from 'element-plus'

import { useAppUpdate } from './registerServiceWorker'

const { needRefresh, offlineReady, applyUpdate, dismiss } = useAppUpdate()

const handleUpdate = async (): Promise<void> => {
  await applyUpdate()
}
</script>

<template>
  <div class="app-update-prompt">
    <ElAlert
      v-if="needRefresh"
      type="info"
      show-icon
      :closable="false"
      title="有新版本可用"
      description="更新后会刷新页面；未提交的表单内容请先自行保存。"
    >
      <template #default>
        <div class="app-update-prompt__actions">
          <ElButton type="primary" size="small" @click="handleUpdate">立即更新</ElButton>
          <ElButton size="small" text @click="dismiss">稍后</ElButton>
        </div>
      </template>
    </ElAlert>
    <ElAlert
      v-else-if="offlineReady"
      type="success"
      show-icon
      :closable="true"
      title="应用外壳已缓存"
      description="断网后仍可打开页面查看已同步的今日、本周与进度概览。"
      @close="dismiss"
    />
  </div>
</template>

<style scoped>
.app-update-prompt {
  position: fixed;
  right: 16px;
  bottom: 16px;
  z-index: 3000;
  width: min(420px, calc(100vw - 32px));
}

.app-update-prompt__actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
