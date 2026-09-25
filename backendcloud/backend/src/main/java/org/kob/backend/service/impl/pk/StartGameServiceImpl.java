package org.kob.backend.service.impl.pk;

import org.kob.backend.consumer.PlayerManager;
import org.kob.backend.consumer.WebSocketHandler;
import org.kob.backend.pojo.Player;
import org.kob.backend.service.pk.StartGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StartGameServiceImpl implements StartGameService {
    @Autowired
    private WebSocketHandler webSocketHandler;
    @Autowired
    private PlayerManager playerManager;
    @Override
    public String startGame(Integer aId, Integer bId) {
        System.out.println("StartGameServiceImpl startGame "  + aId + " " + bId);
        Player a = playerManager.getPlayer(aId);
        Player b = playerManager.getPlayer(bId);
        if (a == null || b == null) return "player offline"; // 配对期间有人掉线
        webSocketHandler.startGame(a, b);
        return "start game success";
    }
}
