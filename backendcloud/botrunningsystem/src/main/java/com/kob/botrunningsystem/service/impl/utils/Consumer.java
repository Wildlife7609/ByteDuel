package com.kob.botrunningsystem.service.impl.utils;

import org.joor.Reflect;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Consumer extends Thread {
    private final RestClient backendClient; // Consumer 是 new 出来的，不能 @Autowired，由 BotPool 传入
    private BotTask botTask;

    public Consumer(RestClient backendClient) {
        this.backendClient = backendClient;
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
            // 每回合创建新对象，避免上一回合留下的字段状态影响这一回合
            Function<String, Integer> bot = Reflect.onClass(BotCompiler.getBotClass(botTask.getBotCode())).create().get();
            Integer direction = bot.apply(botTask.getInput());

            Map<String, Integer> data = new HashMap<>();
            data.put("user_id", botTask.getUserId());
            data.put("direction", direction);
            backendClient.post().uri("/pk/receive/bot/move/").body(data).retrieve().toBodilessEntity();
        } catch (Exception e) {
            e.printStackTrace(); // 编译失败或运行出错：不发送结果，Game 那边超时判负
        }
    }
}
