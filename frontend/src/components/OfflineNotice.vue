<script setup lang="ts">
/**
 * 离线提示条。
 *
 * 只展示状态与最后同步时间，并明确说明第一版离线为只读，
 * 避免用户以为离线也能提交记录。
 */
import { computed } from 'vue'
import { ElAlert } from 'element-plus'

import { formatDateTime } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'

const { online } = useOnlineStatus()
const offline = useOfflineSnapshotStore()

const lastSyncText = computed(() =>
  offline.syncedAt ? formatDateTime(offline.syncedAt) : '尚无同步记录'
)

const title = computed(() => `离线，最后同步时间 ${lastSyncText.value}`)
</script>

<template>
  <ElAlert
    v-if="!online"
    class="offline-notice"
    type="warning"
    show-icon
    :closable="false"
    :title="title"
    description="第一版离线为只读：可查看已同步的今日、本周与进度概览，无法提交学习记录或修改计划。"
  />
</template>

<style scoped>
.offline-notice {
  margin-bottom: 12px;
}
</style>
