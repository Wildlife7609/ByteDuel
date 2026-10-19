package com.kob.botrunningsystem.controller;

import com.kob.botrunningsystem.service.BotRunningService;
import com.kob.botrunningsystem.service.impl.utils.Language;
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
        Language language = Language.fromId((String) data.get("bot_language"));
        String botCode = (String) Objects.requireNonNull(data.get("bot_code"));
        String input = (String) Objects.requireNonNull(data.get("input"));
        if (language == null)
            return "Unsupported language";
        return botRunningService.addBot(userId, language, botCode, input);
    }

    @PostMapping("/bot/compile/")
    public String compileBot(@RequestBody Map<String, String> data) {
        Language language = Language.fromId(data.get("bot_language"));
        String botCode = Objects.requireNonNull(data.get("bot_code"));
        if (language == null)
            return "Unsupported language";
        return botRunningService.compileBot(language, botCode);
    }
}
