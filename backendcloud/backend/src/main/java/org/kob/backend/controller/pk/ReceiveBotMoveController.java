package org.kob.backend.controller.pk;

import org.kob.backend.service.pk.ReceiveBotMoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
public class ReceiveBotMoveController {
    @Autowired
    private ReceiveBotMoveService receiveBotMoveService;

    @PostMapping("/pk/receive/bot/move/")
    public String receiveBotMove(@RequestBody Map<String, Integer> data) {
        Integer userId = Objects.requireNonNull(data.get("user_id"));
        Integer direction = Objects.requireNonNull(data.get("direction"));
        return receiveBotMoveService.receiveBotMove(userId, direction);
    }
}
