package com.kob.botrunningsystem.service.impl;

import com.kob.botrunningsystem.service.BotRunningService;
import com.kob.botrunningsystem.service.impl.utils.BotCompiler;
import com.kob.botrunningsystem.service.impl.utils.BotPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BotRunningServiceImpl implements BotRunningService {
    @Autowired
    private BotPool botPool;

    @Override
    public String addBot(Integer userId, String botCode, String input) {
        System.out.println("addBot : " + userId + " " + botCode + " " + input);
        botPool.addBot(userId, botCode, input);
        return "addBot success";
    }

    @Override
    public String compileBot(String botCode) {
        String error = BotCompiler.check(botCode);
        return error == null ? "success" : error;
    }
}
