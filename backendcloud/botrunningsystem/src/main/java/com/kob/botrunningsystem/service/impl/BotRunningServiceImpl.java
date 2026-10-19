package com.kob.botrunningsystem.service.impl;

import com.kob.botrunningsystem.service.BotRunningService;
import com.kob.botrunningsystem.service.impl.utils.BotPool;
import com.kob.botrunningsystem.service.impl.utils.Language;
import com.kob.botrunningsystem.service.impl.utils.Sandbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class BotRunningServiceImpl implements BotRunningService {
    @Autowired
    private BotPool botPool;
    @Autowired
    private Sandbox sandbox;

    @Override
    public String addBot(Integer userId, Language language, String botCode, String input) {
        System.out.println("addBot : " + userId + " " + language.getId() + " " + input);
        botPool.addBot(userId, language, botCode, input);
        return "addBot success";
    }

    @Override
    public String compileBot(Language language, String botCode) {
        try {
            String error = sandbox.check(language, botCode);
            return error == null ? "success" : error;
        } catch (RestClientException e) {
            e.printStackTrace();
            return "The bot compiler is unavailable. Please try again later.";
        }
    }
}
