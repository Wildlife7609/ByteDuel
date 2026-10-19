<script setup>
import ContentField from '@/components/ContentField.vue'
import { useUserStore } from '@/stores/user'
import { ref, computed } from 'vue'
import axios from 'axios'

const userStore = useUserStore()

const users = ref([])
const totalUsers = ref(0)
const currentPage = ref(1)
const PAGE_SIZE = 3
const totalPages = computed(() => Math.ceil(totalUsers.value / PAGE_SIZE))
const loaded = ref(false) // Hide the empty state until the first response arrives, so it does not flash briefly

// With several pages, fill the short last page with a "No more records" row so the table keeps its height and the pagination bar does not jump
const ROW_HEIGHT = 57 // actual row height: 40px avatar + 16px vertical padding + 1px border
const emptyRows = computed(() => totalPages.value > 1 ? PAGE_SIZE - users.value.length : 0)

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

// 1. Load the user list
const pull_page = async (page) => {
    currentPage.value = page;
    try {
        const res = await axios.get("/ranklist/getlist/", {
            params: { page }
        });
        users.value = res.data.users;
        totalUsers.value = res.data.total;
        loaded.value = true;
    } catch (error) {
        console.log("Failed to load users:", error);
    }
}

// overall rank = users on earlier pages + position on this page
const rankOf = (index) => (currentPage.value - 1) * PAGE_SIZE + index + 1;

const totalGames = (user) => user.winCount + user.loseCount + user.drawCount;

// Draws count toward total games but not wins; show "—" when there are no games, to avoid dividing by 0
const winRate = (user) => {
    const total = totalGames(user);
    if (total === 0) return "—";
    return (user.winCount / total * 100).toFixed(1) + "%";
};

pull_page(currentPage.value);


</script>

<template>
    <ContentField>
        <!-- Empty state -->
        <div v-if="loaded && totalUsers === 0" class="text-center py-5 text-muted">
            <p class="mb-2 fs-5">No players yet</p>
            <p class="mb-0">Nobody has signed up yet. Be the first!</p>
        </div>

        <!-- User table -->
        <div v-else-if="totalUsers > 0" class="table-responsive">
        <table class="table table-striped table-hover align-middle text-center">
            <thead>
                <tr>
                    <th scope="col" style="width: 18%;">Rank</th>
                    <th scope="col" style="width: 27%;">
                        <div class="player-cell">Player</div>
                    </th>
                    <th scope="col" style="width: 25%;">Rating</th>
                    <th scope="col" style="width: 30%;">Win rate</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="(item, index) in users" :key="item.id"
                    :class="{ 'table-warning': String(item.id) === userStore.id }">
                    <td class="fw-bold">{{ rankOf(index) }}</td>
                    <td>
                        <div class="player-cell">
                            <img :src="item.photo" alt="" class="player-photo">
                            <span class="player-name">{{ item.username }}</span>
                        </div>
                    </td>
                    <td>{{ item.rating }}</td>
                    <td>
                        {{ winRate(item) }}
                        <span class="text-muted small text-nowrap ms-md-1 d-block d-md-inline">({{ totalGames(item) }} games)</span>
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
        <nav v-if="totalPages > 1" aria-label="Ranklist pages">
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
/* Fixed width, left-aligned, then centered as a block: avatars line up in a column regardless of username length */
.player-cell {
    display: flex;
    align-items: center;
    width: 200px;
    margin: 0 auto;
}

.player-photo {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    margin-right: 10px;
    flex-shrink: 0;
}

.player-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* phones: drop the fixed width so all four columns fit */
@media (max-width: 767.98px) {
    .player-cell {
        width: auto;
        max-width: 120px;
    }

    .player-photo {
        width: 32px;
        height: 32px;
        margin-right: 6px;
    }
}
</style>