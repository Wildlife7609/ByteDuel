package org.kob.backend.consumer;

import org.kob.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.kob.backend.pojo.User;
import org.kob.backend.pojo.Player;
import org.kob.backend.utils.JwtUtil;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PlayerManager playerManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RestTemplate restTemplate;

    private static final String addPlayerUrl = "http://127.0.0.1:8081/player/add/";
    private static final String removePlayerUrl = "http://127.0.0.1:8081/player/remove/";

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("玩家连接成功: " + user.getUsername());
    }

    private void startMatching(Integer userId) {
        System.out.println("开始匹配");
        Player player = playerManager.getPlayer(userId);
        if (player == null)
            return;
        
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", userId.toString());
        data.add("rating", player.getUser().getRating().toString());
        restTemplate.postForObject(addPlayerUrl, data, String.class);

    }

    private void stopMatching(Integer userId, WebSocketSession session) {
        System.out.println("取消匹配");
        Player player = playerManager.getPlayer(userId);
        if (player == null || player.getSession() != session) // 只处理这个连接自己的 player，不误删同一用户新建立的连接
            return;
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", userId.toString());
        restTemplate.postForObject(removePlayerUrl, data, String.class);
    }

    private void move(Integer userId, Integer direction) {
        Player player = playerManager.getPlayer(userId);
        if (player == null || player.getGame() == null)
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
            startMatching(userId);
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
            playerManager.removePlayer(userId, session);
            System.out.println("玩家断开连接: " + userId);
        }
    }

    @Override // 相当于 @OnError（通信异常）
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        exception.printStackTrace();
    }
}