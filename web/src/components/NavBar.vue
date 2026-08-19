<script setup>
</script>

<template>
    <nav class="navbar navbar-expand-lg bg-body-tertiary" data-bs-theme="dark">
        <div class="container">
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
                <ul class="navbar-nav">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown"
                            aria-expanded="false">
                            JingLi
                        </a>
                        <ul class="dropdown-menu">
                            <li><router-link class="dropdown-item" active-class="active"
                                    :to="{ name: 'userbots' }">我的bot</router-link></li>
                            <li><a class="dropdown-item" href="#">退出登录</a></li>
                        </ul>
                    </li>
                </ul>

            </div>
        </div>
    </nav>

</template>

<style scoped></style>