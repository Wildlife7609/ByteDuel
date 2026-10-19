<script setup>
import ContentField from '@/components/ContentField.vue';
import { useUserStore } from '@/stores/user';
import { ref } from 'vue';
import router from '@/router';

const userStore = useUserStore();

const username = ref("");
const password = ref("");
const error_message = ref("");

const login = async () => {
    error_message.value = "";
    try {
        await userStore.login({
            username: username.value,
            password: password.value
        });
        await userStore.getInfo();
        router.push({ name: "home" });
    } catch (error) {
        if (error.response && error.response.status === 403) {
            error_message.value = "Incorrect username or password";
        } else if (error.response && error.response.status === 429) {
            // a locked account gets JSON with error_message from backend; Cloudflare rate limiting returns HTML without that field
            error_message.value = error.response.data?.error_message || "Too many requests. Please try again later.";
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
                <form @submit.prevent="login">
                    <div class="mb-3">
                        <label for="username" class="form-label">Username</label>
                        <input v-model="username" type="text" class="form-control" id="username" placeholder="Enter your username">
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input v-model="password" type="password" class="form-control" id="password"
                            placeholder="Enter your password">
                    </div>
                    <div class="error-message">
                        {{ error_message }}
                    </div>
                    <button type="submit" class="btn btn-primary">Log in</button>
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