<script setup>
import ContentField from '@/components/ContentField.vue';
import { ref } from 'vue';
import router from '@/router';
import axios from 'axios';

const username = ref("");
const password = ref("");
const confirmedPassword = ref("");
const error_message = ref("");

const register = async () => {
    error_message.value = "";
    try {
        const res = await axios.post("/user/account/register/", {
            username: username.value,
            password: password.value,
            confirmedPassword: confirmedPassword.value
        });
        if (res.data.error_message === "success") {
            router.push({ name: "useraccountlogin" });
        } else {
            error_message.value = res.data.error_message;
        }
    } catch (error) {
        error_message.value = "服务器连接失败";
    }
}

</script>

<template>
    <ContentField>
        <div class="row justify-content-md-center">
            <div class="col-3">
                <form @submit.prevent="register">
                    <div class="mb-3">
                        <label for="username" class="form-label">用户名</label>
                        <input v-model="username" type="text" class="form-control" id="username" placeholder="请输入用户名">
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">密码</label>
                        <input v-model="password" type="password" class="form-control" id="password"
                            placeholder="请输入密码">
                    </div>
                    <div class="mb-3">
                        <label for="confirmedPassword" class="form-label">确认密码</label>
                        <input v-model="confirmedPassword" type="password" class="form-control" id="confirmedPassword"
                            placeholder="请再次输入密码">
                    </div>
                    <div class="error-message">
                        {{ error_message }}
                    </div>
                    <button type="submit" class="btn btn-primary">注册</button>
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