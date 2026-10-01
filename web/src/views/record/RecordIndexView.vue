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
const loaded = ref(false) // 首次请求返回前不显示空状态，避免闪一下"暂无对局记录"

// 多页时最后一页不满，用隐藏行补齐，保持表格高度不变，分页条不会跳动
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
        a_sx: item.record.aSx,
        a_sy: item.record.aSy,
        b_sx: item.record.bSx,
        b_sy: item.record.bSy,
    })
    recordStore.updateSteps(item.record.aSteps, item.record.bSteps)
    recordStore.updateLoser(item.record.loser)
    router.push({ name: 'recordcontent', params: { recordId: item.record.id } });
}


// 1. 获取对局列表
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
        console.log("获取对局列表失败：", error);
    }
}

const resultText= (item, player) => {
    const loser = item.record.loser;
    if (loser === "all") return "平局";
    if (loser === player) return "失败";
    return "胜利";
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
        <!-- 空状态提示 -->
        <div v-if="loaded && totalRecords === 0" class="text-center py-5 text-muted">
            <p class="mb-2 fs-5">暂无对局记录</p>
            <p class="mb-0">平台上还没有任何对局，快去匹配一局吧！</p>
        </div>

        <!-- Record 列表表格 -->
        <table v-else-if="totalRecords > 0" class="table table-striped table-hover align-middle text-center">
            <thead>
                <tr>
                    <th scope="col" style="width: 25%;">玩家 A</th>
                    <th scope="col" style="width: 25%;">玩家 B</th>
                    <th scope="col" style="width: 25%;">对战时间</th>
                    <th scope="col" style="width: 25%;">操作</th>
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
                    <td>{{ item.record.createTime }}</td>
                    <td>
                        <button type="button" class="btn btn-secondary btn-sm me-2"
                            @click="open_record(item)">查看录像</button>
                    </td>
                </tr>
                <tr v-for="i in emptyRows" :key="'empty-' + i" class="placeholder-row">
                    <td colspan="4">&nbsp;</td>
                </tr>
            </tbody>
        </table>
        <nav v-if="totalPages > 1" aria-label="对局列表分页">
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
.placeholder-row {
    visibility: hidden; /* 不可见但仍占位 */
}

.placeholder-row td {
    height: 57px; /* 与真实行等高：头像 40px + 上下内边距 16px + 边框 1px */
}
</style>