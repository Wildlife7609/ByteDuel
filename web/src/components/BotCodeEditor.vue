<script setup>
import { ref, computed, useId } from 'vue';
import { VAceEditor } from 'vue3-ace-editor';
import ace from 'ace-builds';
import 'ace-builds/src-noconflict/mode-c_cpp';
import 'ace-builds/src-noconflict/mode-java';
import 'ace-builds/src-noconflict/mode-python';
import 'ace-builds/src-noconflict/mode-golang';
import 'ace-builds/src-noconflict/theme-textmate';
import 'ace-builds/src-noconflict/theme-chrome';
import 'ace-builds/src-noconflict/theme-monokai';
import 'ace-builds/src-noconflict/theme-dracula';
import 'ace-builds/src-noconflict/ext-language_tools';
import { LANGUAGES, DEFAULT_LANGUAGE, BOT_TEMPLATES } from '@/utils/botTemplate';
ace.config.set(
    "basePath",
    "https://cdn.jsdelivr.net/npm/ace-builds@" + ace.version + "/src-noconflict/"
);
// v-model binds the code, v-model:language binds the language
const content = defineModel({ default: '' });
const language = defineModel('language', { default: DEFAULT_LANGUAGE });
const wrapSwitchId = useId();
const completeSwitchId = useId();
const editorMode = computed(() => LANGUAGES.find(lang => lang.value === language.value)?.mode ?? "text");
const editorSettings = ref({
    theme: localStorage.getItem("bot_editor_theme") || "textmate",
    fontSize: Number(localStorage.getItem("bot_editor_font_size")) || 16,
    wrap: localStorage.getItem("bot_editor_wrap") !== "false",
    enableAutocompletion: localStorage.getItem("bot_editor_autocomplete") !== "false",
});
const saveSettings = () => {
    localStorage.setItem("bot_editor_theme", editorSettings.value.theme);
    localStorage.setItem("bot_editor_font_size", editorSettings.value.fontSize);
    localStorage.setItem("bot_editor_wrap", editorSettings.value.wrap);
    localStorage.setItem("bot_editor_autocomplete", editorSettings.value.enableAutocompletion);
};
const editorOptions = computed(() => ({
    enableBasicAutocompletion: editorSettings.value.enableAutocompletion,
    enableLiveAutocompletion: editorSettings.value.enableAutocompletion,
    fontSize: editorSettings.value.fontSize,
    wrap: editorSettings.value.wrap,
    showPrintMargin: false,
    highlightActiveLine: true,
    highlightSelectedWord: true,
    scrollPastEnd: 0.5,
}));
const resetCode = () => {
    const template = BOT_TEMPLATES[language.value];
    if (content.value === template) return;
    if (confirm("Reset the editor to the default template? Your current code will be lost.")) {
        content.value = template;
    }
};
// Switching language loads that language's template; if the code was edited, confirm first, and revert the dropdown on cancel
const changeLanguage = (event) => {
    const newLanguage = event.target.value;
    const untouched = !content.value.trim() || Object.values(BOT_TEMPLATES).includes(content.value);
    if (!untouched && !confirm("Switching language replaces your code with the template for the new language. Your current code will be lost. Continue?")) {
        event.target.value = language.value;
        return;
    }
    language.value = newLanguage;
    content.value = BOT_TEMPLATES[newLanguage];
};
</script>
<template>
    <div class="mb-3">
        <div class="d-flex justify-content-between align-items-center mb-2">
            <div class="d-flex align-items-center gap-2">
                <label class="form-label mb-0 fw-semibold">Bot code</label>
                <span class="small" :class="(content?.length || 0) > 10000 ? 'text-danger fw-bold' : 'text-muted'">
                    {{ content?.length || 0 }} / 10000
                </span>
            </div>
            <div class="d-flex gap-2 align-items-center">
                <!-- Language picker -->
                <select :value="language" @change="changeLanguage" class="form-select form-select-sm"
                    style="width: 105px;">
                    <option v-for="lang in LANGUAGES" :key="lang.value" :value="lang.value">{{ lang.label }}</option>
                </select>
                <!-- Settings dropdown -->
                <div class="dropdown">
                    <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button"
                        data-bs-toggle="dropdown" data-bs-auto-close="outside" aria-expanded="false">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                            class="bi bi-gear-fill" viewBox="0 0 16 16">
                            <path
                                d="M9.405 1.05c-.413-1.4-2.397-1.4-2.81 0l-.1.34a1.464 1.464 0 0 1-2.105.872l-.31-.17c-1.283-.698-2.686.705-1.987 1.987l.169.311c.446.82.023 1.841-.872 2.105l-.34.1c-1.4.413-1.4 2.397 0 2.81l.34.1a1.464 1.464 0 0 1 .872 2.105l-.17.31c-.698 1.283.705 2.686 1.987 1.987l.311-.169a1.464 1.464 0 0 1 2.105.872l.1.34c.413 1.4 2.397 1.4 2.81 0l.1-.34a1.464 1.464 0 0 1 2.105-.872l.31.17c1.283.698 2.686-.705 1.987-1.987l-.169-.311a1.464 1.464 0 0 1 .872-2.105l.34-.1c1.4-.413 1.4-2.397 0-2.81l-.34-.1a1.464 1.464 0 0 1-.872-2.105l.17-.31c.698-1.283-.705-2.686-1.987-1.987l-.311.169a1.464 1.464 0 0 1-2.105-.872zM8 10.93a2.929 2.929 0 1 1 0-5.86 2.929 2.929 0 0 1 0 5.858z" />
                        </svg>
                    </button>
                    <div class="dropdown-menu dropdown-menu-end p-3 shadow" style="width: 250px; z-index: 1060;">
                        <div class="mb-2">
                            <label class="form-label small text-muted mb-1">Theme</label>
                            <select v-model="editorSettings.theme" @change="saveSettings"
                                class="form-select form-select-sm">
                                <option value="textmate">Light (Textmate)</option>
                                <option value="chrome">Light (Chrome)</option>
                                <option value="monokai">Dark (Monokai)</option>
                                <option value="dracula">Dark (Dracula)</option>
                            </select>
                        </div>
                        <div class="mb-2">
                            <label class="form-label small text-muted mb-1">Font size</label>
                            <select v-model="editorSettings.fontSize" @change="saveSettings"
                                class="form-select form-select-sm">
                                <option :value="14">14 px (small)</option>
                                <option :value="16">16 px (default)</option>
                                <option :value="18">18 px (large)</option>
                                <option :value="20">20 px (extra large)</option>
                            </select>
                        </div>
                        <div class="form-check form-switch mb-1">
                            <input class="form-check-input" type="checkbox" :id=wrapSwitchId
                                v-model="editorSettings.wrap" @change="saveSettings">
                            <label class="form-check-label small" :for=wrapSwitchId>Wrap long lines</label>
                        </div>
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" :id=completeSwitchId
                                v-model="editorSettings.enableAutocompletion" @change="saveSettings">
                            <label class="form-check-label small" :for=completeSwitchId>Live autocompletion</label>
                        </div>
                    </div>
                </div>
                <!-- Reset to template -->
                <button type="button" class="btn btn-sm btn-outline-secondary" title="Reset to template" @click="resetCode">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-arrow-counterclockwise" viewBox="0 0 16 16">
                        <path fill-rule="evenodd" d="M8 3a5 5 0 1 1-4.546 2.914.5.5 0 0 0-.908-.417A6 6 0 1 0 8 2z" />
                        <path
                            d="M8 4.466V.534a.25.25 0 0 0-.41-.192L5.23 2.308a.25.25 0 0 0 0 .384l2.36 1.966A.25.25 0 0 0 8 4.466" />
                    </svg>
                </button>
            </div>
        </div>
        <VAceEditor v-model:value="content" :lang="editorMode" :theme="editorSettings.theme" style="height: 320px"
            :options="editorOptions" />
    </div>
</template>
