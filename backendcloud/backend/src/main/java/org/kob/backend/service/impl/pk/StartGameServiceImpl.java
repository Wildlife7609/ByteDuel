package org.kob.backend.service.impl.pk;

import java.util.ArrayList;

import org.kob.backend.consumer.Game;
import org.kob.backend.consumer.PlayerManager;
import org.kob.backend.mapper.RecordMapper;
import org.kob.backend.pojo.Player;
import org.kob.backend.service.pk.StartGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class StartGameServiceImpl implements StartGameService {
    @Autowired
    private PlayerManager playerManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RecordMapper recordMapper;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String startGame(Integer aId, Integer bId) {
        System.out.println("StartGameServiceImpl startGame "  + aId + " " + bId);
        Player a = playerManager.getPlayer(aId);
        Player b = playerManager.getPlayer(bId);
        if (a == null || b == null) return "player offline"; // 配对期间有人掉线
        startGame(a, b);
        return "start game success";
    }

    private void startGame(Player a, Player b) {
        Game game = new Game(15, 14, 20, a, b, recordMapper, restTemplate);
        game.createMap();

        a.setGame(game);
        a.setSx(game.getRows() - 2);
        a.setSy(1);
        a.setSteps(new ArrayList<>());

        b.setGame(game);
        b.setSx(1);
        b.setSy(game.getCols() - 2);
        b.setSteps(new ArrayList<>());

        game.start();
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

        a.sendMessage(respA.toString());
        b.sendMessage(respB.toString());
    }
}
