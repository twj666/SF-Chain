import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { apiClient } from './services/apiUtils'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 初始化API配置
apiClient.initialize().then(() => {
  app.mount('#app')
}).catch(error => {
  console.error('Failed to initialize API config:', error)
  // 即使配置失败也要启动应用，使用默认配置
  app.mount('#app')
})
