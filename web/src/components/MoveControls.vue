<script setup>
import { usePkStore } from '@/stores/pk';

const pkStore = usePkStore();

// direction matches the keyboard and the backend: 0 up, 1 right, 2 down, 3 left
const buttons = [
    { direction: 0, label: "Up", area: "up", rotate: 0 },
    { direction: 3, label: "Left", area: "left", rotate: 270 },
    { direction: 1, label: "Right", area: "right", rotate: 90 },
    { direction: 2, label: "Down", area: "down", rotate: 180 },
];
</script>

<template>
    <!-- On-screen arrows for touch devices; the CSS hides them where a keyboard is likely -->
    <div class="move-controls">
        <!-- pointerdown fires on touch without waiting for the finger to lift, which a 5-second turn needs -->
        <button v-for="b in buttons" :key="b.direction" type="button" class="move-btn"
            :style="{ gridArea: b.area }" :aria-label="b.label" @pointerdown="pkStore.sendMove(b.direction)">
            <svg viewBox="0 0 24 24" width="28" height="28" :style="{ transform: `rotate(${b.rotate}deg)` }">
                <path d="M6 15l6-6 6 6" fill="none" stroke="currentColor" stroke-width="3"
                    stroke-linecap="round" stroke-linejoin="round" />
            </svg>
        </button>
    </div>
</template>

<style scoped>
.move-controls {
    display: none;
    grid-template-columns: repeat(3, 64px);
    grid-template-rows: repeat(3, 64px);
    grid-template-areas:
        ". up ."
        "left . right"
        ". down .";
    gap: 6px;
    justify-content: center;
    margin: 0 auto 2rem;
}

/* phones and tablets: no hover and a coarse pointer means touch */
@media (hover: none) and (pointer: coarse) {
    .move-controls {
        display: grid;
    }
}

.move-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 16px;
    color: #ffffff;
    /* same dark frosted-glass style as MatchGround */
    background: rgba(30, 41, 59, 0.7);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.15);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.4);
    touch-action: manipulation; /* no double-tap zoom */
    user-select: none;
    -webkit-user-select: none;
    -webkit-tap-highlight-color: transparent;
}

.move-btn:active {
    background: #f59e0b;
    color: #1e293b;
}
</style>
