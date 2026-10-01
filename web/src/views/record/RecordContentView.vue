<script setup>
import PlayGround from '@/components/PlayGround.vue'
import { useRecordStore } from '@/stores/record'
import { usePkStore } from '@/stores/pk'
import { useRouter } from 'vue-router'
import { ref, computed } from 'vue'

const recordStore = useRecordStore()
const pkStore = usePkStore()
const router = useRouter()

if (!pkStore.gameObject) { // 刷新页面后 store 被清空，回到列表页
    router.replace({ name: 'record' });
}

const replayKey = ref(0);
const replay = () => {
    recordStore.updateIsFinished(false);
    replayKey.value++; // key 变化会销毁并重建 PlayGround，从第 0 步重新播放
};

const resultText = computed(() => {
    if (recordStore.loser === "all") return "平局";
    if (recordStore.loser === "A") return "玩家 B 获胜";
    return "玩家 A 获胜";
});
</script>

<template>
    <PlayGround v-if="pkStore.gameObject" :key="replayKey" />
    <div v-if="recordStore.is_finished" class="record-result">
        <div class="record-result-text">{{ resultText }}</div>
        <button type="button" class="btn btn-warning record-result-button" @click="replay">重新播放</button>
        <button type="button" class="btn btn-outline-light record-result-button" @click="router.push({ name: 'record' })">返回列表</button>
    </div>
</template>

<style scoped>
.record-result {
    display: flex;
    align-items: center;
    gap: 1rem;
    width: fit-content;
    margin: -2rem auto 0;
    padding: 1rem 2rem;
    /* 与 MatchGround / ResultBoard 保持一致的深色毛玻璃风格 */
    background: rgba(30, 41, 59, 0.7);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 20px;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.record-result-text {
    font-size: 22px;
    font-weight: 900;
    font-style: italic;
    color: #ffffff;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
    margin-right: 1rem;
}

.record-result-button {
    font-weight: 700;
    border-radius: 12px;
    letter-spacing: 1px;
}

.record-result-button:hover {
    transform: translateY(-2px);
}
</style>
