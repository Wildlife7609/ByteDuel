package org.kob.backend.consumer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.kob.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

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

    private static final Set<Player> matchPool = ConcurrentHashMap.newKeySet();

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
        Player player = new Player(user, session);
        playerManager.addPlayer(player);
        System.out.println("玩家连接成功: " + user.getUsername());
    }

    private void startMatching(Integer userId) {
        System.out.println("开始匹配");
        Player player = playerManager.getPlayer(userId);
        if (player == null)
            return;
        matchPool.add(player);

        while (matchPool.size() >= 2) {
            Iterator<Player> it = matchPool.iterator();
            Player a = it.next();
            Player b = it.next();

            matchPool.remove(a);
            matchPool.remove(b);

            Game game = new Game(15, 14, 20, a, b);
            game.createMap();

            a.setGame(game);
            a.setSx(game.rows - 2);
            a.setSy(1);
            a.setSteps(new ArrayList<>());

            b.setGame(game);
            b.setSx(1);
            b.setSy(game.cols - 2);
            b.setSteps(new ArrayList<>());

            ObjectNode respGame = objectMapper.createObjectNode();
            respGame.put("a_id", a.getUser().getId());
            respGame.put("a_sx", a.getSx());
            respGame.put("a_sy", a.getSy());
            respGame.put("b_id", b.getUser().getId());
            respGame.put("b_sx", b.getSx());
            respGame.put("b_sy", b.getSy());
            respGame.putPOJO("map", game.getG());

            ObjectNode respA = objectMapper.createObjectNode();
            respA.put("event", "match-success");
            respA.put("opponent_username", b.getUser().getUsername());
            respA.put("opponent_photo", b.getUser().getPhoto());
            respA.set("game", respGame);

            ObjectNode respB = objectMapper.createObjectNode();
            respB.put("event", "match-success");
            respB.put("opponent_username", a.getUser().getUsername());
            respB.put("opponent_photo", a.getUser().getPhoto());
            respB.set("game", respGame);

            playerManager.sendMessage(a, respA.toString());
            playerManager.sendMessage(b, respB.toString());
        }
    }

    private void stopMatching(Integer userId) {
        System.out.println("取消匹配");
        Player player = playerManager.getPlayer(userId);
        if (player == null)
            return;
        matchPool.remove(player);
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
            stopMatching(userId);
        }
    }

    @Override // 相当于 @OnClose（断开连接）
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        stopMatching(userId);
        playerManager.removePlayer(userId);
        System.out.println("玩家断开连接: " + userId);

    }

    @Override // 相当于 @OnError（通信异常）
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        exception.printStackTrace();
    }
}