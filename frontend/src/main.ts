import { createPinia } from 'pinia'
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from './App.vue'
import { primeCsrfToken, setUnauthorizedHandler } from './api/http'
import { useSessionStore } from './stores/session'
import { initServiceWorker } from './pwa/registerServiceWorker'
import router from './router'
import './styles/index.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 会话失效（401）时统一清空本地状态并回到登录页，避免停留在半加载页面。
setUnauthorizedHandler(() => {
  const session = useSessionStore()
  session.markAnonymous()
  const current = router.currentRoute.value
  if (current.name !== 'login') {
    void router.replace({ name: 'login', query: { redirect: current.fullPath } })
  }
})

// 预热 CSRF 令牌：后端登录等写操作必须带 X-XSRF-TOKEN。
// 失败（例如离线启动）不阻塞应用，写操作时还会再取一次。
void primeCsrfToken()

app.mount('#app')

// 生产环境注册应用外壳的 Service Worker（只缓存外壳，不缓存 /api）。
initServiceWorker()
