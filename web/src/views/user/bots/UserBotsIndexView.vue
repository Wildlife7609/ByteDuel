<script setup>
import ContentField from '@/components/ContentField.vue'
import axios from 'axios';

// axios.post("/user/bot/add/", {
//     title: "Bot的标题",
//     description: "Bot的描述",
//     content: "Bot的代码"
// }).then(res => console.log(res.data))
//     .catch(err => console.log(err));

axios.post("/user/bot/remove/", {
    bot_id: 15
}).then(res => console.log(res.data))
    .catch(err => console.log(err));

axios.post("/user/bot/update/", {
    bot_id: 17,
    title: "Bot17的标题",
    description: "Bot17的描述",
    content: "Bot17的代码"
}).then(res => console.log(res.data))
    .catch(err => console.log(err));

axios.get("/user/bot/getlist/").then(res => console.log(res.data))
    .catch(err => console.log(err));

</script>

<template>
    <ContentField>
        <div>我的bot</div>
    </ContentField>
</template>

<style scoped></style>