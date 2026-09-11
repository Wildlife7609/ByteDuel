<script setup>
import { useUserStore } from '@/stores/user';
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { Modal } from 'bootstrap/dist/js/bootstrap.bundle.min.js';
import axios from 'axios';

const userStore = useUserStore();
const router = useRouter();
let bots = ref([]);

const refresh_bots = async () => {
    try {
        const res = await axios.get("/user/bot/getlist/");
        bots.value = res.data;
    } catch (error) {
        error_message.value = "服务器连接失败或登录已过期，请重试";
    }
}

refresh_bots();

let title = ref("");
let description = ref("");
let content = ref("");
let error_message = ref("");

const add_bot = async () => {
    error_message.value = "";
    try {
        const res = await axios.post("/user/bot/add/", {
            title: title.value,
            description: description.value,
            content: content.value,
        })

        if (res.data.error_message === "success") {
            title.value = "";
            description.value = "";
            content.value = "";
            Modal.getInstance("#add-bot-modal").hide();
            refresh_bots();
        } else {
            error_message.value = res.data.error_message;
        }
    } catch (error) {
        error_message.value = "服务器连接失败或登录已过期，请重试";
    }
}

const update_bot = async (bot) => {
    error_message.value = "";
    try {
        const res = await axios.post("/user/bot/update/", {
            bot_id: bot.id,
            title: bot.title,
            description: bot.description,
            content: bot.content,
        })

        if (res.data.error_message === "success") {
            Modal.getInstance("#update-bot-modal-" + bot.id).hide();
            refresh_bots();
        } else {
            error_message.value = res.data.error_message;
        }
    } catch (error) {
        console.error("更新Bot失败：", error);
        error_message.value = "服务器连接失败或登录已过期，请重试";
    }
}

const remove_bot = async (bot_id) => {
    try {
        const res = await axios.post("/user/bot/remove/", {
            bot_id: bot_id,
        })
        if (res.data.error_message === "success") {
            refresh_bots();
        }
    } catch (error) {
        error_message.value = "服务器连接失败或登录已过期，请重试";
    }
}


</script>

<template>
    <div class="container">
        <div class="row">
            <div class="col-3">
                <div class="card" style="margin-top: 20px;">
                    <img :src="userStore.photo" class="card-img-top" alt="" style="width: 100%;">
                    <div class="card-body">
                        <h5 class="card-title">Card title</h5>
                        <p class="card-text">Some quick example text to build on the card title and make up the bulk of
                            the card’s content.</p>
                        <a href="#" class="btn btn-primary">Go somewhere</a>
                    </div>
                </div>

            </div>
            <div class="col-9">
                <div class="card" style="margin-top: 20px;">
                    <div class="card-header">
                        <span style="font-size: 25px;">我的Bot</span>
                        <button type="button" class="btn btn-primary float-end" data-bs-toggle="modal"
                            data-bs-target="#add-bot-modal">创建Bot</button>

                        <!-- Modal -->
                        <div class="modal fade" id="add-bot-modal" data-bs-backdrop="static" data-bs-keyboard="false"
                            tabindex="-1">
                            <div class="modal-dialog modal-xl">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <h1 class="modal-title fs-5">创建Bot</h1>
                                        <button type="button" class="btn-close" data-bs-dismiss="modal"
                                            aria-label="Close"></button>
                                    </div>
                                    <div class="modal-body">
                                        <form>
                                            <div class="mb-3">
                                                <label for="bot-title" class="form-label">Bot名称</label>
                                                <input v-model="title" type="text" class="form-control" id="bot-title"
                                                    placeholder="请输入Bot名称">
                                            </div>
                                            <div class="mb-3">
                                                <label for="bot-description" class="form-label">Bot描述</label>
                                                <textarea v-model="description" class="form-control" rows="5"
                                                    id="bot-description" placeholder="请输入Bot描述"></textarea>
                                            </div>
                                            <div class="mb-3">
                                                <label for="bot-code" class="form-label">Bot代码</label>
                                                <textarea v-model="content" class="form-control" rows="10" id="bot-code"
                                                    placeholder="请编写Bot代码"></textarea>
                                            </div>
                                        </form>
                                    </div>
                                    <div class="modal-footer">
                                        <div class="error-message">{{ error_message }}</div>
                                        <button type="button" class="btn btn-primary" @click="add_bot">创建</button>
                                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"
                                            @click="refresh_bots">取消</button>

                                    </div>
                                </div>
                            </div>
                        </div>
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
                                            data-bs-toggle="modal"
                                            :data-bs-target="'#update-bot-modal-' + bot.id">编辑</button>
                                        <button type="button" class="btn btn-danger"
                                            @click="remove_bot(bot.id)">删除</button>

                                        <!-- Modal -->
                                        <div class="modal fade" :id="'update-bot-modal-' + bot.id"
                                            data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1">
                                            <div class="modal-dialog modal-xl">
                                                <div class="modal-content">
                                                    <div class="modal-header">
                                                        <h1 class="modal-title fs-5">更新Bot</h1>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal"
                                                            aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <form>
                                                            <div class="mb-3">
                                                                <label :for="'bot-title-' + bot.id"
                                                                    class="form-label">Bot名称</label>
                                                                <input v-model="bot.title" type="text"
                                                                    class="form-control" :id="'bot-title-' + bot.id"
                                                                    placeholder="请输入Bot名称">
                                                            </div>
                                                            <div class="mb-3">
                                                                <label :for="'bot-description-' + bot.id"
                                                                    class="form-label">Bot描述</label>
                                                                <textarea v-model="bot.description" class="form-control"
                                                                    rows="5" :id="'bot-description-' + bot.id"
                                                                    placeholder="请输入Bot描述"></textarea>
                                                            </div>
                                                            <div class="mb-3">
                                                                <label :for="'bot-code-' + bot.id"
                                                                    class="form-label">Bot代码</label>
                                                                <textarea v-model="bot.content" class="form-control"
                                                                    rows="10" :id="'bot-code-' + bot.id"
                                                                    placeholder="请编写Bot代码"></textarea>
                                                            </div>
                                                        </form>
                                                    </div>
                                                    <div class="modal-footer">
                                                        <div class="error-message">{{ error_message }}</div>
                                                        <button type="button" class="btn btn-primary"
                                                            @click="update_bot(bot)">更新</button>
                                                        <button type="button" class="btn btn-secondary"
                                                            data-bs-dismiss="modal" @click="refresh_bots">取消</button>

                                                    </div>
                                                </div>
                                            </div>
                                        </div>
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