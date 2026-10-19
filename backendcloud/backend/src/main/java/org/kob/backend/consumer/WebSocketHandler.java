package org.kob.backend.consumer;

import org.kob.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.kob.backend.pojo.User;
import org.kob.backend.mapper.BotMapper;
import org.kob.backend.pojo.Bot;

import org.kob.backend.pojo.Player;
import org.kob.backend.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BotMapper botMapper;
    @Autowired
    private PlayerManager playerManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    @Qualifier("matchingClient") // there are two RestClients, so pick one by name
    private RestClient matchingClient;


    @Override // equivalent to @OnOpen
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        String token = path.substring(path.lastIndexOf("/") + 1);
        Integer userId = JwtUtil.getUserId(token);
        if (userId == -1) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        session.getAttributes().put("userId", userId);
        Player oldPlayer = playerManager.getPlayer(userId);
        Player player = new Player(user, session);
        playerManager.addPlayer(player);

        if (oldPlayer != null) { // one connection per account: close the old one
            try {
                oldPlayer.getSession().close(new CloseStatus(4000, "kicked"));
                // when the old connection closes, stopMatching skips it because the session does not match, so remove the user from the matching pool here
                removeFromMatchingPool(userId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Player connected: " + user.getUsername());
    }

    private void startMatching(Integer userId, Integer botId) {
        System.out.println("Start matching");
        Player player = playerManager.getPlayer(userId);
        if (player == null)
            return;
        Game game = player.getGame();
        if (game != null && game.isPlaying()) // the previous game is still running, so no new game
            return;
        Bot bot = null;
        if (botId != -1) {
            bot = botMapper.selectById(botId);
            if (bot == null || !bot.getUserId().equals(userId)) {      
                return;
            }
        }
        player.setBot(bot);
        Map<String, Integer> data = new HashMap<>();
        data.put("user_id", userId);
        data.put("rating", player.getUser().getRating());
        matchingClient.post().uri("/player/add/").body(data).retrieve().toBodilessEntity();

    }

    private void stopMatching(Integer userId, WebSocketSession session) {
        System.out.println("Stop matching");
        Player player = playerManager.getPlayer(userId);
        if (player == null || player.getSession() != session) // only handle this connection's own player, not a newer connection from the same user
            return;
        removeFromMatchingPool(userId);
    }

    private void removeFromMatchingPool(Integer userId) {
        Map<String, Integer> data = new HashMap<>();
        data.put("user_id", userId);
        matchingClient.post().uri("/player/remove/").body(data).retrieve().toBodilessEntity();
    }

    private void move(Integer userId, Integer direction) {
        Player player = playerManager.getPlayer(userId);
        if (player == null || player.getGame() == null || player.getBot() != null) 
            return;
        Game game = player.getGame();
        if (game.getP1() == player) {
            game.setNextStepP1(direction);
        } else if (game.getP2() == player) {
            game.setNextStepP2(direction);
        }
    }

    @Override // equivalent to @OnMessage
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("Message received");
        JsonNode data = objectMapper.readTree(message.getPayload()); // the event field acts as a route
        String event = data.get("event").asString();
        Integer userId = (Integer) session.getAttributes().get("userId");
        if ("start-matching".equals(event)) {
            Integer botId = data.path("bot_id").asInt(-1);
            startMatching(userId, botId);
        } else if ("stop-matching".equals(event)) {
            stopMatching(userId, session);
        } else if ("move".equals(event)) {
            move(userId, data.get("direction").asInt());
        }
    }

    @Override // equivalent to @OnClose
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) return;
        try {
            stopMatching(userId, session);
        } finally {
            playerManager.removePlayer(userId,session);
            System.out.println("Player disconnected: " + userId);
        }
    }

    @Override // equivalent to @OnError
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        exception.printStackTrace();
    }
}