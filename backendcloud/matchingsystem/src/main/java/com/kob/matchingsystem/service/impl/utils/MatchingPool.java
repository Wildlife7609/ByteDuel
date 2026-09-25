package com.kob.matchingsystem.service.impl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class MatchingPool extends Thread {
    private final List<Player> players = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

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

    private void matchPlayers() { // 尝试匹配所有玩家
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
                    sendResult(playerA, playerB);
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
    }

    private void sendResult(Player a, Player b) { // 发送匹配结果

    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                lock.lock();
                try {
                    increaseWaitingTime();
                    matchPlayers();
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

        }
    }
}
