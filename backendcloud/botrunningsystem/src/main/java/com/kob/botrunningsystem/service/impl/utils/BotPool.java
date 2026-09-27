package com.kob.botrunningsystem.service.impl;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BotPool extends Thread{
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition condition = lock.newCondition();
    private Queue<Bot> bots = new LinkedList<>();

    private void consumeBot(Bot bot) {
        try {
            lock.lock();
        }
    }

    public void addBot(Integer userId, String botCode, String input) {
        lock.lock();
        try {
            bots.add(new Bot(userId, botCode, input));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        while (true) {
            lock.lock();
            if (bots.isEmpty()) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    lock.unlock();
                    break;
                }
            }
            else  {
                Bot bot = bots.remove();
                lock.unlock();
                consumeBot(bot); // 比较耗时 可能执行几秒钟
            }
        }
    }
}
