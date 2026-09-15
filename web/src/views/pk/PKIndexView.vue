<script setup>
import PlayGround from '@/components/PlayGround.vue'
import { usePkStore } from '@/stores/pk';
import { useUserStore } from '@/stores/user';
import { onMounted, onUnmounted } from 'vue';

const pkStore = usePkStore();
const userStore = useUserStore();
const socketUrl = `ws://localhost:8080/websocket/${userStore.token}`;
let socket = null;

onMounted(() => {
    socket = new WebSocket(socketUrl);
    socket.onopen = () => {
        console.log("连接成功");
    }
    socket.onmessage = (msg) => {
        const data = JSON.parse(msg.data);
        console.log("收到消息: ", data);
    }
    socket.onclose = () => {
        console.log("连接关闭");
    }
})

onUnmounted(() => {
    socket.close();
})

</script>

<template>
    <PlayGround />
</template>

<style scoped></style>