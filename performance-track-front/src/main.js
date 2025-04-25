import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 创建应用实例
const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router)
app.use(ElementPlus, {
  size: 'default'
})

// 处理 ResizeObserver 警告
const debounce = (fn, delay) => {
  let timeoutId
  return (...args) => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => fn.apply(this, args), delay)
  }
}

// 创建一个新的 MutationObserver 实例
const mutationObserver = new MutationObserver(debounce(() => {
  window.dispatchEvent(new Event('resize'))
}, 16))

// 监听 DOM 变化
mutationObserver.observe(document.body, {
  childList: true,
  subtree: true
})

// 全局错误处理
app.config.errorHandler = (err, vm, info) => {
  console.error('Global Error:', err)
  console.error('Error Info:', info)
}

// 忽略某些特定警告
app.config.warnHandler = (msg, vm, trace) => {
  if (msg.includes('ResizeObserver') || msg.includes('Invalid prop')) {
    return
  }
  console.warn(msg, trace)
}

app.mount('#app')
