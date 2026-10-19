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

@Component // must be a Spring bean for @Autowired to work
public class MatchingPool extends Thread {
    @Autowired
    private RestClient backendClient;
    private final List<Player> players = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    @PostConstruct // start the thread after injection, so backendClient is ready
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

    // Try to match every player and return the pairs; the caller notifies backend after releasing the lock
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

    private void sendResult(Player a, Player b) { // send a match to backend
        System.out.println("MatchingPool sendResult " + a.getUserId() + " " + b.getUserId());
        Map<String, Integer> data = new HashMap<>();
        data.put("a_id", a.getUserId());
        data.put("b_id", b.getUserId());
        try {
            backendClient.post().uri("/pk/start/game/").body(data).retrieve().toBodilessEntity();
        } catch (Exception e) {
            e.printStackTrace(); // failing to notify backend must not kill the matching thread
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
                // Do not hold the lock while notifying backend: if backend is slow, requests to join or leave matching would be blocked
                for (Player[] pair : matched)
                    sendResult(pair[0], pair[1]);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

        }
    }
}
