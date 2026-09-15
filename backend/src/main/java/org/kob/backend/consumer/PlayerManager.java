package org.kob.backend.consumer;

import java.util.concurrent.ConcurrentHashMap;

import org.kob.backend.pojo.Player;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

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

    public void sendMessage(Integer userId, String message) {
        Player player = getPlayer(userId);
        if (player != null && player.getSession() != null && player.getSession().isOpen()) {
            try {
                synchronized (player.getSession()) {
                    player.getSession().sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
