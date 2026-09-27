package com.kob.botrunningsystem.service.impl;

import com.kob.botrunningsystem.service.BotRunningService;
import com.kob.botrunningsystem.service.impl.utils.BotPool;
import org.springframework.stereotype.Service;

@Service
public class BotRunningServiceImpl implements BotRunningService {
    @Override
    private final BotPool botPool;

    @Override
    public String addBot(Integer userId, String botCode, String input) {
        System.out.println("addBot : " + userId + " " + botCode + " " + input);
        botPool.addBot(userId, botCode, input);
        return "addBot success";
    }
}
