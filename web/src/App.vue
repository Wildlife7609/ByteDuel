<script setup>
import NavBar from '@/components/NavBar.vue'
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import axios from 'axios';

axios.post("http://localhost:8080/user/account/token/", {
  username: "jli",
  password: "pli"
}).then(res => console.log(res.data));

axios.get("http://localhost:8080/user/account/info/", {
  headers: {
    Authorization: "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI3NjM4NDA3MTE2ZGU0MzUyODkxODk4MjM4NjU4OTc3YiIsInN1YiI6IjIiLCJpc3MiOiJzZyIsImlhdCI6MTc4ODY1NDc4OCwiZXhwIjoxNzg5ODY0Mzg4fQ.NvTf2rYokzcucTYy1tbSvjy5IXFHDPe6-nbAHoT6B-s"
  }
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
