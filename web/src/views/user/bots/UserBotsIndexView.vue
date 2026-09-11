<script setup>
import { useUserStore } from '@/stores/user';
import { ref } from 'vue';
import { Modal } from 'bootstrap/dist/js/bootstrap.bundle.min.js';
import axios from 'axios';
import { VAceEditor } from 'vue3-ace-editor';
import ace from 'ace-builds';

// 引入支持的语言模式（如 C++、Java、Python）
import 'ace-builds/src-noconflict/mode-c_cpp';
import 'ace-builds/src-noconflict/mode-java';
import 'ace-builds/src-noconflict/mode-python';
import 'ace-builds/src-noconflict/mode-golang';
// 引入主题（浅色 + 深色）
import 'ace-builds/src-noconflict/theme-textmate';
import 'ace-builds/src-noconflict/theme-chrome';
import 'ace-builds/src-noconflict/theme-monokai';
import 'ace-builds/src-noconflict/theme-dracula';
import 'ace-builds/src-noconflict/ext-language_tools';
// 配置 CDN 资源路径（避免 Vite 打包找不到静态文件）
ace.config.set(
    "basePath",
    "https://cdn.jsdelivr.net/npm/ace-builds@" + ace.version + "/src-noconflict/"
);

// 默认选 C++ 和 浅色主题
const editorLang = ref("c_cpp");
const editorTheme = ref("textmate");

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

// 2. 创建 Bot
const addBotForm = ref({ title: "", description: "", content: "" });
const addErrorMessage = ref("");

const add_bot = async () => {
    addErrorMessage.value = "";
    try {
        const res = await axios.post("/user/bot/add/", addBotForm.value);
        if (res.data.error_message === "success") {
            addBotForm.value = { title: "", description: "", content: "" };
            Modal.getInstance("#add-bot-modal")?.hide();
            refresh_bots();
        } else {
            addErrorMessage.value = res.data.error_message;
        }
    } catch (error) {
        addErrorMessage.value = "请求失败，请稍后重试";
    }
}

// 3. 更新 Bot
const updateBotForm = ref({ bot_id: -1, title: "", description: "", content: "" });
const updateErrorMessage = ref("");

const openUpdateModal = (bot) => {
    updateErrorMessage.value = "";
    updateBotForm.value = {
        bot_id: bot.id,
        title: bot.title,
        description: bot.description,
        content: bot.content,
    };
}

const update_bot = async () => {
    updateErrorMessage.value = "";
    try {
        const res = await axios.post("/user/bot/update/", updateBotForm.value);
        if (res.data.error_message === "success") {
            Modal.getInstance("#update-bot-modal")?.hide();
            refresh_bots();
        } else {
            updateErrorMessage.value = res.data.error_message;
        }
    } catch (error) {
        updateErrorMessage.value = "请求失败，请稍后重试";
    }
}

// 4. 删除 Bot
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
    <!-- #add-bot-modal -->
    <div class="modal fade" id="add-bot-modal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1">
        <div class="modal-dialog modal-xl">
            <div class="modal-content">
                <div class="modal-header">
                    <h1 class="modal-title fs-5">创建Bot</h1>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="mb-3">
                            <label for="add-bot-title" class="form-label">Bot名称</label>
                            <input v-model="addBotForm.title" type="text" class="form-control" id="add-bot-title"
                                placeholder="请输入Bot名称">
                        </div>
                        <div class="mb-3">
                            <label for="add-bot-description" class="form-label">Bot描述</label>
                            <textarea v-model="addBotForm.description" class="form-control" rows="5"
                                id="add-bot-description" placeholder="请输入Bot描述"></textarea>
                        </div>
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <label class="form-label mb-0">Bot代码</label>
                                <div class="d-flex gap-2">
                                    <!-- 语言切换 -->
                                    <select v-model="editorLang" class="form-select form-select-sm"
                                        style="width: 110px;">
                                        <option value="c_cpp">C++</option>
                                        <option value="java">Java</option>
                                        <option value="python">Python</option>
                                        <option value="golang">Go</option>
                                    </select>
                                    <!-- 主题切换 -->
                                    <select v-model="editorTheme" class="form-select form-select-sm"
                                        style="width: 140px;">
                                        <option value="textmate">浅色 (Textmate)</option>
                                        <option value="chrome">浅色 (Chrome)</option>
                                        <option value="monokai">深色 (Monokai)</option>
                                        <option value="dracula">深色 (Dracula)</option>
                                    </select>
                                </div>
                            </div>
                            <VAceEditor v-model:value="addBotForm.content" :lang="editorLang" :theme="editorTheme"
                                style="height: 300px" :options="{
                                    enableBasicAutocompletion: true,
                                    enableLiveAutocompletion: true,
                                    fontSize: 16,
                                    tabSize: 4,
                                    showPrintMargin: false,
                                    highlightActiveLine: true,
                                    highlightSelectedWord: true,
                                    wrap: true,
                                    scrollPastEnd: 0.5
                                }" />

                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <div class="error-message">{{ addErrorMessage }}</div>
                    <button type="button" class="btn btn-primary" @click="add_bot">创建</button>
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>

                </div>
            </div>
        </div>
    </div>

    <!-- #update-bot-modal -->
    <div class="modal fade" id="update-bot-modal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1">
        <div class="modal-dialog modal-xl">
            <div class="modal-content">
                <div class="modal-header">
                    <h1 class="modal-title fs-5">更新Bot</h1>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="mb-3">
                            <label for="update-bot-title" class="form-label">Bot名称</label>
                            <input v-model="updateBotForm.title" type="text" class="form-control" id="update-bot-title"
                                placeholder="请输入Bot名称">
                        </div>
                        <div class="mb-3">
                            <label for="update-bot-description" class="form-label">Bot描述</label>
                            <textarea v-model="updateBotForm.description" class="form-control" rows="5"
                                id="update-bot-description" placeholder="请输入Bot描述"></textarea>
                        </div>
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <label class="form-label mb-0">Bot代码</label>
                                <div class="d-flex gap-2">
                                    <!-- 语言切换 -->
                                    <select v-model="editorLang" class="form-select form-select-sm"
                                        style="width: 110px;">
                                        <option value="c_cpp">C++</option>
                                        <option value="java">Java</option>
                                        <option value="python">Python</option>
                                        <option value="golang">Go</option>
                                    </select>
                                    <!-- 主题切换 -->
                                    <select v-model="editorTheme" class="form-select form-select-sm"
                                        style="width: 140px;">
                                        <option value="textmate">浅色 (Textmate)</option>
                                        <option value="chrome">浅色 (Chrome)</option>
                                        <option value="monokai">深色 (Monokai)</option>
                                        <option value="dracula">深色 (Dracula)</option>
                                    </select>
                                </div>
                            </div>
                            <VAceEditor v-model:value="updateBotForm.content" :lang="editorLang" :theme="editorTheme"
                                style="height: 300px" :options="{
                                    enableBasicAutocompletion: true,
                                    enableLiveAutocompletion: true,
                                    fontSize: 16,
                                    tabSize: 4,
                                    showPrintMargin: false,
                                    highlightActiveLine: true,
                                    highlightSelectedWord: true,
                                    wrap: true,
                                    scrollPastEnd: 0.5
                                }" />
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <div class="error-message">{{ updateErrorMessage }}</div>
                    <button type="button" class="btn btn-primary" @click="update_bot">更新</button>
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
                        <p class="card-text text-muted">已创建 {{ bots.length }} 个 Bot</p>
                    </div>
                </div>
            </div>

            <div class="col-9">
                <div class="card" style="margin-top: 20px;">
                    <div class="card-header">
                        <span style="font-size: 25px;">我的Bot</span>
                        <button type="button" class="btn btn-primary float-end" data-bs-toggle="modal"
                            data-bs-target="#add-bot-modal">创建Bot</button>


                    </div>
                    <div class="card-body">
                        <table class="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th scope="col">名称</th>
                                    <th scope="col">描述</th>
                                    <th scope="col">创建时间</th>
                                    <th scope="col">操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="bot in bots" :key="bot.id">
                                    <td>{{ bot.title }}</td>
                                    <td>{{ bot.description }}</td>
                                    <td>{{ bot.createTime }}</td>
                                    <td>
                                        <button type="button" class="btn btn-secondary" style="margin-right: 10px;"
                                            data-bs-toggle="modal" data-bs-target="#update-bot-modal"
                                            @click="openUpdateModal(bot)">更新</button>
                                        <button type="button" class="btn btn-danger"
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
}
</style>