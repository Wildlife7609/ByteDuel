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

// Same origin as the page; Caddy (production) or Vite (local dev) forwards /api to backend
axios.defaults.baseURL = "/api";

// Silence Chrome's "Blocked aria-hidden" warning when a Bootstrap modal closes
document.addEventListener('hide.bs.modal', () => {
    if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur();
    }
});

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
