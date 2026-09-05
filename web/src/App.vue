<script setup>
import NavBar from '@/components/NavBar.vue'
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import axios from 'axios';

axios.post("http://localhost:8080/user/account/token/", {
  username: "yxc",
  password: "pyxc"
}).then(res => console.log(res.data));
</script>

<template>
  <NavBar />
  <RouterView />
</template>

<style>
body {
  background-image: url('@/assets/images/background.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  min-height: 100vh;
}
</style>
