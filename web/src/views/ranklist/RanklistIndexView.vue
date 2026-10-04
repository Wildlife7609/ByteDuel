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
const loaded = ref(false) // 首次请求返回前不显示空状态，避免闪一下"暂无对局记录"

// 多页时最后一页不满，用一行"没有更多记录了"占满剩余高度，保持表格高度不变，分页条不会跳动
const ROW_HEIGHT = 57 // 真实行高：头像 40px + 上下内边距 16px + 边框 1px
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

// 1. 获取用户列表
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
        console.log("获取用户列表失败：", error);
    }
}

// 全局排名 = 前面几页的人数 + 本页序号
const rankOf = (index) => (currentPage.value - 1) * PAGE_SIZE + index + 1;

const totalGames = (user) => user.winCount + user.loseCount + user.drawCount;

// 平局计入总场数但不算胜；没打过对局时显示 "—"，避免除以 0
const winRate = (user) => {
    const total = totalGames(user);
    if (total === 0) return "—";
    return (user.winCount / total * 100).toFixed(1) + "%";
};

pull_page(currentPage.value);


</script>

<template>
    <ContentField>
        <!-- 空状态提示 -->
        <div v-if="loaded && totalUsers === 0" class="text-center py-5 text-muted">
            <p class="mb-2 fs-5">暂无用户记录</p>
            <p class="mb-0">平台上还没有任何用户，快去注册一个吧！</p>
        </div>

        <!-- User 列表表格 -->
        <table v-else-if="totalUsers > 0" class="table table-striped table-hover align-middle text-center">
            <thead>
                <tr>
                    <th scope="col" style="width: 18%;">排名</th>
                    <th scope="col" style="width: 27%;">
                        <div class="player-cell">玩家</div>
                    </th>
                    <th scope="col" style="width: 25%;">天梯分</th>
                    <th scope="col" style="width: 30%;">胜率</th>
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
                        <span class="text-muted small ms-1">（{{ totalGames(item) }} 局）</span>
                    </td>
                </tr>
                <tr v-if="emptyRows > 0" class="no-more-row">
                    <td colspan="4" class="text-muted" :style="{ height: emptyRows * ROW_HEIGHT + 'px' }">
                        没有更多记录了
                    </td>
                </tr>
            </tbody>
        </table>
        <nav v-if="totalPages > 1" aria-label="排行榜分页">
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
/* 固定宽度 + 左对齐，再整体居中：头像排成一列，不随用户名长短左右错开 */
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
</style>