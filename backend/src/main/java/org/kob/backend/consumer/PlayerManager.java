package org.kob.backend.consumer;

import java.util.concurrent.ConcurrentHashMap;

import org.kob.backend.pojo.Player;
import org.springframework.stereotype.Component;

@Component
public class PlayerManager {
    private final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();

    public void addPlayer(Player player) {
        if (player != null && player.getUser() != null) {
            players.put(player.getUser().getId(), player);
        }
    }

    public void removePlayer(Integer userId) {
        if (userId != null) {
            players.remove(userId);
        }
    }

    public Player getPlayer(Integer userId) {
        if (userId == null)
            return null;
        return players.get(userId);
    }
}
