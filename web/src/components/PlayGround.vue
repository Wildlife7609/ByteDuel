<script setup>
import GameMap from "@/components/GameMap.vue";
import MoveControls from "@/components/MoveControls.vue";
import { usePkStore } from "@/stores/pk";
import { useRecordStore } from "@/stores/record";

const pkStore = usePkStore();
const recordStore = useRecordStore();
</script>

<template>
    <div class="playground">
        <GameMap />
    </div>
    <!-- only in a live game played by hand, not in replays or when a bot is playing -->
    <MoveControls v-if="!recordStore.is_record && pkStore.selectedBotId === -1" />
</template>

<style scoped>
div.playground {
    width: 70vw;
    height: 70vh;
    margin: 4rem auto;
}

@media (max-width: 767.98px) {
    div.playground {
        width: calc(100vw - 32px);
        height: auto;
        aspect-ratio: 14 / 15; /* the board is 14 columns by 15 rows */
        margin: 1.5rem auto;
    }
}
</style>
