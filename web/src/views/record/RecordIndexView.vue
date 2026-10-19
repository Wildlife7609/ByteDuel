<script setup>
import ContentField from '@/components/ContentField.vue'
import { useRouter } from 'vue-router'
import { useRecordStore } from '@/stores/record'
import { usePkStore } from '@/stores/pk'
import { ref, computed } from 'vue'
import axios from 'axios'

const router = useRouter()
const recordStore = useRecordStore()
const pkStore = usePkStore()

const records = ref([])
const totalRecords = ref(0)
const currentPage = ref(1)
const PAGE_SIZE = 10
const totalPages = computed(() => Math.ceil(totalRecords.value / PAGE_SIZE))
const loaded = ref(false) // Hide the empty state until the first response arrives, so "No matches yet" does not flash briefly

// With several pages, fill the short last page with a "No more records" row so the table keeps its height and the pagination bar does not jump
const ROW_HEIGHT = 57 // actual row height: 40px avatar + 16px vertical padding + 1px border
const emptyRows = computed(() => totalPages.value > 1 ? PAGE_SIZE - records.value.length : 0)

const pages = computed(() => {
    let st = Math.max(1, currentPage.value - 2);
    let ed = Math.min(totalPages.value, st + 4);
    st = Math.max(1, ed - 4);
    const res = [];
    for (let i = st; i <= ed; i ++) res.push(i);
    return res;
});

const go_page = (page) => {
    if (page < 1 || page > totalPages.value || page === currentPage.value) return;
    pull_page(page);
};

const stringTo2D = (mapString) => {
    const g = []
    for (let i = 0; i < 15; i++) {
        const row = []
        for (let j = 0; j < 14; j++) {
            row.push(mapString.charAt(i * 14 + j) - '0')
        }
        g.push(row)
    }
    return g;
}


const open_record = (item) => {
    recordStore.updateIsRecord(true)
    pkStore.updateGameObject({
        map: stringTo2D(item.record.map),
        a_id: item.record.aId,
        b_id: item.record.bId,
        a_username: item.a_username,
        b_username: item.b_username,
        a_sx: item.record.aSx,
        a_sy: item.record.aSy,
        b_sx: item.record.bSx,
        b_sy: item.record.bSy,
    })
    recordStore.updateSteps(item.record.aSteps, item.record.bSteps)
    recordStore.updateLoser(item.record.loser)
    router.push({ name: 'recordcontent', params: { recordId: item.record.id } });
}


// 1. Load the match list
const pull_page = async (page) => {
    currentPage.value = page;
    try {
        const res = await axios.get("/record/getlist/", {
            params: { page }
        });
        records.value = res.data.records;
        totalRecords.value = res.data.total;
        loaded.value = true;
    } catch (error) {
        console.log("Failed to load matches:", error);
    }
}

const resultText= (item, player) => {
    const loser = item.record.loser;
    if (loser === "all") return "Draw";
    if (loser === player) return "Loss";
    return "Win";
}

const resultClass = (item, player) => {
    const loser = item.record.loser;
    if (loser === "all") return "bg-secondary";
    if (loser === player) return "bg-danger";
    return "bg-success";
}

pull_page(currentPage.value);


</script>

<template>
    <ContentField>
        <!-- Empty state -->
        <div v-if="loaded && totalRecords === 0" class="text-center py-5 text-muted">
            <p class="mb-2 fs-5">No matches yet</p>
            <p class="mb-0">No one has played yet. Go find a match!</p>
        </div>

        <!-- Record table -->
        <div v-else-if="totalRecords > 0" class="table-responsive">
        <table class="table table-striped table-hover align-middle text-center">
            <thead>
                <tr>
                    <th scope="col" style="width: 25%;">Player A</th>
                    <th scope="col" style="width: 25%;">Player B</th>
                    <th scope="col" class="d-none d-md-table-cell" style="width: 25%;">Played at</th>
                    <th scope="col" style="width: 25%;">Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="item in records" :key="item.record.id">
                    <td>
                        <img :src="item.a_photo" alt="" style="width: 40px; height: 40px; border-radius: 50%; margin-right: 10px;">
                        {{ item.a_username }}
                        <span class="badge ms-2" :class="resultClass(item, 'A')">
                            {{ resultText(item, 'A') }}
                        </span>

                    </td>
                    <td>
                        <img :src="item.b_photo" alt="" style="width: 40px; height: 40px; border-radius: 50%; margin-right: 10px;">
                        {{ item.b_username }}
                        <span class="badge ms-2" :class="resultClass(item, 'B')">
                            {{ resultText(item, 'B') }}
                        </span>
                    </td>
                    <td class="d-none d-md-table-cell">{{ item.record.createTime }}</td>
                    <td>
                        <button type="button" class="btn btn-secondary btn-sm"
                            @click="open_record(item)">Watch replay</button>
                        <!-- on phones the time moves here instead of its own column -->
                        <div class="d-md-none text-muted small mt-1">{{ item.record.createTime }}</div>
                    </td>
                </tr>
                <tr v-if="emptyRows > 0" class="no-more-row">
                    <td colspan="4" class="text-muted" :style="{ height: emptyRows * ROW_HEIGHT + 'px' }">
                        No more records
                    </td>
                </tr>
            </tbody>
        </table>
        </div>
        <nav v-if="totalPages > 1" aria-label="Match list pages">
            <ul class="pagination" style="justify-content: center; margin-top: 30px;">
                <li class="page-item" :class="{ disabled: currentPage === 1}">
                <a class="page-link" href="#" aria-label="Previous" @click.prevent="go_page(currentPage - 1)">

                    <span aria-hidden="true">&laquo;</span>
                </a>
                
                </li>
                <li v-for="page in pages" :key="page" class="page-item" :class="{ active: page === currentPage }">
                    <a class="page-link" href="#" @click.prevent="go_page(page)">{{ page }}</a>
                </li>
                <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                    <a class="page-link" href="#" aria-label="Next" @click.prevent="go_page(currentPage + 1)">
                        <span aria-hidden="true">&raquo;</span>
                    </a>
                </li>
            </ul>
        </nav>
    </ContentField>
</template>

<style scoped>
</style>