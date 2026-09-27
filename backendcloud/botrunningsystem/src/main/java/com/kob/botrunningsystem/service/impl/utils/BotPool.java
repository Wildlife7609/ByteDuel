package com.kob.botrunningsystem.service.impl.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class BotPool extends Thread{
    @Autowired
    private RestTemplate restTemplate;

    private final BlockingQueue<Bot> bots = new LinkedBlockingQueue<>();

    @PostConstruct // 依赖注入完成后再启动线程，保证 restTemplate 已就绪
    public void init() {
        start();
    }

   public void addBot(Integer userId, String botCode, String input) {
        bots.add(new Bot(userId, botCode, input)); // 线程安全，立刻返回
    }

    private void consumeBot(Bot bot) {
        // TODO: 新建线程编译执行，join(2000) 超时放弃，结果发回 backend
    }

    @Override
    public void run() {
        while (true) {
            try {
                Bot bot = bots.take(); // 队列空时自动阻塞，有任务时自动唤醒
                consumeBot(bot);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
