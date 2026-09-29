package com.kob.botrunningsystem.controller;

import com.kob.botrunningsystem.service.BotRunningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
public class BotRunningController {
    @Autowired
    private BotRunningService botRunningService;

    @PostMapping("/bot/add/")
    public String addBot(@RequestBody Map<String, Object> data) {
        Integer userId = (Integer) Objects.requireNonNull(data.get("user_id"));
        String botCode = (String) Objects.requireNonNull(data.get("bot_code"));
        String input = (String) Objects.requireNonNull(data.get("input"));
        return botRunningService.addBot(userId, botCode, input);
    }

    @PostMapping("/bot/compile/")
    public String compileBot(@RequestBody Map<String, String> data) {
        String botCode = Objects.requireNonNull(data.get("bot_code"));
        return botRunningService.compileBot(botCode);
    }
}
