package org.kob.backend.controller.pk;

import org.kob.backend.service.pk.StartGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
public class StartGameController {
    @Autowired
    private StartGameService startGameService;

    @PostMapping("/pk/start/game/")
    public String startGame(@RequestBody Map<String, Integer> data) {
        Integer aId = Objects.requireNonNull(data.get("a_id"));
        Integer bId = Objects.requireNonNull(data.get("b_id"));
        return  startGameService.startGame(aId, bId);
    }
}
