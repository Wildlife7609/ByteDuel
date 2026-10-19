import { ref, shallowRef } from 'vue'
import { defineStore } from 'pinia'

export const usePkStore = defineStore('pk', () => {
    const socket = ref(null);
    const status = ref('matching'); // 'matching' or 'playing'
    const selectedBotId = ref(-1);
    const opponentUsername = ref('???');
    const opponentPhoto = ref('https://api.dicebear.com/10.x/bottts/svg?seed=opponent_photo');
    const opponentSelectedBotId = ref(-1);
    const gameObject = ref(null);
    const gameMap = shallowRef(null);
    const loser = ref("none"); // "all", "A", "B", "none"

    const updateSocket = (newSocket) => {
        socket.value = newSocket;
    }

    const initOpponent = () => {
        opponentUsername.value = '???';
        opponentPhoto.value = 'https://api.dicebear.com/10.x/bottts/svg?seed=opponent_photo';
        opponentSelectedBotId.value = -1;
        gameObject.value = null;
        loser.value = "none";
    }

    const updateGameObject = (game) => {
        gameObject.value = game;
    }

    const updateGameMap = (newGameMap) => {
        gameMap.value = newGameMap;
    }

    const updateOpponent = (username, photo) => {
        opponentUsername.value = username;
        opponentPhoto.value = photo;
    }

    const updateStatus = (newStatus) => {
        status.value = newStatus;
    }

    const updateLoser = (newLoser) => {
        loser.value = newLoser;
    }

    // direction: 0 up, 1 right, 2 down, 3 left. Used by both the keyboard and the on-screen buttons
    const sendMove = (direction) => {
        socket.value?.send(JSON.stringify({
            event: "move",
            direction,
        }));
    }

    return {
        socket,
        status,
        selectedBotId,
        opponentUsername,
        opponentPhoto,
        opponentSelectedBotId,
        gameObject,
        gameMap,
        loser,
        updateLoser,
        sendMove,
        updateSocket,
        updateOpponent,
        updateStatus,
        initOpponent,
        updateGameObject,
        updateGameMap,
    }
})
