package com.kob.botrunningsystem.service;

public interface BotRunningService {
    String addBot(Integer userId, String botCode, String input);

    String compileBot(String botCode); // 通过返回 "success"，否则返回错误信息
}
