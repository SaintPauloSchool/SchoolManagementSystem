import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 載入當前 mode 的環境變數
  const env = loadEnv(mode, process.cwd(), '')

  return {
    base: env.VITE_APP_BASE_PATH || '/school-management-system/',
    plugins: [
      vue({
        template: {
          compilerOptions: {
            // 將所有帶有 @ 符號的標籤視爲自定義元素
            isCustomElement: (tag) => tag.startsWith('@')
          }
        }
      })
    ],
    define: {
      // Vue 3 特性標誌配置
      __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: false,
      __VUE_OPTIONS_API__: true,
      __VUE_PROD_DEVTOOLS__: false
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      }
    },
    server: {
      port: 3001,
      proxy: {
        [env.VITE_API_BASE_PATH || '/sms-api']: {
          target: 'http://localhost:8006',
          changeOrigin: true
        },
        '/profile': {
          target: 'http://localhost:8006/sms-api',
          changeOrigin: true
        }
      }
    },
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      // 關閉 sourcemap，防止原始碼洩漏
      sourcemap: false,
      rollupOptions: {
        output: {
          chunkFileNames: 'js/[name]-[hash].js',
          entryFileNames: 'js/[name]-[hash].js',
          assetFileNames: '[ext]/[name]-[hash].[ext]'
        }
      }
    },
    esbuild: {
      // 生產環境下移除 console.log 和 debugger
      drop: ['console', 'debugger']
    },
    optimizeDeps: {
      include: ['element-plus']
    }
  }
})
