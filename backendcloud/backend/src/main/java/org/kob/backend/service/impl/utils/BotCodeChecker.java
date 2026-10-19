package org.kob.backend.service.impl.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Have botrunningsystem compile the bot before saving: code that does not compile is rejected, and the compile cache is warmed up
@Component
public class BotCodeChecker {
    // keep in sync with the Language enum in botrunningsystem
    public static final Set<String> LANGUAGES = Set.of("cpp", "java", "python", "go");

    @Autowired
    @Qualifier("botRunningClient") // there are two RestClients, so pick one by name
    private RestClient botRunningClient;

    // returns null on success, otherwise an error message for the user
    public String check(String language, String content) {
        Map<String, String> data = new HashMap<>();
        data.put("bot_language", language);
        data.put("bot_code", content);
        try {
            String resp = botRunningClient.post().uri("/bot/compile/").body(data).retrieve().body(String.class);
            if ("success".equals(resp))
                return null;
            return resp == null ? "Bot compile check failed" : resp;
        } catch (Exception e) {
            e.printStackTrace();
            return "The bot compiler is unavailable. Please try again later.";
        }
    }
}
