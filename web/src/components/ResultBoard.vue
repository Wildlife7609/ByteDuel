<template>
    <div class="result-board">
        <div class="result-board-content">
            <div class="result-board-text">
               <span v-if="pkStore.loser === 'all'">平局</span>
                <span v-else-if="pkStore.loser === 'A' && String(pkStore.gameObject.a_id) === userStore.id">你输了</span>
                <span v-else-if="pkStore.loser === 'B' && String(pkStore.gameObject.b_id) === userStore.id">你输了</span>
                <span v-else>你赢了</span>
            </div>
            <button type="button" class="btn btn-warning result-board-button" @click="handleClick">
                返回大厅
            </button>
        </div>
    </div>
</template>

<script setup>
import { usePkStore } from "@/stores/pk";
import { useUserStore } from "@/stores/user";
const pkStore = usePkStore();
const userStore = useUserStore();
const handleClick = () => { 
pkStore.updateStatus("matching");
pkStore.initOpponent();
pkStore.updateLoser("none");
};


</script>

<style scoped>
.result-board {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: flex-start;
    padding-top: 35vh;
}

/* 与 MatchGround 保持一致的深色毛玻璃风格 */
.result-board-content {
    width: 400px;
    height: 220px;
    background: rgba(30, 41, 59, 0.7);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 20px;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.result-board-text {
    font-size: 36px;
    font-weight: 900;
    font-style: italic;
    color: #ffffff;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
    margin-bottom: 24px;
}

.result-board-button {
    padding: 0.6rem 2.2rem;
    font-size: 1.15rem;
    font-weight: 700;
    border-radius: 12px;
    letter-spacing: 1px;
    box-shadow: 0 8px 20px rgba(245, 158, 11, 0.35);
}

.result-board-button:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 24px rgba(245, 158, 11, 0.5);
}
</style>
