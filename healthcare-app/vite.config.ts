import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],
    server: {
      proxy: {
        '/api': {
          // 后端默认 8080；本机 8080 被占用时可启动到 8081，
          // 并在 .env.local 中设置 VITE_API_PROXY_TARGET=http://localhost:8081
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8081',
          // 保留原始 Host：手机通过局域网 IP 访问时，后端才能识别为同源请求
          changeOrigin: false,
        },
      },
    },
  }
})
