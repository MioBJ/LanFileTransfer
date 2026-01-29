import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    {
      name: 'strip-crossorigin',
      transformIndexHtml(html) {
        return html.replace(/\s+crossorigin\b/g, '')
      }
    }
  ],
  
  // 路径别名
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  
  // 构建配置（用于内置到 iOS App）
  build: {
    // 所有资源文件输出到根目录（不使用子目录）
    // 这样可以避免 iOS Xcode 打包时目录结构被打平的问题
    assetsDir: '',
    
    // 不生成 source map（减小体积）
    sourcemap: false,
    
    // 关闭 CSS 代码分割，所有 CSS 打包到一个文件
    cssCodeSplit: false,
    
    // 构建目标
    target: 'es2015',
    
    // 设置块大小警告限制
    chunkSizeWarningLimit: 1000
  },
  
  // 基础路径 - 使用相对路径
  // 这是内置到 App 的关键配置
  base: './'
})
