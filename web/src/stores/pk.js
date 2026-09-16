import { ref } from 'vue'
import { defineStore } from 'pinia'

export const usePkStore = defineStore('pk', () => {
    const socket = ref(null);
    const status = ref('matching'); // 'matching' or 'playing'
    const opponent_username = ref('???');
    const opponent_photo = ref('https://api.dicebear.com/10.x/bottts/svg?seed=opponent_photo');
    const gamemap = ref(null);

    const updateSocket = (newSocket) => {
        socket.value = newSocket;
    }

    const updateOpponent = (username, photo) => {
        opponent_username.value = username;
        opponent_photo.value = photo;
    }

    const updateStatus = (newStatus) => {
        status.value = newStatus;
    }

    const updateGameMap = (newGamemap) => {
        gamemap.value = newGamemap;
    }


    return {
        socket,
        status,
        opponent_username,
        opponent_photo,
        gamemap,
        updateSocket,
        updateOpponent,
        updateStatus,
        updateGameMap
    }
})
