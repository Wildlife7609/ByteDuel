import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from '@/App.vue'
import router from '@/router'
import axios from 'axios'

axios.interceptors.request.use(config => {
    const jwt_token = localStorage.getItem("jwt_token");
    if (jwt_token) {
        config.headers.Authorization = "Bearer " + jwt_token;
    }
    return config;
}, error => {
    throw error;
});

// 和页面同域名，由 Caddy（线上）或 Vite（本地开发）把 /api 转发给 backend
axios.defaults.baseURL = "/api";

// 消除 Bootstrap Modal 关闭时 Chrome 报 Blocked aria-hidden 警告
document.addEventListener('hide.bs.modal', () => {
    if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur();
    }
});

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
