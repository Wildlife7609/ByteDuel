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
        if (error.response && error.response.status === 429) {
            error_message.value = "Too many requests. Please try again later."; // rate limited by Cloudflare
        } else {
            error_message.value = "Could not connect to the server";
        }
    }
}

</script>

<template>
    <ContentField>
        <div class="row justify-content-center">
            <div class="col-12 col-sm-8 col-md-6 col-lg-4 col-xl-3">
                <form @submit.prevent="register">
                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input v-model="username" type="text" class="form-control" id="username" placeholder="Choose a username">
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input v-model="password" type="password" class="form-control" id="password"
                            placeholder="Choose a password">
                    </div>
                    <div class="mb-3">
                        <label for="confirmedPassword" class="form-label">Confirm password</label>
                        <input v-model="confirmedPassword" type="password" class="form-control" id="confirmedPassword"
                            placeholder="Enter the password again">
                    </div>
                    <div class="error-message">
                        {{ error_message }}
                    </div>
                    <button type="submit" class="btn btn-primary">Sign up</button>
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