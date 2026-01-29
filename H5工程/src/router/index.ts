import { createRouter, createWebHistory, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Root',
    redirect: () => {
      // 检查是否已配置服务器
      // 从localStorage读取配置
      try {
        const configStr = localStorage.getItem('appConfig')
        if (configStr) {
          const config = JSON.parse(configStr)
          // 如果已配置服务器IP，直接跳转到首页
          if (config.server && config.server.ip) {
            return '/home'
          }
        }
      } catch (error) {
        console.error('读取配置失败:', error)
      }
      // 否则跳转到引导页
      return '/guide'
    }
  },
  {
    path: '/guide',
    name: 'Guide',
    component: () => import('../views/Guide.vue'),
    meta: {
      title: '欢迎使用'
    }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue'),
    meta: {
      title: '服务器配置'
    }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '文件传输'
    }
  },
  {
    path: '/confirm',
    name: 'Confirm',
    component: () => import('../views/Confirm.vue'),
    meta: {
      title: '确认上传'
    }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('../views/History.vue'),
    meta: {
      title: '历史记录'
    }
  }
]

// 创建路由实例
// 非 http/https 协议（如 file://、lft://）使用 hash 模式，避免 history 模式无法匹配路径
const isBrowserEnv = typeof window !== 'undefined'
const protocol = isBrowserEnv ? window.location.protocol : ''
const useHash = protocol !== 'http:' && protocol !== 'https:'
const router = createRouter({
  history: useHash ? createWebHashHistory() : createWebHistory(),
  routes
})

// 路由守卫：设置页面标题和权限检查
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 局域网文件传输`
  }
  
  // 检查是否需要配置（除了引导页和配置页，其他页面都需要先配置）
  if (to.path !== '/guide' && to.path !== '/settings' && to.path !== '/') {
    try {
      const configStr = localStorage.getItem('appConfig')
      if (configStr) {
        const config = JSON.parse(configStr)
        if (!config.server || !config.server.ip) {
          // 未配置，跳转到配置页
          next('/settings')
          return
        }
      } else {
        // 没有配置，跳转到引导页
        next('/guide')
        return
      }
    } catch (error) {
      console.error('检查配置失败:', error)
      next('/guide')
      return
    }
  }
  
  next()
})

export default router

