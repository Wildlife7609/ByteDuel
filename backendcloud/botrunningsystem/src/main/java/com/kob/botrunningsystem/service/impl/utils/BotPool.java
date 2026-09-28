package com.kob.botrunningsystem.service.impl.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class BotPool extends Thread {
    @Autowired
    private RestTemplate restTemplate;

    private final BlockingQueue<BotTask> bots = new LinkedBlockingQueue<>();

    @PostConstruct // 依赖注入完成后再启动线程，保证 restTemplate 已就绪
    public void init() {
        start();
    }

    public void addBot(Integer userId, String botCode, String input) {
        bots.add(new BotTask(userId, botCode, input)); // 线程安全，立刻返回
    }

    private void consumeBot(BotTask botTask) {
        // 由虚拟线程等待执行结果，boss 线程不被 join 阻塞，可以马上处理下一个任务
        Thread.ofVirtual().start(() -> new Consumer(restTemplate).startTimeout(2000, botTask));
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
