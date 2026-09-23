<template>
    <div class="result-board">
        <div class="result-board-content">
            <div class="result-board-text">
               <span v-if="pkStore.loser === 'all'">平局</span>
                <span v-else-if="pkStore.loser === 'A' && String(pkStore.gameObject.a_id) === userStore.id">你输了</span>
                <span v-else-if="pkStore.loser === 'B' && String(pkStore.gameObject.b_id) === userStore.id">你输了</span>
                <span v-else>你赢了</span>
            </div>
            <div class="result-board-button" @click="handleClick">
                返回大厅
            </div>  
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

.result-board-content {
    width: 400px;
    height: 200px;
    background-color: #fff;
    border-radius: 10px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.result-board-text {
    font-size: 24px;
    font-weight: bold;
    margin-bottom: 20px;
    font-style: italic;
    padding: 10px 20px;
}

.result-board-button {
    width: 200px;
    height: 50px;
    background-color: #409eff;
    color: #fff;
    border-radius: 5px;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
}

.result-board-button:hover {
    background-color: #66b1ff;
}
</style>
