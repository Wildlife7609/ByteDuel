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
ace.config.set(
    "basePath",
    "https://cdn.jsdelivr.net/npm/ace-builds@" + ace.version + "/src-noconflict/"
);
// 支持 v-model 双向绑定
const content = defineModel({ default: '' });
const wrapSwitchId = useId();
const completeSwitchId = useId();
const editorLang = ref("c_cpp");
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
const clearCode = () => {
    if (!content.value) return;
    if (confirm("确定要清空当前编辑器中的所有代码吗？")) {
        content.value = "";
    }
};
</script>
<template>
    <div class="mb-3">
        <div class="d-flex justify-content-between align-items-center mb-2">
            <div class="d-flex align-items-center gap-2">
                <label class="form-label mb-0 fw-semibold">Bot 代码</label>
                <span class="small" :class="(content?.length || 0) > 10000 ? 'text-danger fw-bold' : 'text-muted'">
                    {{ content?.length || 0 }} / 10000
                </span>
            </div>
            <div class="d-flex gap-2 align-items-center">
                <!-- 语言切换 -->
                <select v-model="editorLang" class="form-select form-select-sm" style="width: 105px;">
                    <option value="c_cpp">C++</option>
                    <option value="java">Java</option>
                    <option value="python">Python</option>
                    <option value="golang">Go</option>
                </select>
                <!-- 设置悬浮面板 -->
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
                            <label class="form-label small text-muted mb-1">主题风格</label>
                            <select v-model="editorSettings.theme" @change="saveSettings"
                                class="form-select form-select-sm">
                                <option value="textmate">浅色 (Textmate)</option>
                                <option value="chrome">浅色 (Chrome)</option>
                                <option value="monokai">深色 (Monokai)</option>
                                <option value="dracula">深色 (Dracula)</option>
                            </select>
                        </div>
                        <div class="mb-2">
                            <label class="form-label small text-muted mb-1">字体大小</label>
                            <select v-model="editorSettings.fontSize" @change="saveSettings"
                                class="form-select form-select-sm">
                                <option :value="14">14 px (较小)</option>
                                <option :value="16">16 px (推荐)</option>
                                <option :value="18">18 px (较大)</option>
                                <option :value="20">20 px (超大)</option>
                            </select>
                        </div>
                        <div class="form-check form-switch mb-1">
                            <input class="form-check-input" type="checkbox" :id=wrapSwitchId
                                v-model="editorSettings.wrap" @change="saveSettings">
                            <label class="form-check-label small" :for=wrapSwitchId>长代码自动换行</label>
                        </div>
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" :id=completeSwitchId
                                v-model="editorSettings.enableAutocompletion" @change="saveSettings">
                            <label class="form-check-label small" :for=completeSwitchId>实时自动补全</label>
                        </div>
                    </div>
                </div>
                <!-- 清空按钮 -->
                <button type="button" class="btn btn-sm btn-outline-danger" title="清空代码" @click="clearCode">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-trash3-fill" viewBox="0 0 16 16">
                        <path
                            d="M11 1.5v1h3.5a.5.5 0 0 1 0 1h-.538l-.853 10.66A2 2 0 0 1 11.115 16h-6.23a2 2 0 0 1-1.994-1.84L2.038 3.5H1.5a.5.5 0 0 1 0-1H5v-1A1.5 1.5 0 0 1 6.5 0h3A1.5 1.5 0 0 1 11 1.5m-5 0v1h4v-1a.5.5 0 0 0-.5-.5h-3a.5.5 0 0 0-.5.5M4.5 5.029l.5 8.5a.5.5 0 1 0 .998-.06l-.5-8.5a.5.5 0 1 0-.998.06m6.53-.528a.5.5 0 0 0-.528.47l-.5 8.5a.5.5 0 0 0 .998.058l.5-8.5a.5.5 0 0 0-.47-.528M8 4.5a.5.5 0 0 0-.5.5v8.5a.5.5 0 0 0 1 0V5a.5.5 0 0 0-.5-.5" />
                    </svg>
                </button>
            </div>
        </div>
        <VAceEditor v-model:value="content" :lang="editorLang" :theme="editorSettings.theme" style="height: 320px"
            :options="editorOptions" />
    </div>
</template>
