<script setup>
import { useUserStore } from '@/stores/user';
import { usePkStore } from '@/stores/pk';
import { ref } from 'vue';
import axios from 'axios';

const userStore = useUserStore();
const pkStore = usePkStore();
const matchBtnText = ref("Find match");
const bots = ref([]);

axios.get("/user/bot/getlist/").then(resp => {
    bots.value = resp.data;
}).catch(error => {
    console.log("Failed to load bots:", error);
})

const click_match_btn = () => {
    if (matchBtnText.value === "Find match") {
        matchBtnText.value = "Cancel";
        // tell the backend to start matching, with the selected bot_id
        pkStore.socket?.send(JSON.stringify({
            event: "start-matching",
            bot_id: pkStore.selectedBotId,  // -1 means playing manually
        }));
    } else {
        matchBtnText.value = "Find match";
        // tell the backend to stop matching
        pkStore.socket?.send(JSON.stringify({
            event: "stop-matching",
        }));
    }
}

</script>

<template>
    <!-- Desktop: you | VS above the controls | opponent. Phone: you | VS | opponent, with the controls below -->
    <div class="matchground">
        <div class="player me">
            <div class="user-photo">
                <img :src="userStore.photo" alt="">
            </div>
            <div class="user-username">{{ userStore.username }}</div>
        </div>
        <div class="vs-badge">VS</div>
        <div class="controls">
            <select class="form-select select-bot" v-model="pkStore.selectedBotId">
                <option :value="-1">Play manually</option>
                <option v-for="bot in bots" :key="bot.id" :value="bot.id">
                    {{ bot.title }}
                </option>
            </select>
            <!-- Controls hint for manual play; keyboard or touch lines are hidden by the CSS -->
            <div v-if="pkStore.selectedBotId === -1" class="play-tip">
                <div class="tip-keyboard"><kbd>W</kbd> <kbd>A</kbd> <kbd>S</kbd> <kbd>D</kbd> to move</div>
                <div class="tip-touch">Use the arrow buttons below the board</div>
                <div>5 seconds per turn, or you lose</div>
                <div class="tip-keyboard">Keys not working? Click the board</div>
            </div>
            <button @click="click_match_btn" type="button" class="btn btn-warning btn-lg match-btn">
                {{ matchBtnText }}
            </button>
        </div>
        <div class="player opponent">
            <div class="user-photo">
                <img :src="pkStore.opponentPhoto" alt="">
            </div>
            <div class="user-username">{{ pkStore.opponentUsername }}</div>
        </div>
    </div>
</template>

<style scoped>
div.matchground {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    grid-template-areas:
        "me vs opponent"
        "me controls opponent";
    align-content: center;
    width: 65vw;
    height: 60vh;
    margin: 3.5rem auto;
    /* frosted glass */
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
    max-width: 100%; /* no wider than its column */
    height: auto;
    aspect-ratio: 1 / 1; /* keep it square so the round avatar is not distorted */
    object-fit: cover;
    /* glow ring and drop shadow */
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
    grid-area: vs;
    align-self: end;
    text-align: center;
    font-size: 48px;
    font-weight: 900;
    font-style: italic;
    color: #f59e0b;
    text-shadow: 0 0 20px rgba(245, 158, 11, 0.6);
}

.player {
    align-self: center;
    min-width: 0; /* let long usernames shrink instead of widening the column */
    text-align: center;
}

.player.me {
    grid-area: me;
}

.player.opponent {
    grid-area: opponent;
}

.controls {
    grid-area: controls;
    display: flex;
    flex-direction: column;
    align-items: center;
}

.select-bot {
    width: 70%;
    margin-top: 1.5rem;
}

.play-tip {
    margin-top: 0.8rem;
    text-align: center;
    font-size: 13px;
    line-height: 1.8;
    color: rgba(255, 255, 255, 0.75);
}

.play-tip kbd {
    background: rgba(255, 255, 255, 0.15);
    border: 1px solid rgba(255, 255, 255, 0.3);
    color: #ffffff;
}

.tip-touch {
    display: none;
}

/* touch devices get on-screen arrows instead of the keyboard */
@media (hover: none) and (pointer: coarse) {
    .tip-keyboard {
        display: none;
    }

    .tip-touch {
        display: block;
    }
}

.match-btn {
    margin-top: 1.5rem;
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

@media (max-width: 767.98px) {
    div.matchground {
        grid-template-columns: 1fr auto 1fr;
        grid-template-areas:
            "me vs opponent"
            "controls controls controls";
        column-gap: 0.5rem;
        width: calc(100vw - 32px);
        height: auto;
        margin: 1.5rem auto;
        padding: 1.5rem 1rem;
    }

    div.user-photo>img {
        width: 88px;
        border-width: 3px;
    }

    div.user-username {
        font-size: 16px;
        margin-top: 0.6rem;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .vs-badge {
        align-self: center;
        font-size: 32px;
    }

    .select-bot {
        width: 100%;
        max-width: 280px;
    }
}
</style>