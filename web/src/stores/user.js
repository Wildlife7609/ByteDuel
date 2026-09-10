import { ref } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios';

export const useUserStore = defineStore('user', () => {
    const id = ref('');
    const username = ref('');
    const photo = ref('');
    const token = ref(localStorage.getItem("jwt_token") || '');
    const is_login = ref(false);
    const pulling_info = ref(localStorage.getItem("jwt_token") !== null);

    const login = async (data) => {
        const res = await axios.post("/user/account/token/", {
            username: data.username,
            password: data.password,
        })

        if (res.data.error_message === "success") {
            token.value = res.data.token;
            localStorage.setItem("jwt_token", res.data.token);
            is_login.value = true;
        }
        return res.data;
    }

    const getInfo = async () => {
        try {
            const res = await axios.get("/user/account/info/");
            if (res.data.error_message === "success") {
                id.value = res.data.id;
                username.value = res.data.username;
                photo.value = res.data.photo;
                is_login.value = true;
            }
            return res.data;
        } finally {
            pulling_info.value = false;
        }
    }

    const logout = () => {
        id.value = '';
        username.value = '';
        photo.value = '';
        token.value = '';
        is_login.value = false;
        localStorage.removeItem("jwt_token");
        pulling_info.value = false;
    }

    return {
        id,
        username,
        photo,
        token,
        is_login,
        pulling_info,
        login,
        getInfo,
        logout
    }
})
