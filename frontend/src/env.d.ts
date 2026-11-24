/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后端接口前缀，开发环境默认走 Vite 代理 */
  readonly VITE_API_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
