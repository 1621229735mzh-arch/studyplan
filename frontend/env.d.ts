/// <reference types="vite/client" />
/// <reference types="vite-plugin-pwa/client" />
/// <reference types="element-plus/global" />

interface ImportMetaEnv {
  /** 后端接口前缀，默认 /api */
  readonly VITE_API_BASE?: string
  /** 页面标题（可选） */
  readonly VITE_APP_TITLE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
