import { ref } from 'vue'
import { defineStore } from 'pinia'
import axios from 'axios';

export const useUserStore = defineStore('user', () => {
    const id = ref('');
    const username = ref('');
    const photo = ref('');
    const token = ref(localStorage.getItem("jwt_token") || '');
    const is_login = ref(false);

    const login = async (data) => {
        const res = await axios.post("http://localhost:8080/user/account/token/", {
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

    const get_user_info = async () => {
        const res = await axios.get("http://localhost:8080/user/account/info/", {
            headers: {
                Authorization: "Bearer " + token.value,
            }
        });
        if (res.data.error_message === "success") {
            id.value = res.data.id;
            username.value = res.data.username;
            photo.value = res.data.photo;
            is_login.value = true;
        }
        return res.data;
    }

    const logout = () => {
        id.value = '';
        username.value = '';
        photo.value = '';
        token.value = '';
        is_login.value = false;
        localStorage.removeItem("jwt_token");
    }

    return {
        id,
        username,
        photo,
        token,
        is_login,
        login,
        get_user_info,
        logout
    }
})
