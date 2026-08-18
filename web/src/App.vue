<script setup>
import { onMounted, ref } from 'vue'
import { RouterView } from 'vue-router';

const botList = ref([])
onMounted(async () => {
  try {
    const res = await fetch('http://127.0.0.1:8080/pk/getbotinfo/')
    botList.value = await res.json()
  }
  catch (e) {
    console.error(e)
  }
})
</script>

<template>
  <div class="container">
    <h1>Robot Arena</h1>
    <div v-for="bot in botList" :key="bot.id" class="bot">
      <span> bot-name: {{ bot.name }}</span>
      <span> bot-attack: {{ bot.attack }}</span>
      <span> bot-rating: {{ bot.rating }}</span>
    </div>
  </div>
  <RouterView />
</template>

<style>
body {
  background-image: url('./assets/background.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  min-height: 100vh;
  margin: 10px;

}
</style>
