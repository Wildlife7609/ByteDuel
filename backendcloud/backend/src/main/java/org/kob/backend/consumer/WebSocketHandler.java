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
    @Qualifier("matchingClient") // 有两个 RestClient，按名字指定用哪一个
    private RestClient matchingClient;


    @Override // 相当于 @OnOpen（建立连接）
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

        if (oldPlayer != null) { // 同一账号只允许一个在线连接，踢掉旧的
            try {
                oldPlayer.getSession().close(new CloseStatus(4000, "kicked"));
                // 旧连接关闭时 stopMatching 会因为 session 不匹配而跳过，所以在这里把该用户移出匹配池
                removeFromMatchingPool(userId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("玩家连接成功: " + user.getUsername());
    }

    private void startMatching(Integer userId, Integer botId) {
        System.out.println("开始匹配");
        Player player = playerManager.getPlayer(userId);
        if (player == null)
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
        System.out.println("取消匹配");
        Player player = playerManager.getPlayer(userId);
        if (player == null || player.getSession() != session) // 只处理这个连接自己的 player，不误删同一用户新建立的连接
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

    @Override // 相当于 @OnMessage（收到消息）
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("收到消息");
        JsonNode data = objectMapper.readTree(message.getPayload());// 通常把message 当作路由
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

    @Override // 相当于 @OnClose（断开连接）
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) return;
        try {
            stopMatching(userId, session);
        } finally {
            playerManager.removePlayer(userId,session);
            System.out.println("玩家断开连接: " + userId);
        }
    }

    @Override // 相当于 @OnError（通信异常）
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        exception.printStackTrace();
    }
}