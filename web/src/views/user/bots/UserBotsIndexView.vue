<script setup>
import { useUserStore } from '@/stores/user';
import { ref, computed } from 'vue';
import { Modal } from 'bootstrap/dist/js/bootstrap.bundle.min.js';
import axios from 'axios';
import BotCodeEditor from '@/components/BotCodeEditor.vue';
import { LANGUAGES, DEFAULT_LANGUAGE, BOT_TEMPLATES } from '@/utils/botTemplate';

const userStore = useUserStore();
const bots = ref([]);

// 1. Load the bot list
const refresh_bots = async () => {
    try {
        const res = await axios.get("/user/bot/getlist/");
        bots.value = res.data;
    } catch (error) {
        console.log("Failed to load bots:", error);
    }
}
refresh_bots();

// 2. Bot modal form and state, shared by create and update
const botForm = ref({ bot_id: null, title: "", description: "", content: "", language: DEFAULT_LANGUAGE });
const languageLabel = (value) => LANGUAGES.find(lang => lang.value === value)?.label ?? value;
const isUpdating = ref(false);
const errorMessage = ref("");
const isSubmitting = ref(false);


// open the create modal
const openCreateModal = () => {
    isUpdating.value = false;
    errorMessage.value = "";
    botForm.value = {
        bot_id: null,
        title: "",
        description: "",
        content: BOT_TEMPLATES[DEFAULT_LANGUAGE], // new bots start from the template
        language: DEFAULT_LANGUAGE,
    };
    Modal.getOrCreateInstance("#bot-modal").show();
}

// open the update modal
const openUpdateModal = (bot) => {
    isUpdating.value = true;
    errorMessage.value = "";
    botForm.value = {
        bot_id: bot.id,
        title: bot.title,
        description: bot.description,
        content: bot.content,
        language: bot.language,
    };
    Modal.getOrCreateInstance("#bot-modal").show();
}

// submit for both create and update
const submitBotForm = async () => {
    errorMessage.value = "";

    // basic client-side validation
    if (!botForm.value.title.trim()) {
        errorMessage.value = "Bot name cannot be empty";
        return;
    }
    if (botForm.value.title.length > 100) {
        errorMessage.value = "Bot name cannot exceed 100 characters";
        return;
    }
    if (botForm.value.description && botForm.value.description.length > 300) {
        errorMessage.value = "Bot description cannot exceed 300 characters";
        return;
    }
    if (!botForm.value.content.trim()) {
        errorMessage.value = "Bot code cannot be empty";
        return;
    }
    if (botForm.value.content.length > 10000) {
        errorMessage.value = "Bot code cannot exceed 10,000 characters";
        return;
    }

    isSubmitting.value = true;
    try {
        const url = isUpdating.value ? "/user/bot/update/" : "/user/bot/add/";
        const payload = isUpdating.value
            ? { ...botForm.value }
            : {
                title: botForm.value.title,
                description: botForm.value.description,
                content: botForm.value.content,
                language: botForm.value.language,
            };

        const res = await axios.post(url, payload);
        if (res.data.error_message === "success") {
            Modal.getOrCreateInstance("#bot-modal").hide();
            refresh_bots();
        } else {
            errorMessage.value = res.data.error_message;
        }
    } catch (error) {
        if (error.response && error.response.status === 429) {
            errorMessage.value = "Too many requests. Please try again later."; // rate limited by Cloudflare
        } else {
            errorMessage.value = "Request failed. Please try again later.";
        }
    } finally {
        isSubmitting.value = false;
    }
}

// 3. Delete a bot
const remove_bot = async (bot) => {
    if (!confirm(`Delete bot "${bot.title}"?`)) return;
    try {
        const res = await axios.post("/user/bot/remove/", { bot_id: bot.id });
        if (res.data.error_message === "success") {
            refresh_bots();
        } else {
            alert(res.data.error_message);
        }
    } catch (error) {
        console.error("Failed to delete bot:", error);
    }
}
</script>

<template>
    <!-- Shared bot modal #bot-modal -->
    <div class="modal fade" id="bot-modal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1">
        <div class="modal-dialog modal-xl">
            <div class="modal-content">
                <div class="modal-header">
                    <h1 class="modal-title fs-5">{{ isUpdating ? "Update Bot" : "Create Bot" }}</h1>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <label for="bot-title" class="form-label mb-0">Name</label>
                                <span class="small"
                                    :class="botForm.title.length > 100 ? 'text-danger fw-bold' : 'text-muted'">
                                    {{ botForm.title.length }} / 100
                                </span>
                            </div>
                            <input v-model="botForm.title" type="text" class="form-control"
                                :class="{ 'is-invalid': botForm.title.length > 100 }" id="bot-title"
                                placeholder="Name your bot">
                        </div>
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <label for="bot-description" class="form-label mb-0">Description</label>
                                <span class="small"
                                    :class="(botForm.description.length) > 300 ? 'text-danger fw-bold' : 'text-muted'">
                                    {{ botForm.description.length }} / 300
                                </span>
                            </div>
                            <textarea v-model="botForm.description" class="form-control" rows="4"
                                :class="{ 'is-invalid': botForm.description.length > 300 }" id="bot-description"
                                placeholder="Describe your bot (optional)"></textarea>
                        </div>
                        <BotCodeEditor v-model="botForm.content" v-model:language="botForm.language" />
                    </form>
                </div>
                <div class="modal-footer">
                    <div class="error-message me-auto">{{ errorMessage }}</div>
                    <button type="button" class="btn btn-primary" :disabled="isSubmitting" @click="submitBotForm">
                        <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-1" role="status"></span>
                        {{ isUpdating ? "Update" : "Create" }}
                    </button>
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                </div>
            </div>
        </div>
    </div>

    <div class="container">
        <div class="row">
            <div class="col-12 col-md-3">
                <div class="card profile-card" style="margin-top: 20px;">
                    <img :src="userStore.photo" class="card-img-top" alt="">
                    <div class="card-body">
                        <h5 class="card-title">{{ userStore.username }}</h5>
                        <p class="card-text text-muted">{{ bots.length }} / 10 bots created</p>
                    </div>
                </div>
            </div>

            <div class="col-12 col-md-9">
                <div class="card" style="margin-top: 20px;">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <span style="font-size: 22px; font-weight: 600;">My Bots</span>
                        <button type="button" class="btn btn-primary" :disabled="bots.length >= 10"
                            @click="openCreateModal">
                            {{ bots.length >= 10 ? 'Limit reached (10)' : 'Create Bot' }}
                        </button>
                    </div>
                    <div class="card-body">
                        <!-- Empty state -->
                        <div v-if="bots.length === 0" class="text-center py-5 text-muted">
                            <p class="mb-2 fs-5">No bots yet</p>
                            <p class="small">Click "Create Bot" at the top right to write your first bot!</p>
                        </div>

                        <!-- Bot table -->
                        <table v-else class="table table-striped table-hover align-middle">
                            <thead>
                                <tr>
                                    <th scope="col" style="width: 25%;">Name</th>
                                    <th scope="col" style="width: 35%;">Description</th>
                                    <th scope="col" class="d-none d-md-table-cell" style="width: 20%;">Created</th>
                                    <th scope="col" style="width: 20%;">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="bot in bots" :key="bot.id">
                                    <td class="text-break">
                                        {{ bot.title }}
                                        <span class="badge text-bg-light border ms-1">{{ languageLabel(bot.language) }}</span>
                                    </td>
                                    <td class="text-break text-muted">{{ bot.description }}</td>
                                    <td class="d-none d-md-table-cell">{{ bot.createTime }}</td>
                                    <td>
                                        <button type="button" class="btn btn-secondary btn-sm me-2 mb-1 mb-md-0"
                                            @click="openUpdateModal(bot)">Edit</button>
                                        <button type="button" class="btn btn-danger btn-sm"
                                            @click="remove_bot(bot)">Delete</button>
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
.profile-card .card-body {
    text-align: center;
}

/* phones: a compact row with a small avatar instead of a full-width image */
@media (max-width: 767.98px) {
    .profile-card {
        flex-direction: row;
        align-items: center;
    }

    .profile-card .card-img-top {
        width: 72px;
        flex-shrink: 0;
    }

    .profile-card .card-body {
        text-align: start;
        min-width: 0;
    }
}

div.error-message {
    color: red;
    font-size: 14px;
    white-space: pre-wrap; /* compile errors span several lines; keep line breaks and indentation */
    max-height: 200px;
    overflow-y: auto;
}
</style>