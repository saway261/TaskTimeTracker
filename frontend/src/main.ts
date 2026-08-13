import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './assets/theme.css'
import App from './App.vue'
import { router } from './router'

// data-themeの初期適用は index.html のインラインスクリプトが済ませている（§7.1）。

createApp(App).use(createPinia()).use(router).mount('#app')
