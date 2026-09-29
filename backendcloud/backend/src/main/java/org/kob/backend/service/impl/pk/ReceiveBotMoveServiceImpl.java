package org.kob.backend.service.impl.pk;

import org.kob.backend.consumer.Game;
import org.kob.backend.consumer.PlayerManager;
import org.kob.backend.pojo.Player;
import org.kob.backend.service.pk.ReceiveBotMoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReceiveBotMoveServiceImpl implements ReceiveBotMoveService {
    @Autowired
    private PlayerManager playerManager;
    @Override
    public String receiveBotMove(Integer userId, Integer direction) {
        System.out.println("receiveBotMove - userId: " + userId + ", direction: " + direction);
        Player player = playerManager.getPlayer(userId);
        if (player == null) {
            return "Player not found";
        }
        Game game = player.getGame();
        if (game == null) {
            return "Game not found";
        }
        if (game.getP1() == player) {
            game.setNextStepP1(direction);
        }
        else if (game.getP2() == player) {
            game.setNextStepP2(direction);
        }
        return "receiveBotMove success";
    }
}
