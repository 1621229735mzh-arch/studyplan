import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

/**
 * 单元测试独立配置：不加载 vite.config.ts 里的 PWA 插件，
 * 避免测试过程生成 Service Worker 产物。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'jsdom',
    include: ['tests/unit/**/*.spec.ts'],
    exclude: ['tests/e2e/**', 'node_modules/**', 'dist/**'],
    setupFiles: ['./tests/setup.ts'],
    restoreMocks: true,
    // 显式从 'vitest' 导入 describe/it/expect，因此不开启 globals。
    globals: false
  }
})
