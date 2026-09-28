package com.kob.botrunningsystem.service.impl.utils;

import org.joor.Reflect;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.util.function.Function;

public class Consumer extends Thread {
    private static final String receiveBotMoveUrl = "http://127.0.0.1:8080/pk/receive/bot/move/";

    private final RestTemplate restTemplate; // Consumer 是 new 出来的，不能 @Autowired，由 BotPool 传入
    private BotTask botTask;

    public Consumer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void startTimeout(long timeout, BotTask botTask) {
        this.botTask = botTask;
        this.start();
        try {
            this.join(timeout); // 最多等待timeout毫秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.interrupt(); // 打断执行线程（死循环的代码不会响应，只是不再等它）
        }
    }

    @Override
    public void run() {
        try {
            // 类名加上随机后缀，避免同名类重复编译冲突
            String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String code = botTask.getBotCode().replaceFirst("class Bot\\b", "class Bot" + uid);
            Function<String, Integer> bot = Reflect.compile(
                    "com.kob.botrunningsystem.utils.Bot" + uid, code).create().get();
            Integer direction = bot.apply(botTask.getInput());

            MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
            data.add("user_id", botTask.getUserId().toString());
            data.add("direction", direction.toString());
            restTemplate.postForObject(receiveBotMoveUrl, data, String.class);
        } catch (Exception e) {
            e.printStackTrace(); // 编译失败或运行出错：不发送结果，Game 那边超时判负
        }
    }
}
