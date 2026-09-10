<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { GameMap } from "@/assets/scripts/GameMap";
let parent = ref(null);
let canvas = ref(null);
let gamemap = null;
onMounted(() => {
    gamemap = new GameMap(canvas.value.getContext("2d"), parent.value);
})
onUnmounted(() => {
    gamemap.destroy();
})
</script>

<template>
    <div ref="parent" class="gamemap">
        <canvas ref="canvas" tabindex="0"></canvas>
    </div>
</template>

<style scoped>
.gamemap {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
}
</style>