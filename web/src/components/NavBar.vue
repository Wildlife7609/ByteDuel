<script setup>
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
</script>

<template>
    <nav class="navbar navbar-expand-lg bg-body-tertiary" data-bs-theme="dark">
        <div class="container">
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarText"
                aria-controls="navbarText" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <router-link class="navbar-brand" :to="{ name: 'home' }">King of Bots</router-link>
            <div class="collapse navbar-collapse" id="navbarText">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active" :to="{ name: 'pk' }">对战</router-link>
                    </li>
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active" :to="{ name: 'record' }">对局列表</router-link>
                    </li>
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active" :to="{ name: 'ranklist' }">排行榜</router-link>
                    </li>
                </ul>
                <ul class="navbar-nav" v-if="!userStore.is_login">
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active"
                            :to="{ name: 'useraccountlogin' }">登录</router-link>
                    </li>
                    <li class="nav-item">
                        <router-link class="nav-link" active-class="active"
                            :to="{ name: 'useraccountregister' }">注册</router-link>
                    </li>
                </ul>
                <ul class="navbar-nav" v-else>
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown"
                            aria-expanded="false">
                            {{ userStore.username }}
                        </a>
                        <ul class="dropdown-menu">
                            <li><router-link class="dropdown-item" active-class="active"
                                    :to="{ name: 'userbots' }">我的bot</router-link></li>
                            <li><a class="dropdown-item" href="#" @click.prevent="userStore.logout">退出登录</a></li>
                        </ul>
                    </li>
                </ul>

            </div>
        </div>
    </nav>

</template>

<style scoped></style>