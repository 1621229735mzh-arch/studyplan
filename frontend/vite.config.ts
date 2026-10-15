import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'prompt',
      // 由 src/pwa/registerServiceWorker.ts 自行注册，便于展示“有新版本”提示。
      injectRegister: false,
      manifest: {
        name: '考研学习工作台',
        short_name: '考研工作台',
        description: '个人考研学习计划、记录、进度与复习工作台',
        lang: 'zh-CN',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        background_color: '#ffffff',
        theme_color: '#2f6feb',
        icons: [
          {
            src: 'icon.svg',
            sizes: 'any',
            type: 'image/svg+xml',
            purpose: 'any'
          }
        ]
      },
      workbox: {
        // 只预缓存应用外壳（HTML/JS/CSS/图标）。
        globPatterns: ['**/*.{js,css,html,svg,png,ico,webmanifest,woff2}'],
        navigateFallback: '/index.html',
        // /api 请求绝不落到导航回退，也绝不进入任何运行时缓存：
        // 个人数据只通过 src/offline 的显式快照保存。
        navigateFallbackDenylist: [/^\/api\//],
        runtimeCaching: []
      },
      devOptions: {
        enabled: false
      }
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 本地开发把 /api 转发到 Spring Boot；Cookie 与 CSRF 头原样透传。
      '/api': {
        target: process.env.STUDY_API_PROXY ?? 'http://127.0.0.1:8080',
        changeOrigin: false
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1200
  }
})
