import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useRecordStore = defineStore('record', () => {
    const is_record = ref(false);
    const a_steps = ref("");
    const b_steps = ref("");
    const loser = ref(null);
    const is_finished = ref(false);

    const updateIsRecord = (newIsRecord) => {
        is_record.value = newIsRecord;
    }

    const updateSteps = (newASteps, newBSteps) => {
        a_steps.value = newASteps;
        b_steps.value = newBSteps;
    }

    const updateLoser = (newLoser) => {
        loser.value = newLoser;
    }

    const updateIsFinished = (newIsFinished) => {
        is_finished.value = newIsFinished;
    }

    return {
        is_record,
        a_steps,
        b_steps,
        loser,
        is_finished,
        updateIsRecord,
        updateSteps,
        updateLoser,
        updateIsFinished
    }
})