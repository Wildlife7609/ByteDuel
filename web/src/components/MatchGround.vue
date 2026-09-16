<script setup>
import { useUserStore } from '@/stores/user';
import { usePkStore } from '@/stores/pk';
import { ref } from 'vue';
import axios from 'axios';

const userStore = useUserStore();
const pkStore = usePkStore();
const matchBtnText = ref("开始匹配");
const bots = ref([]);

axios.get("/user/bot/getlist/").then(resp => {
    bots.value = resp.data;
}).catch(error => {
    console.log("获取Bot列表失败：", error);
})

const click_match_btn = () => {
    if (matchBtnText.value === "开始匹配") {
        matchBtnText.value = "取消";
        // 向后端发送开始匹配消息（带上选中的 bot_id）
        pkStore.socket.send(JSON.stringify({
            event: "start-matching",
            bot_id: select_bot.value,
        }));
    } else {
        matchBtnText.value = "开始匹配";
        // 向后端发送取消匹配消息
        pkStore.socket.send(JSON.stringify({
            event: "stop-matching",
        }));
    }
}

</script>

<template>
    <div class="matchground">
        <div class="row align-items-center h-100">
            <!-- 左侧：自己 -->
            <div class="col-4 text-center">
                <div class="user-photo">
                    <img :src="userStore.photo" alt="">
                </div>
                <div class="user-username">{{ userStore.username }}</div>
            </div>
            <!-- 中间：VS 与出战配置 -->
            <div class="col-4 text-center middle-panel">
                <div class="vs-badge">VS</div>
                <div class="user-select-bot my-4">
                    <select class="form-select select-bot">
                        <option value="-1" selected>亲自出战</option>
                        <option v-for="bot in bots" :key="bot.id" :value="bot.id">
                            {{ bot.title }}
                        </option>
                    </select>
                </div>
                <button @click="click_match_btn" type="button" class="btn btn-warning btn-lg match-btn">
                    {{ matchBtnText }}
                </button>
            </div>
            <!-- 右侧：对手 -->
            <div class="col-4 text-center">
                <div class="user-photo">
                    <img :src="pkStore.opponent_photo" alt="">
                </div>
                <div class="user-username">{{ pkStore.opponent_username }}</div>
            </div>
        </div>
    </div>
</template>

<style scoped>
div.matchground {
    width: 65vw;
    height: 60vh;
    margin: 3.5rem auto;
    /* 现代高级毛玻璃效果 */
    background: rgba(30, 41, 59, 0.7);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 20px;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
    padding: 2rem;
}

div.user-photo>img {
    border-radius: 50%;
    width: 16vh;
    height: 16vh;
    object-fit: cover;
    /* 头像光晕描边与立体阴影 */
    border: 5px solid rgba(255, 255, 255, 0.8);
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.4);
}


div.user-username {
    font-size: 22px;
    font-weight: 700;
    color: #ffffff;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
    margin-top: 1.2rem;
    letter-spacing: 0.5px;
}


.vs-badge {
    font-size: 48px;
    font-weight: 900;
    font-style: italic;
    color: #f59e0b;
    text-shadow: 0 0 20px rgba(245, 158, 11, 0.6);
}

.user-select-bot {
    width: 70%;
    margin: 1.5rem auto;
}

.match-btn {
    padding: 0.6rem 2.2rem;
    font-size: 1.15rem;
    font-weight: 700;
    border-radius: 12px;
    letter-spacing: 1px;
    box-shadow: 0 8px 20px rgba(245, 158, 11, 0.35);
}

.match-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 24px rgba(245, 158, 11, 0.5);
}
</style>