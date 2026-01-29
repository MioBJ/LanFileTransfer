<template>
  <div id="app-root">
    <router-view v-slot="{ Component }">
      <transition :name="transitionName" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'
import { useConfigStore } from './stores/config'
import { showFailToast } from 'vant'

const router = useRouter()
const configStore = useConfigStore()

// 页面切换动画名称
const transitionName = ref('page-forward')

// 同级Tab页面（不需要滑动动画）
const tabPages = ['/home', '/history']

// 历史栈（用于判断前进/后退）
const historyStack: string[] = []

// 调试开关（设为 true 可在控制台查看导航日志）
const DEBUG_NAV = false

// 监听路由变化，动态设置动画
router.beforeEach((to, from) => {
  const toPath = to.path
  const fromPath = from.path
  
  if (DEBUG_NAV) {
    console.log('[NAV] ================== beforeEach ==================')
    console.log('[NAV] from:', fromPath, '→ to:', toPath)
    console.log('[NAV] 当前历史栈:', [...historyStack])
    console.log('[NAV] 浏览器历史长度:', window.history.length)
  }
  
  // 首次加载或从根路径进入：初始化历史栈
  if (!fromPath || fromPath === '/') {
    if (historyStack.length === 0) {
      historyStack.push(toPath)
      if (DEBUG_NAV) console.log('[NAV] 首次加载，初始化历史栈:', [...historyStack])
    }
    transitionName.value = 'page-fade' // 首次加载不需要动画
    return
  }
  
  // Tab页面之间切换：使用淡入淡出，不修改历史栈
  if (tabPages.includes(toPath) && tabPages.includes(fromPath)) {
    if (DEBUG_NAV) console.log('[NAV] Tab切换，使用淡入淡出')
    transitionName.value = 'page-fade'
    return
  }
  
  // 判断是否是后退操作：目标路径是历史栈的倒数第二个
  const isBack = historyStack.length >= 2 && historyStack[historyStack.length - 2] === toPath
  
  if (DEBUG_NAV) {
    console.log('[NAV] 栈倒数第2个:', historyStack[historyStack.length - 2])
    console.log('[NAV] 判断为:', isBack ? '后退' : '前进')
  }
  
  if (isBack) {
    // 后退：从左侧滑入
    historyStack.pop()
    transitionName.value = 'page-back'
    if (DEBUG_NAV) console.log('[NAV] 后退后历史栈:', [...historyStack])
  } else {
    // 前进：从右侧滑入
    // 避免重复添加相同路径
    if (historyStack[historyStack.length - 1] !== toPath) {
      historyStack.push(toPath)
    }
    transitionName.value = 'page-forward'
    if (DEBUG_NAV) console.log('[NAV] 前进后历史栈:', [...historyStack])
  }
})

// 导航完成后的日志
router.afterEach((to, from) => {
  if (DEBUG_NAV) {
    console.log('[NAV] ================== afterEach ==================')
    console.log('[NAV] 导航完成:', from.path, '→', to.path)
    console.log('[NAV] 最终历史栈:', [...historyStack])
  }
})

// 初始化配置
onMounted(async () => {
  try {
    if (!configStore.initialized) {
      await configStore.init()
    }
  } catch (error) {
    console.error('初始化配置失败:', error)
    showFailToast('初始化失败，请刷新页面重试')
  }
})

// 全局错误捕获
onErrorCaptured((err, instance, info) => {
  console.error('Vue错误捕获:', err, info)
  showFailToast('发生错误，请刷新页面重试')
  return false
})

// 全局未捕获错误处理
window.addEventListener('error', (event) => {
  console.error('全局错误:', event.error)
})

// 全局Promise错误处理
window.addEventListener('unhandledrejection', (event) => {
  console.error('未处理的Promise拒绝:', event.reason)
  event.preventDefault()
})
</script>

<style>
/* ===== 前进动画（从右侧滑入）===== */
.page-forward-enter-active {
  animation: slideInRight 0.3s ease-out;
}

.page-forward-leave-active {
  animation: slideOutLeft 0.25s ease-in;
}

/* ===== 后退动画（从左侧滑入）===== */
.page-back-enter-active {
  animation: slideInLeft 0.3s ease-out;
}

.page-back-leave-active {
  animation: slideOutRight 0.25s ease-in;
}

/* ===== Tab切换动画（淡入淡出）===== */
.page-fade-enter-active {
  animation: fadeIn 0.2s ease-out;
}

.page-fade-leave-active {
  animation: fadeOut 0.15s ease-in;
}

/* ===== 动画关键帧 ===== */
@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideOutLeft {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(-30px);
  }
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideOutRight {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(30px);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes fadeOut {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

/* 确保根元素充满视口 */
#app-root {
  min-height: 100vh;
  width: 100%;
}
</style>
