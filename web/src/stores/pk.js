import { ref } from 'vue'
import { defineStore } from 'pinia'

export const usePkStore = defineStore('pk', () => {
    const socket = ref(null);
    const status = ref('matching'); // 'matching' or 'playing'
    const selectedBotId = ref("-1");
    const opponentUsername = ref('???');
    const opponentPhoto = ref('https://api.dicebear.com/10.x/bottts/svg?seed=opponent_photo');
    const opponentSelectedBotId = ref("-1");
    const gameMap = ref([]);

    const updateSocket = (newSocket) => {
        socket.value = newSocket;
    }

    const initOpponent = () => {
        opponentUsername.value = '???';
        opponentPhoto.value = 'https://api.dicebear.com/10.x/bottts/svg?seed=opponent_photo';
        opponentSelectedBotId.value = "-1";
        gameMap.value = [];
    }

    const updateGameMap = (map) => {
        gameMap.value = map;
    }

    const updateOpponent = (username, photo) => {
        opponentUsername.value = username;
        opponentPhoto.value = photo;
    }

    const updateStatus = (newStatus) => {
        status.value = newStatus;
    }

    return {
        socket,
        status,
        selectedBotId,
        opponentUsername,
        opponentPhoto,
        opponentSelectedBotId,
        gameMap,
        updateSocket,
        updateOpponent,
        updateStatus,
        initOpponent,
        updateGameMap,
    }
})
