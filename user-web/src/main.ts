import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'highlight.js/styles/atom-one-dark.css'
import App from './App.vue'
import router from './router'
import './styles/theme.css'
import { enableImageLightbox } from './utils/lightbox'
import { enableBodyLinkHandler } from './utils/markdown'
import { attachScrollMemory } from './utils/scroll'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
enableImageLightbox()
// 全局统一处理正文 Markdown 链接：站内 SPA、外链新标签页（当前页不变）
enableBodyLinkHandler(router)
// 记录每个页面的滚动位置，刷新后不再被强制回顶
attachScrollMemory(router)
