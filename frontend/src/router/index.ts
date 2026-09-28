import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import AppLayout from '@/layouts/AppLayout.vue'
import { useSessionStore } from '@/stores/session'

declare module 'vue-router' {
  interface RouteMeta {
    /** true 表示无需登录即可访问 */
    public?: boolean
    /** 页面标题 */
    title?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    // 功能视图按需加载，首屏只加载登录或今日页面
    component: () => import('@/features/account/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'today',
        component: () => import('@/features/today/views/TodayView.vue'),
        meta: { title: '今日' }
      },
      {
        path: 'plan',
        name: 'plan',
        component: () => import('@/features/plan/views/PlanView.vue'),
        meta: { title: '计划' }
      },
      {
        path: 'learning',
        name: 'learning',
        component: () => import('@/features/learning/views/LearningView.vue'),
        meta: { title: '记录' }
      },
      {
        path: 'progress',
        name: 'progress',
        component: () => import('@/features/progress/views/ProgressView.vue'),
        meta: { title: '进度' }
      },
      {
        path: 'review',
        name: 'review',
        component: () => import('@/features/review/views/ReviewView.vue'),
        meta: { title: '复习' }
      },
      {
        path: 'memo',
        name: 'memo',
        component: () => import('@/features/memo/views/MemoView.vue'),
        meta: { title: '备忘录' }
      },
      {
        path: 'targets',
        name: 'targets',
        component: () => import('@/features/target/views/TargetView.vue'),
        meta: { title: '目标' }
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/features/settings/views/SettingsView.vue'),
        meta: { title: '设置' }
      }
    ]
  },
  {
    // 未匹配路径回到首页，避免出现空白页
    path: '/:pathMatch(.*)*',
    redirect: { name: 'today' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 全局登录守卫。
 *
 * 身份未确认（首次进入、刷新页面）时先请求 /api/auth/me；
 * 离线状态下 fetchMe 会退回到本地快照记录的登录名，允许只读查看。
 */
router.beforeEach(async (to) => {
  const session = useSessionStore()
  if (!session.resolved) {
    await session.fetchMe()
  }
  if (to.meta.public === true) {
    // 已登录时访问登录页直接回到首页
    return session.isAuthenticated ? { name: 'today' } : true
  }
  if (!session.isAuthenticated) {
    return {
      name: 'login',
      query: to.fullPath === '/' ? {} : { redirect: to.fullPath }
    }
  }
  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : ''
  document.title = title ? `${title} · 考研学习工作台` : '考研学习工作台'
})

export default router
