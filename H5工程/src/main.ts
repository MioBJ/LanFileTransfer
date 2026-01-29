import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// 引入Vant样式
import 'vant/lib/index.css'

// 引入暖阳传递主题样式（覆盖Vant默认样式）
import './styles/index.css'

// 创建应用实例
const app = createApp(App)

// 使用Pinia状态管理
app.use(createPinia())

// 使用Vue Router
app.use(router)

// 挂载应用
app.mount('#app')
