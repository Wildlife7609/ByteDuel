<script setup>
import NavBar from './components/NavBar.vue'
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";

components: {
  NavBar
}

</script>

<template>
  <NavBar />
  <RouterView />
</template>

<style>
body {
  background-image: url('./assets/background.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  min-height: 100vh;
}
</style>
