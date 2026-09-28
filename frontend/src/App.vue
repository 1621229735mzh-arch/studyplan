<script setup lang="ts">
/**
 * 根组件：只负责渲染路由出口与应用更新提示。
 *
 * 启动时读取一次离线快照；是否允许进入业务页面由路由守卫决定。
 */
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'

import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import AppUpdatePrompt from '@/pwa/AppUpdatePrompt.vue'

const offline = useOfflineSnapshotStore()

onMounted(() => {
  void offline.load()
})
</script>

<template>
  <RouterView />
  <AppUpdatePrompt />
</template>
