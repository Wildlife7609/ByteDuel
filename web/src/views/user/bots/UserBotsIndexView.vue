<script setup>
import { useUserStore } from '@/stores/user';
import { ref, computed } from 'vue';
import { Modal } from 'bootstrap/dist/js/bootstrap.bundle.min.js';
import axios from 'axios';
import BotCodeEditor from '@/components/BotCodeEditor.vue';

const userStore = useUserStore();
const bots = ref([]);

// 1. 获取 Bot 列表
const refresh_bots = async () => {
    try {
        const res = await axios.get("/user/bot/getlist/");
        bots.value = res.data;
    } catch (error) {
        console.log("获取Bot列表失败：", error);
    }
}
refresh_bots();

// 2. 统一 Bot 模态框表单与状态（创建与更新公用）
const botForm = ref({ bot_id: null, title: "", description: "", content: "" });
const isUpdating = ref(false);
const errorMessage = ref("");
const isSubmitting = ref(false);


// 打开创建弹窗
const openCreateModal = () => {
    isUpdating.value = false;
    errorMessage.value = "";
    botForm.value = {
        bot_id: null,
        title: "",
        description: "",
        content: "",
    };
    Modal.getOrCreateInstance("#bot-modal").show();
}

// 打开更新弹窗
const openUpdateModal = (bot) => {
    isUpdating.value = true;
    errorMessage.value = "";
    botForm.value = {
        bot_id: bot.id,
        title: bot.title,
        description: bot.description,
        content: bot.content,
    };
    Modal.getOrCreateInstance("#bot-modal").show();
}

// 统一提交处理
const submitBotForm = async () => {
    errorMessage.value = "";

    // 前端基础校验
    if (!botForm.value.title.trim()) {
        errorMessage.value = "Bot名称不能为空";
        return;
    }
    if (botForm.value.title.length > 100) {
        errorMessage.value = "Bot名称长度不能大于100";
        return;
    }
    if (botForm.value.description && botForm.value.description.length > 300) {
        errorMessage.value = "Bot描述长度不能大于300";
        return;
    }
    if (!botForm.value.content.trim()) {
        errorMessage.value = "Bot代码不能为空";
        return;
    }
    if (botForm.value.content.length > 10000) {
        errorMessage.value = "Bot代码长度不能大于10000";
        return;
    }

    isSubmitting.value = true;
    try {
        const url = isUpdating.value ? "/user/bot/update/" : "/user/bot/add/";
        const payload = isUpdating.value
            ? { ...botForm.value }
            : { title: botForm.value.title, description: botForm.value.description, content: botForm.value.content };

        const res = await axios.post(url, payload);
        if (res.data.error_message === "success") {
            Modal.getOrCreateInstance("#bot-modal").hide();
            refresh_bots();
        } else {
            errorMessage.value = res.data.error_message;
        }
    } catch (error) {
        errorMessage.value = "请求失败，请稍后重试";
    } finally {
        isSubmitting.value = false;
    }
}

// 3. 删除 Bot
const remove_bot = async (bot) => {
    if (!confirm(`确定要删除 Bot "${bot.title}" 吗？`)) return;
    try {
        const res = await axios.post("/user/bot/remove/", { bot_id: bot.id });
        if (res.data.error_message === "success") {
            refresh_bots();
        } else {
            alert(res.data.error_message);
        }
    } catch (error) {
        console.error("删除Bot失败：", error);
    }
}
</script>

<template>
    <!-- 统一 Bot 模态框 #bot-modal -->
    <div class="modal fade" id="bot-modal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1">
        <div class="modal-dialog modal-xl">
            <div class="modal-content">
                <div class="modal-header">
                    <h1 class="modal-title fs-5">{{ isUpdating ? "更新Bot" : "创建Bot" }}</h1>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <label for="bot-title" class="form-label mb-0">Bot名称</label>
                                <span class="small"
                                    :class="botForm.title.length > 100 ? 'text-danger fw-bold' : 'text-muted'">
                                    {{ botForm.title.length }} / 100
                                </span>
                            </div>
                            <input v-model="botForm.title" type="text" class="form-control"
                                :class="{ 'is-invalid': botForm.title.length > 100 }" id="bot-title"
                                placeholder="请输入Bot名称">
                        </div>
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <label for="bot-description" class="form-label mb-0">Bot描述</label>
                                <span class="small"
                                    :class="(botForm.description.length) > 300 ? 'text-danger fw-bold' : 'text-muted'">
                                    {{ botForm.description.length }} / 300
                                </span>
                            </div>
                            <textarea v-model="botForm.description" class="form-control" rows="4"
                                :class="{ 'is-invalid': botForm.description.length > 300 }" id="bot-description"
                                placeholder="请输入Bot描述（选填）"></textarea>
                        </div>
                        <BotCodeEditor v-model="botForm.content" />
                    </form>
                </div>
                <div class="modal-footer">
                    <div class="error-message me-auto">{{ errorMessage }}</div>
                    <button type="button" class="btn btn-primary" :disabled="isSubmitting" @click="submitBotForm">
                        <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-1" role="status"></span>
                        {{ isUpdating ? "更新" : "创建" }}
                    </button>
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                </div>
            </div>
        </div>
    </div>

    <div class="container">
        <div class="row">
            <div class="col-3">
                <div class="card" style="margin-top: 20px;">
                    <img :src="userStore.photo" class="card-img-top" alt="" style="width: 100%;">
                    <div class="card-body text-center">
                        <h5 class="card-title">{{ userStore.username }}</h5>
                        <p class="card-text text-muted">已创建 {{ bots.length }} / 10 个 Bot</p>
                    </div>
                </div>
            </div>

            <div class="col-9">
                <div class="card" style="margin-top: 20px;">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <span style="font-size: 22px; font-weight: 600;">我的Bot</span>
                        <button type="button" class="btn btn-primary" :disabled="bots.length >= 10"
                            @click="openCreateModal">
                            {{ bots.length >= 10 ? '已达上限(10个)' : '创建Bot' }}
                        </button>
                    </div>
                    <div class="card-body">
                        <!-- 空状态提示 -->
                        <div v-if="bots.length === 0" class="text-center py-5 text-muted">
                            <p class="mb-2 fs-5">暂无 Bot</p>
                            <p class="small">点击右上角“创建Bot”编写并上线你的第一个 AI 吧！</p>
                        </div>

                        <!-- Bot 列表表格 -->
                        <table v-else class="table table-striped table-hover align-middle">
                            <thead>
                                <tr>
                                    <th scope="col" style="width: 25%;">名称</th>
                                    <th scope="col" style="width: 35%;">描述</th>
                                    <th scope="col" style="width: 20%;">创建时间</th>
                                    <th scope="col" style="width: 20%;">操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="bot in bots" :key="bot.id">
                                    <td class="text-break">{{ bot.title }}</td>
                                    <td class="text-break text-muted">{{ bot.description }}</td>
                                    <td>{{ bot.createTime }}</td>
                                    <td>
                                        <button type="button" class="btn btn-secondary btn-sm me-2"
                                            @click="openUpdateModal(bot)">更新</button>
                                        <button type="button" class="btn btn-danger btn-sm"
                                            @click="remove_bot(bot)">删除</button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
div.error-message {
    color: red;
    font-size: 14px;
    white-space: pre-wrap; /* 编译错误是多行的，保留换行和缩进 */
    max-height: 200px;
    overflow-y: auto;
}
</style>