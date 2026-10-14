package com.kob.matchingsystem.service.impl.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component // 必须交给 Spring 管理，@Autowired 才会生效
public class MatchingPool extends Thread {
    @Autowired
    private RestClient backendClient;
    private final List<Player> players = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    @PostConstruct // 依赖注入完成后再启动线程，保证 backendClient 已就绪
    public void init() {
        start();
    }

    public void addPlayer(Integer userId, Integer rating) {
        lock.lock();
        try {
            players.removeIf(p -> p.getUserId().equals(userId));
            players.add(new Player(userId, rating, 0));
        } finally {
            lock.unlock();
        }
    }

    private void increaseWaitingTime() {
        for (Player player : players) {
            player.setWaitingTime(player.getWaitingTime() + 1);
        }
    }

    public void removePlayer(Integer userId) {
        lock.lock();
        try {
            players.removeIf(p -> p.getUserId().equals(userId));
        } finally {
            lock.unlock();
        }
    }

    // 尝试匹配所有玩家，返回配好的对，由调用方在释放锁之后通知 backend
    private List<Player[]> matchPlayers() {
        System.out.println("MatchingPool matchPlayers " + players.toString());
        List<Player[]> matched = new ArrayList<>();
        boolean[] used = new boolean[players.size()];
        for (int i = 0; i < players.size(); i++) {
            if (used[i])
                continue;
            Player playerA = players.get(i);
            for (int j = i + 1; j < players.size(); j++) {
                if (used[j])
                    continue;
                Player playerB = players.get(j);
                int ratingDiff = Math.abs(playerA.getRating() - playerB.getRating());
                int waitingTime = Math.min(playerA.getWaitingTime(), playerB.getWaitingTime());
                if (ratingDiff <= waitingTime * 10) {
                    used[i] = true;
                    used[j] = true;
                    matched.add(new Player[]{playerA, playerB});
                    break;
                }
            }
        }
        List<Player> newPlayers = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (!used[i]) {
                newPlayers.add(players.get(i));
            }
        }
        players.clear();
        players.addAll(newPlayers);
        return matched;
    }

    private void sendResult(Player a, Player b) { // 发送匹配结果
        System.out.println("MatchingPool sendResult " + a.getUserId() + " " + b.getUserId());
        Map<String, Integer> data = new HashMap<>();
        data.put("a_id", a.getUserId());
        data.put("b_id", b.getUserId());
        try {
            backendClient.post().uri("/pk/start/game/").body(data).retrieve().toBodilessEntity();
        } catch (Exception e) {
            e.printStackTrace(); // 通知 backend 失败，不能让匹配线程退出
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                List<Player[]> matched;
                lock.lock();
                try {
                    increaseWaitingTime();
                    matched = matchPlayers();
                } finally {
                    lock.unlock();
                }
                // 通知 backend 时不能拿着锁：backend 响应慢的话，加入、取消匹配的请求都会被卡住
                for (Player[] pair : matched)
                    sendResult(pair[0], pair[1]);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

        }
    }
}
