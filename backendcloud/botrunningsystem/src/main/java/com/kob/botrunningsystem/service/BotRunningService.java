package com.kob.botrunningsystem.service;

import com.kob.botrunningsystem.service.impl.utils.Language;

public interface BotRunningService {
    String addBot(Integer userId, Language language, String botCode, String input);

    String compileBot(Language language, String botCode); // returns "success" on success, otherwise an error message
}
