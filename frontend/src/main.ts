import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './assets/theme.css'
import App from './App.vue'
import { router } from './router'
import { applyStoredTheme } from './theme'

applyStoredTheme()

createApp(App).use(createPinia()).use(router).mount('#app')
