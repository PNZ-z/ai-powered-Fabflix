import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBaseUrl = env.VITE_API_BASE_URL || '/api'
  const backendTarget = env.FABFLIX_BACKEND_TARGET || 'http://localhost:8081'

  return {
    appType: 'spa',
    plugins: [react()],
    server: {
      proxy: {
        [apiBaseUrl]: {
          target: backendTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
