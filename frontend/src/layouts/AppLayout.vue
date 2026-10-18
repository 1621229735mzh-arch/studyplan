<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElButton, ElDrawer, ElMessage, ElTag } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import ArchiveArtwork from '@/components/ArchiveArtwork.vue'
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
const menuItems = [
  { path: '/', label: '今日', icon: 'today' },
  { path: '/plan', label: '计划', icon: 'plan' },
  { path: '/learning', label: '记录', icon: 'learning' },
  { path: '/progress', label: '进度', icon: 'progress' },
  { path: '/review', label: '复习', icon: 'review' },
  { path: '/memo', label: '备忘录', icon: 'memo' },
  { path: '/targets', label: '目标', icon: 'targets' },
  { path: '/settings', label: '设置', icon: 'settings' }
]
const primaryItems = menuItems.slice(0, 4)
const moreItems = menuItems.slice(4)
const isToday = computed(() => route.name === 'today')
const pageTitle = computed(() => typeof route.meta.title === 'string' ? route.meta.title : '今日')
const moreActive = computed(() => moreItems.some(item => isActive(item.path)))
const lastSyncText = computed(() => offline.syncedAt ? formatDateTime(offline.syncedAt) : '尚无同步记录')
const moreOpen = ref(false)
const loggingOut = ref(false)
function isActive(path: string): boolean {
  return path === '/' ? route.path === '/' : route.path === path || route.path.startsWith(`${path}/`)
}
watch(() => route.fullPath, () => { moreOpen.value = false })
onMounted(() => { void catalog.load() })
async function handleLogout(): Promise<void> {
  loggingOut.value = true
  try {
    await session.logout()
    moreOpen.value = false
    ElMessage.success('已退出登录，本地离线缓存已清理')
    await router.push({ name: 'login' })
  } finally { loggingOut.value = false }
}
</script>

<template>
  <div class="app-layout" :class="{ 'app-layout--today': isToday }">
    <a class="skip-link" href="#study-main">跳到主要内容</a>
    <aside class="app-layout__aside">
      <RouterLink to="/" class="app-layout__brand" aria-label="学习工作台首页">
        <img src="@/assets/logo.svg" alt="" class="app-layout__logo" />
        <div class="app-layout__brand-text"><strong>学习工作台</strong><span>STUDY ROOM</span></div>
      </RouterLink>
      <nav class="app-layout__menu" aria-label="主导航">
        <RouterLink v-for="item in menuItems" :key="item.path" :to="item.path" class="app-layout__nav-link" :class="{ 'is-active': isActive(item.path) }" :aria-current="isActive(item.path) ? 'page' : undefined" :aria-label="item.label"><AppIcon :name="item.icon" /><span>{{ item.label }}</span></RouterLink>
      </nav>
      <div class="app-layout__seal" aria-hidden="true"><span></span></div>
      <div class="app-layout__aside-foot"><span class="connection-dot" :class="{ 'is-offline': !online }"></span>{{ online ? '已连接' : '离线只读' }}</div>
    </aside>
    <div class="app-layout__body">
      <header class="app-layout__header">
        <div class="app-layout__title"><span class="desktop-page-title">{{ pageTitle }}</span><span class="mobile-brand">学习工作台</span></div>
        <div class="app-layout__status">
          <ElTag :type="online ? 'success' : 'warning'" size="small" effect="plain">{{ online ? '在线' : '离线' }}</ElTag>
          <span class="app-layout__sync">最后同步：{{ lastSyncText }}</span>
          <span class="app-layout__user">{{ session.displayName }}</span>
          <ElButton class="desktop-logout" size="small" :loading="loggingOut" @click="handleLogout">退出</ElButton>
        </div>
      </header>
      <main id="study-main" class="app-layout__main"><OfflineNotice /><RouterView /></main>
    </div>
    <aside v-if="isToday" class="app-layout__art" aria-hidden="true"><ArchiveArtwork /></aside>
    <nav class="mobile-navigation" aria-label="手机主导航">
      <RouterLink v-for="item in primaryItems" :key="item.path" :to="item.path" :class="{ 'is-active': isActive(item.path) }" :aria-current="isActive(item.path) ? 'page' : undefined"><AppIcon :name="item.icon" :size="22" /><span>{{ item.label }}</span></RouterLink>
      <button type="button" :class="{ 'is-active': moreActive || moreOpen }" :aria-expanded="moreOpen" :aria-controls="moreOpen ? 'mobile-more-menu' : undefined" @click="moreOpen = !moreOpen"><AppIcon name="more" :size="22" /><span>更多</span></button>
    </nav>
    <ElDrawer v-model="moreOpen" title="更多" direction="btt" size="auto" class="mobile-more-drawer">
      <nav id="mobile-more-menu" class="more-menu" aria-label="更多页面"><RouterLink v-for="item in moreItems" :key="item.path" :to="item.path" :class="{ 'is-active': isActive(item.path) }" :aria-current="isActive(item.path) ? 'page' : undefined"><AppIcon :name="item.icon" :size="22" /><span>{{ item.label }}</span></RouterLink></nav>
      <div class="more-session"><div><strong>{{ session.displayName }}</strong><p>{{ online ? '在线' : '离线只读' }} · 最后同步：{{ lastSyncText }}</p></div><ElButton :loading="loggingOut" @click="handleLogout"><AppIcon name="logout" :size="16" />退出</ElButton></div>
    </ElDrawer>
  </div>
</template>
<style scoped src="@/styles/layout.css"></style>
