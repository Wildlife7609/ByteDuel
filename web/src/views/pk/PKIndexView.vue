<script setup>
import PlayGround from '@/components/PlayGround.vue'
import { usePkStore } from '@/stores/pk';
import { useUserStore } from '@/stores/user';
import { onMounted, onUnmounted } from 'vue';
import MatchGround from '@/components/MatchGround.vue';

const pkStore = usePkStore();
const userStore = useUserStore();
const socketUrl = `ws://localhost:8080/websocket/${userStore.token}`;
let socket = null;

onMounted(() => {
    socket = new WebSocket(socketUrl);
    socket.onopen = () => {
        console.log("连接成功");
    }
    pkStore.updateSocket(socket);
    socket.onmessage = (msg) => {
        const data = JSON.parse(msg.data);
        if (data.event === "match-success") {
            console.log("匹配成功");
            pkStore.updateOpponent(data.opponent_username, data.opponent_photo);
            setTimeout(() => {
                pkStore.updateStatus("playing");
            }, 2000)
            pkStore.updateGameObject(data.game);

        }
    }
    socket.onclose = () => {
        console.log("连接关闭");
    }
})

onUnmounted(() => {
    socket.close();
    pkStore.updateStatus("matching");
    pkStore.initOpponent();
})

</script>

<template>
    <PlayGround v-if="pkStore.status === 'playing'" />
    <MatchGround v-if="pkStore.status === 'matching'" />
</template>

<style scoped></style>