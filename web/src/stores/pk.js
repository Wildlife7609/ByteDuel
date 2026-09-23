import { ref } from 'vue'
import { defineStore } from 'pinia'

export const usePkStore = defineStore('pk', () => {
    const socket = ref(null);
    const status = ref('matching'); // 'matching' or 'playing'
    const selectedBotId = ref("-1");
    const opponentUsername = ref('???');
    const opponentPhoto = ref('https://api.dicebear.com/10.x/bottts/svg?seed=opponent_photo');
    const opponentSelectedBotId = ref("-1");
    const gameObject = ref(null);
    const gameMap = ref(null);
    const loser = ref("none"); // "all", "A", "B", "none"

    const updateSocket = (newSocket) => {
        socket.value = newSocket;
    }

    const initOpponent = () => {
        opponentUsername.value = '???';
        opponentPhoto.value = 'https://api.dicebear.com/10.x/bottts/svg?seed=opponent_photo';
        opponentSelectedBotId.value = "-1";
        gameObject.value = null;
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
        updateSocket,
        updateOpponent,
        updateStatus,
        initOpponent,
        updateGameObject,
        updateGameMap,
    }
})
