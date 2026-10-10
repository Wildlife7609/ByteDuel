package com.kob.botrunningsystem.service.impl.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class BotPool extends Thread {
    @Autowired
    @Qualifier("backendClient") // 有两个 RestClient，按名字指定用哪一个
    private RestClient backendClient;
    @Autowired
    private Sandbox sandbox;

    private final BlockingQueue<BotTask> bots = new LinkedBlockingQueue<>();

    @PostConstruct // 依赖注入完成后再启动线程，保证 backendClient 已就绪
    public void init() {
        start();
    }

    public void addBot(Integer userId, Language language, String botCode, String input) {
        bots.add(new BotTask(userId, language, botCode, input)); // 线程安全，立刻返回
    }

    private void consumeBot(BotTask botTask) {
        // 由虚拟线程等待沙箱的运行结果，boss 线程不被阻塞，可以马上处理下一个任务
        Thread.ofVirtual().start(() -> runBot(botTask));
    }

    private void runBot(BotTask botTask) {
        try {
            // 超时由沙箱控制，超时、运行出错或输出不合法都不发送结果，Game 那边超时判负
            String output = sandbox.run(botTask.getLanguage(), botTask.getBotCode(), botTask.getInput());
            Integer direction = parseDirection(output);
            if (direction == null)
                return;

            Map<String, Integer> data = new HashMap<>();
            data.put("user_id", botTask.getUserId());
            data.put("direction", direction);
            backendClient.post().uri("/pk/receive/bot/move/").body(data).retrieve().toBodilessEntity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Integer parseDirection(String output) {
        if (output == null)
            return null;
        try {
            int direction = Integer.parseInt(output.trim());
            return direction >= 0 && direction < 4 ? direction : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                BotTask botTask = bots.take();
                consumeBot(botTask);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace(); // 单个 Bot 出错，继续处理下一个
            }
        }
    }
}
