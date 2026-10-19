<script setup>
import PlayGround from '@/components/PlayGround.vue'
import { usePkStore } from '@/stores/pk';
import { useUserStore } from '@/stores/user';
import { useRecordStore } from '@/stores/record';
import { onMounted, onUnmounted } from 'vue';
import MatchGround from '@/components/MatchGround.vue';
import ResultBoard from '@/components/ResultBoard.vue';

const pkStore = usePkStore();
const userStore = useUserStore();
const recordStore = useRecordStore();
// Same origin as the page; an HTTPS page must use wss
const socketUrl = `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/websocket/${userStore.token}`;
let socket = null;

onMounted(() => {
    recordStore.updateIsRecord(false);
    socket = new WebSocket(socketUrl);
    socket.onopen = () => {
        console.log("Connected");
    }
    pkStore.updateSocket(socket);
    socket.onmessage = (msg) => {
        const data = JSON.parse(msg.data);
        if (data.event === "match-success") {
            console.log("Match found");
            pkStore.updateOpponent(data.opponent_username, data.opponent_photo);
            setTimeout(() => {
                pkStore.updateStatus("playing");
            }, 200)
            pkStore.updateGameObject(data.game);
        }
        else if (data.event === "move") {
            console.log("Received move", data);
            const gameMap = pkStore.gameMap;
            if (!gameMap || !gameMap.snakes) {
                console.log("⚠️ gameMap is not ready, move dropped", gameMap);
                return;
            }
            const [snake0, snake1] = gameMap.snakes;
            snake0.set_direction(data.a_step);
            snake1.set_direction(data.b_step);
        }
        else if (data.event === "result") {
            console.log("Game result", data);
            const gameMap = pkStore.gameMap;
            const [snake0, snake1] = gameMap.snakes;
            // show the result after both snakes finish their queued moves
            snake0.finish(data.loser === "all" || data.loser === "A", data.a_step);
            snake1.finish(data.loser === "all" || data.loser === "B", data.b_step);
            pkStore.updateLoser(data.loser);
        }

    }
    socket.onclose = (e) => {
        console.log("Connection closed");
        if (e.code === 4000) { // kicked by the backend: the same account connected from another window
            alert("Your account opened the Play page in another window, so this window has been disconnected.");
        }
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
    <ResultBoard v-if="pkStore.loser !== 'none'" />
</template>

<style scoped></style>