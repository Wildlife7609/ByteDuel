package org.kob.backend.service.impl.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// 保存 Bot 前让 botrunningsystem 预编译一次：编译不过的代码不入库，同时预热编译缓存
@Component
public class BotCodeChecker {
    // 和 botrunningsystem 的 Language 枚举保持一致
    public static final Set<String> LANGUAGES = Set.of("cpp", "java", "python", "go");

    @Autowired
    @Qualifier("botRunningClient") // 有两个 RestClient，按名字指定用哪一个
    private RestClient botRunningClient;

    // 通过返回 null，否则返回给用户看的错误信息
    public String check(String language, String content) {
        Map<String, String> data = new HashMap<>();
        data.put("bot_language", language);
        data.put("bot_code", content);
        try {
            String resp = botRunningClient.post().uri("/bot/compile/").body(data).retrieve().body(String.class);
            if ("success".equals(resp))
                return null;
            return resp == null ? "Bot 编译检查失败" : resp;
        } catch (Exception e) {
            e.printStackTrace();
            return "Bot 编译服务暂不可用，请稍后重试";
        }
    }
}
