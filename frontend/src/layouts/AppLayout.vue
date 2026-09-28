<script setup lang="ts">
/**
 * 应用主布局：侧边导航 + 顶栏（当前用户、离线状态与最后同步时间、退出）。
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { ElAside, ElButton, ElContainer, ElHeader, ElMain, ElMenu, ElMenuItem, ElMessage, ElTag } from 'element-plus'

import OfflineNotice from '@/components/OfflineNotice.vue'
import { formatDateTime } from '@/composables/useFormat'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'
import { useCatalogStore } from '@/stores/catalog'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const catalog = useCatalogStore()
const offline = useOfflineSnapshotStore()
const { online } = useOnlineStatus()

/** 侧边菜单项。index 同时作为路由路径（ElMenu 开启 router 模式）。 */
const menuItems: Array<{ path: string; label: string; abbr: string }> = [
  { path: '/', label: '今日', abbr: '今' },
  { path: '/plan', label: '计划', abbr: '计' },
  { path: '/learning', label: '记录', abbr: '记' },
  { path: '/progress', label: '进度', abbr: '进' },
  { path: '/review', label: '复习', abbr: '复' },
  { path: '/memo', label: '备忘录', abbr: '备' },
  { path: '/targets', label: '目标', abbr: '标' },
  { path: '/settings', label: '设置', abbr: '设' }
]

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => (typeof route.meta.title === 'string' ? route.meta.title : '今日'))
const lastSyncText = computed(() =>
  offline.syncedAt ? formatDateTime(offline.syncedAt) : '尚无同步记录'
)

/** 窄屏时折叠侧栏，把空间留给记录表单。 */
const narrow = ref(false)
const loggingOut = ref(false)

function updateNarrow(): void {
  narrow.value = typeof window !== 'undefined' && window.innerWidth < 900
}

onMounted(() => {
  updateNarrow()
  window.addEventListener('resize', updateNarrow)
  // 科目与单位被多个页面共用，进应用时预加载一次（离线时静默失败）
  void catalog.load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateNarrow)
})

async function handleLogout(): Promise<void> {
  loggingOut.value = true
  try {
    await session.logout()
    ElMessage.success('已退出登录，本地离线缓存已清理')
    await router.push({ name: 'login' })
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <ElContainer class="app-layout">
    <ElAside :width="narrow ? '64px' : '200px'" class="app-layout__aside">
      <div class="app-layout__brand">
        <img src="@/assets/logo.svg" alt="考研学习工作台" class="app-layout__logo" />
        <span v-if="!narrow" class="app-layout__brand-text">考研工作台</span>
      </div>
      <ElMenu :default-active="activeMenu" :collapse="narrow" router class="app-layout__menu">
        <ElMenuItem v-for="item in menuItems" :key="item.path" :index="item.path">
          <span class="app-layout__menu-abbr">{{ item.abbr }}</span>
          <template #title>{{ item.label }}</template>
        </ElMenuItem>
      </ElMenu>
    </ElAside>

    <ElContainer class="app-layout__body">
      <ElHeader class="app-layout__header">
        <div class="app-layout__title">{{ pageTitle }}</div>
        <div class="app-layout__status">
          <ElTag :type="online ? 'success' : 'warning'" size="small" effect="plain">
            {{ online ? '在线' : '离线' }}
          </ElTag>
          <span class="app-layout__sync">最后同步：{{ lastSyncText }}</span>
          <span class="app-layout__user">{{ session.displayName }}</span>
          <ElButton size="small" :loading="loggingOut" @click="handleLogout">退出</ElButton>
        </div>
      </ElHeader>

      <ElMain class="app-layout__main">
        <OfflineNotice />
        <RouterView />
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.app-layout__aside {
  background: #1f2d3d;
  color: #fff;
  overflow-x: hidden;
}

.app-layout__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 56px;
  padding: 0 12px;
  color: #fff;
  font-weight: 600;
}

.app-layout__logo {
  width: 26px;
  height: 26px;
  flex: 0 0 auto;
}

.app-layout__brand-text {
  white-space: nowrap;
}

.app-layout__menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #d5dbe3;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: #2b3d52;
}

.app-layout__menu-abbr {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-right: 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
}

.app-layout__body {
  background: var(--kaoyan-bg);
}

.app-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--kaoyan-border);
}

.app-layout__title {
  font-size: 16px;
  font-weight: 600;
}

.app-layout__status {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--kaoyan-text-secondary);
  font-size: 13px;
  flex-wrap: wrap;
}

.app-layout__sync {
  white-space: nowrap;
}

.app-layout__user {
  color: var(--kaoyan-text);
}

.app-layout__main {
  padding: 16px;
}

@media (max-width: 700px) {
  .app-layout__sync {
    display: none;
  }
}
</style>
