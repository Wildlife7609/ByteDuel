<script setup>
import PlayGround from '@/components/PlayGround.vue'
import { usePkStore } from '@/stores/pk';
import { useUserStore } from '@/stores/user';
import { onMounted, onUnmounted } from 'vue';
import MatchGround from '@/components/MatchGround.vue';
import ResultBoard from '@/components/ResultBoard.vue';

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
            }, 200)
            pkStore.updateGameObject(data.game);
        }
        else if (data.event === "move") {
            console.log("收到移动命令", data);
            const gameMap = pkStore.gameMap;
            if (!gameMap || !gameMap.snakes) {
                console.log("⚠️ gameMap 还没准备好，方向被丢弃了", gameMap); // 加这行
                return;
            }
            const [snake0, snake1] = gameMap.snakes;
            snake0.set_direction(data.a_step);
            snake1.set_direction(data.b_step);
        }
        else if (data.event === "result") {
            console.log("比赛结果", data);
            const gameMap = pkStore.gameMap;
            const [snake0, snake1] = gameMap.snakes;
            if (data.a_step != null) snake0.eye_direction = data.a_step;
            if (data.b_step != null) snake1.eye_direction = data.b_step;
            if (data.loser == "all") {
                snake0.status = "die";
                snake1.status = "die";
            } else if (data.loser === "A") {
                snake0.status = "die";
            } else if (data.loser === "B") {
                snake1.status = "die";
            }
            pkStore.updateLoser(data.loser);
        }

    }
    socket.onclose = (e) => {
        console.log("连接关闭");
        if (e.code === 4000) { // 后端踢下线：同一账号在别的窗口连上了
            alert("你的账号已在其他窗口进入对战页面，当前窗口已断开");
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