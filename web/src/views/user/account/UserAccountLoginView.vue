<script setup>
import ContentField from '@/components/ContentField.vue';
import { useUserStore } from '@/stores/user';
import { ref } from 'vue';
import router from '@/router';

const userStore = useUserStore();

let username = ref("");
let password = ref("");
let error_message = ref("");

const login = async () => {
    error_message.value = "";
    try {
        const res = await userStore.login({
            username: username.value,
            password: password.value
        });
        if (res.error_message === "success") {
            await userStore.get_user_info();
            router.push({ name: "home" });
        } else {
            error_message.value = res.error_message;
        }
    } catch (error) {
        if (error.response && error.response.status === 403) {
            error_message.value = "用户名或密码错误";
        } else {
            error_message.value = "服务器连接失败";
        }
    }

}
</script>

<template>
    <ContentField>
        <div class="row justify-content-md-center">
            <div class="col-3">
                <form @submit.prevent="login">
                    <div class="mb-3">
                        <label for="username" class="form-label">用户名</label>
                        <input v-model="username" type="text" class="form-control" id="username" placeholder="请输入用户名">
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">密码</label>
                        <input v-model="password" type="password" class="form-control" id="password"
                            placeholder="请输入密码">
                    </div>
                    <div class="error-message">
                        {{ error_message }}
                    </div>
                    <button type="submit" class="btn btn-primary">登录</button>
                </form>
            </div>
        </div>
    </ContentField>
</template>

<style scoped>
button {
    width: 100%;
}

div.error-message {
    color: red;
}
</style>