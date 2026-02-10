import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  base: process.env.VITE_BASE_PATH || '/sf/',
  plugins: [
    vue(),
    vueDevTools(),
  ],
  server: {
    proxy: {
      '/sf-chain': {
        target: process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:19090',
        changeOrigin: true
      },
      '/v1': {
        target: process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:19090',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
