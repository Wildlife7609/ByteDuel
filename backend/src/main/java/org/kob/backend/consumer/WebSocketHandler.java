package org.kob.backend.consumer;

import org.kob.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.kob.backend.pojo.User;
import org.kob.backend.pojo.Player;
import org.kob.backend.utils.JwtUtil;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PlayerManager playerManager;

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

    @Override // 相当于 @OnMessage（收到消息）
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println("收到消息: " + message.getPayload());
    }

    @Override // 相当于 @OnClose（断开连接）
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        playerManager.removePlayer(userId);
        System.out.println("玩家断开连接: " + userId);
    }

    @Override // 相当于 @OnError（通信异常）
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        exception.printStackTrace();
    }
}