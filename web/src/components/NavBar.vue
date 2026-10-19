<script setup>
import { useUserStore } from '@/stores/user';
import router from '@/router';
const userStore = useUserStore();

const logout = () => {
    userStore.logout();
    router.push({ name: "useraccountlogin" });
}
</script>

<template>
    <nav class="navbar navbar-expand-lg bg-body-tertiary" data-bs-theme="dark">
        <div class="container">
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarText"
                aria-controls="navbarText" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <router-link class="navbar-brand" :to="{ name: 'home' }">ByteDuel</router-link>
            <div class="collapse navbar-collapse" id="navbarText">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active" :to="{ name: 'pk' }">Play</router-link>
                    </li>
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active" :to="{ name: 'record' }">Matches</router-link>
                    </li>
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active" :to="{ name: 'ranklist' }">Ranklist</router-link>
                    </li>
                </ul>
                <ul class="navbar-nav" v-if="userStore.is_login">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown"
                            aria-expanded="false">
                            {{ userStore.username }}
                        </a>
                        <ul class="dropdown-menu">
                            <li><router-link class="dropdown-item" active-class="active"
                                    :to="{ name: 'userbots' }">My Bots</router-link></li>
                            <li><a class="dropdown-item" href="#" @click.prevent="logout">Log out</a></li>
                        </ul>
                    </li>
                </ul>
                <ul class="navbar-nav" v-else-if="!userStore.pulling_info">
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active"
                            :to="{ name: 'useraccountlogin' }">Log in</router-link>
                    </li>
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active"
                            :to="{ name: 'useraccountregister' }">Sign up</router-link>
                    </li>
                </ul>


            </div>
        </div>
    </nav>

</template>

<style scoped></style>