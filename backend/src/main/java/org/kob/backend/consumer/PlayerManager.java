package org.kob.backend.consumer;

import java.util.concurrent.ConcurrentHashMap;

import org.kob.backend.pojo.Player;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class PlayerManager {
    private final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();

    public void addPlayer(Player player) {
        if (player != null && player.getUser() != null) {
            players.put(player.getUser().getId(), player);
        }
    }

    // PlayerManager
    public void removePlayer(Integer userId, WebSocketSession session) {
        Player p = players.get(userId);
        if (p != null && p.getSession() == session) {
            players.remove(userId, p);
        }
    }

    public Player getPlayer(Integer userId) {
        if (userId == null)
            return null;
        return players.get(userId);
    }
}
