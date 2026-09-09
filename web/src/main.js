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

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
