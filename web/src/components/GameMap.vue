<script setup>
import { ref, onMounted } from "vue";
import { GameMap } from "@/assets/scripts/GameMap";
let parent = ref(null);
let canvas = ref(null);

onMounted(() => {
    new GameMap(canvas.value.getContext("2d"), parent.value);
})
</script>

<template>
    <div ref="parent" class="gamemap">
        <canvas ref="canvas"></canvas>
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